package br.ufmg.mes.evaluation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ShopContractTest {
    private static final String ADMIN = "admin@evaluation.local";
    private static final String ALICE = "alice@evaluation.local";
    private static final String BOB = "bob@evaluation.local";
    private ApiClient admin;
    private ApiClient alice;
    private ApiClient bob;
    private ApiClient anonymous;

    @BeforeAll
    static void requiresRunningApplicationAndEvaluationFixtures() throws Exception {
        new ApiClient(null).get("/actuator/health").expect(200);
        JsonNode profile = new ApiClient(ADMIN).get("/api/me").expect(200).body;
        assertEquals(1, profile.path("id").asLong(), "Use fixtures de evaluation/ em banco separado");
        assertEquals("ADMIN", profile.path("role").asText());
        assertEquals("FIXTURE-ACTIVE", new ApiClient(null).get("/api/products/10").expect(200)
                .body.path("sku").asText());
    }

    @BeforeEach
    void clients() {
        admin = new ApiClient(ADMIN);
        alice = new ApiClient(ALICE);
        bob = new ApiClient(BOB);
        anonymous = new ApiClient(null);
    }

    @Test
    void publicHealthCatalogAndPagination() throws Exception {
        JsonNode health = anonymous.get("/actuator/health").expect(200).body;
        assertEquals(Set.of("status"), fields(health));
        assertEquals("UP", health.get("status").asText());
        JsonNode page = anonymous.get("/api/products?size=1").expect(200).body;
        assertEquals(Set.of("content", "page", "size", "totalElements", "totalPages"), fields(page));
        assertEquals(0, page.get("page").asInt());
        assertEquals(1, page.get("size").asInt());
        assertTrue(page.get("totalElements").asLong() >= 2);
        assertEquals(10, page.at("/content/0/id").asLong());
        JsonNode product = anonymous.get("/api/products/10").expect(200).body;
        assertEquals(Set.of("id", "sku", "name", "price", "stockQuantity"), fields(product));
        money(product, "price", "19.90");
        assertEquals(25, product.get("stockQuantity").asInt());
        assertEquals(0, anonymous.get("/api/products/30").expect(200).body.get("stockQuantity").asInt());
        anonymous.get("/api/products/20").error(404, "PRODUCT_NOT_FOUND");
        anonymous.get("/api/products/9223372036854775807").error(404, "PRODUCT_NOT_FOUND");
        List<JsonNode> products = pages(anonymous, "/api/products");
        List<Long> ids = products.stream().map(p -> p.get("id").asLong()).toList();
        assertEquals(ids.stream().sorted().toList(), ids);
        assertFalse(ids.contains(20L));
        JsonNode beyond = anonymous.get("/api/products?page=1000000&size=100").expect(200).body;
        assertTrue(beyond.get("content").isEmpty());
        assertEquals(products.size(), beyond.get("totalElements").asInt());
        anonymous.get("/api/products?size=101").error(400, "INVALID_REQUEST");
    }

    @Test
    void databaseAuthenticationRolesAndCsrfRemainEffective() throws Exception {
        for (ApiClient actor : List.of(admin, alice, bob)) {
            JsonNode profile = actor.get("/api/me").expect(200).body;
            assertEquals(Set.of("id", "name", "email", "role"), fields(profile));
        }
        anonymous.get("/api/me").error(401, "AUTHENTICATION_REQUIRED");
        new ApiClient(ALICE, "wrong-password").get("/api/me").error(401, "AUTHENTICATION_REQUIRED");
        new ApiClient("disabled@evaluation.local").get("/api/me").error(401, "AUTHENTICATION_REQUIRED");
        alice.get("/api/admin/orders").error(403, "FORBIDDEN");
        bob.get("/api/admin/reports/orders").error(403, "FORBIDDEN");
        admin.get("/api/orders").error(403, "FORBIDDEN");
        admin.write("POST", "/api/orders", items(10, 1)).error(403, "FORBIDDEN");
        alice.writeWithoutCsrf("/api/orders", items(10, 1)).error(403, "CSRF_INVALID");
        alice.get("/api/csrf").expect(200);
        alice.getWithoutAuthentication("/api/me").error(401, "AUTHENTICATION_REQUIRED");
        anonymous.get("/actuator/env").error(401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    void administratorManagesProductAndAbsoluteStockWithStrictValidation() throws Exception {
        JsonNode product = product("12.30", 5, true);
        long id = product.get("id").asLong();
        assertEquals(Set.of("id", "sku", "name", "price", "stockQuantity", "active"), fields(product));
        String duplicate = productBody(product.get("sku").asText(), "12.30", 5, true);
        admin.write("POST", "/api/admin/products", duplicate).error(409, "SKU_ALREADY_EXISTS");
        alice.write("POST", "/api/admin/products", productBody("FORBIDDEN", "1.00", 1, true))
                .error(403, "FORBIDDEN");
        admin.write("PATCH", "/api/admin/products/" + id + "/stock", "{\"stockQuantity\":2}")
                .expect(200);
        assertEquals(2, stock(id));
        admin.write("PATCH", "/api/admin/products/" + id + "/stock", "{\"stockQuantity\":-1}")
                .error(400, "INVALID_REQUEST");
        admin.write("PATCH", "/api/admin/products/" + id + "/stock", "{\"stockQuantity\":1.5}")
                .error(400, "INVALID_REQUEST");
        admin.write("PUT", "/api/admin/products/" + id,
                "{\"name\":\"Revisado\",\"price\":0,\"active\":true}").error(400, "INVALID_REQUEST");
        assertEquals(2, stock(id));
        money(anonymous.get("/api/products/" + id).expect(200).body, "price", "12.30");
        JsonNode updated = admin.write("PUT", "/api/admin/products/" + id,
                "{\"name\":\"Revisado\",\"price\":15.50,\"active\":false}").expect(200).body;
        assertFalse(updated.get("active").asBoolean());
        assertEquals(2, updated.get("stockQuantity").asInt());
        anonymous.get("/api/products/" + id).error(404, "PRODUCT_NOT_FOUND");
    }

    @Test
    void computesOrderTotalsAndStockFromServerPrices() throws Exception {
        long first = product("12.30", 5, true).get("id").asLong();
        long second = product("2.25", 8, true).get("id").asLong();
        String payload = "{\"items\":[{\"productId\":" + second + ",\"quantity\":3},"
                + "{\"productId\":" + first + ",\"quantity\":2}]}";
        ApiClient.Result response = alice.write("POST", "/api/orders", payload).expect(201);
        JsonNode order = response.body;
        long id = order.get("id").asLong();
        assertEquals("/api/orders/" + id, response.response.headers().firstValue("Location").orElseThrow());
        assertEquals(Set.of("id", "customerId", "status", "createdAt", "cancelledAt", "total", "items"), fields(order));
        assertEquals(2, order.get("customerId").asLong());
        assertEquals("CREATED", order.get("status").asText());
        assertTrue(order.get("cancelledAt").isNull());
        assertTrue(order.get("createdAt").asText().endsWith("Z"));
        Instant.parse(order.get("createdAt").asText());
        money(order, "total", "31.35");
        assertEquals(first, order.at("/items/0/productId").asLong());
        assertEquals(Set.of("productId", "quantity", "unitPrice", "subtotal"), fields(order.at("/items/0")));
        money(order.at("/items/0"), "unitPrice", "12.30");
        money(order.at("/items/0"), "subtotal", "24.60");
        money(order.at("/items/1"), "subtotal", "6.75");
        assertEquals(3, stock(first));
        assertEquals(5, stock(second));
        assertEquals(order, alice.get("/api/orders/" + id).expect(200).body);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"items\":[]}", "{\"items\":[null]}",
            "{\"items\":[{\"productId\":10,\"quantity\":0}]}",
            "{\"items\":[{\"productId\":10,\"quantity\":1.5}]}",
            "{\"items\":[{\"productId\":10,\"quantity\":\"1\"}]}",
            "{\"customerId\":3,\"items\":[{\"productId\":10,\"quantity\":1}]}",
            "{\"items\":[{\"productId\":10,\"quantity\":1,\"unitPrice\":0.01}]}",
            "{\"items\":[{\"productId\":10,\"quantity\":1},{\"productId\":10,\"quantity\":1}]}"})
    void rejectsInvalidOrderBodiesWithoutObservableSideEffects(String payload) throws Exception {
        long before = alice.get("/api/orders").expect(200).body.get("totalElements").asLong();
        alice.write("POST", "/api/orders", payload).error(400, "INVALID_REQUEST");
        assertEquals(before, alice.get("/api/orders").expect(200).body.get("totalElements").asLong());
        assertEquals(25, stock(10));
    }

    @Test
    void invalidLaterItemRollsBackWholeOrderAndStock() throws Exception {
        long first = product("4.00", 5, true).get("id").asLong();
        long inactive = product("3.00", 4, false).get("id").asLong();
        long scarce = product("3.00", 1, true).get("id").asLong();
        long before = alice.get("/api/orders").expect(200).body.get("totalElements").asLong();
        for (long later : new long[] {inactive, scarce, Long.MAX_VALUE}) {
            String payload = "{\"items\":[{\"productId\":" + first + ",\"quantity\":2},"
                    + "{\"productId\":" + later + ",\"quantity\":2}]}";
            String code = later == inactive ? "PRODUCT_INACTIVE"
                    : later == scarce ? "INSUFFICIENT_STOCK" : "PRODUCT_NOT_FOUND";
            alice.write("POST", "/api/orders", payload).error(later == Long.MAX_VALUE ? 404 : 409, code);
            assertEquals(5, stock(first));
            assertEquals(1, stock(scarce));
            assertEquals(before, alice.get("/api/orders").expect(200).body.get("totalElements").asLong());
        }
    }

    @Test
    void customersCannotReadListOrCancelEachOthersOrders() throws Exception {
        long product = product("9.90", 4, true).get("id").asLong();
        long own = order(alice, product, 1).get("id").asLong();
        long other = order(bob, product, 1).get("id").asLong();
        JsonNode hidden = bob.get("/api/orders/" + own).error(404, "ORDER_NOT_FOUND").body;
        assertEquals(hidden, bob.get("/api/orders/9223372036854775807").error(404, "ORDER_NOT_FOUND").body);
        alice.get("/api/orders/" + other).error(404, "ORDER_NOT_FOUND");
        bob.write("POST", "/api/orders/" + own + "/cancel", null).error(404, "ORDER_NOT_FOUND");
        List<Long> ownIds = pages(alice, "/api/orders").stream().map(o -> o.get("id").asLong()).toList();
        assertTrue(ownIds.contains(own));
        assertFalse(ownIds.contains(other));
        assertEquals(ownIds.stream().sorted(java.util.Comparator.reverseOrder()).toList(), ownIds);
        for (JsonNode listed : pages(alice, "/api/orders")) {
            assertEquals(2, listed.get("customerId").asLong());
        }
        admin.get("/api/orders/" + other).expect(200);
        assertEquals(2, stock(product));
    }

    @Test
    void priceHistorySurvivesInactivationAndCancellationRestocksOnlyOnce() throws Exception {
        long product = product("7.90", 4, true).get("id").asLong();
        JsonNode created = order(alice, product, 2);
        long id = created.get("id").asLong();
        admin.write("PUT", "/api/admin/products/" + product,
                "{\"name\":\"Reajustado\",\"price\":99.99,\"active\":false}").expect(200);
        assertEquals(created, alice.get("/api/orders/" + id).expect(200).body);
        JsonNode cancelled = admin.write("POST", "/api/orders/" + id + "/cancel", null).expect(200).body;
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertFalse(Instant.parse(cancelled.get("cancelledAt").asText())
                .isBefore(Instant.parse(cancelled.get("createdAt").asText())));
        assertEquals(cancelled, alice.write("POST", "/api/orders/" + id + "/cancel", null).expect(200).body);
        admin.write("PUT", "/api/admin/products/" + product,
                "{\"name\":\"Reativado\",\"price\":99.99,\"active\":true}").expect(200);
        assertEquals(4, stock(product));
        money(cancelled, "total", "15.80");
        alice.write("POST", "/api/orders", items(30, 1)).error(409, "INSUFFICIENT_STOCK");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void simultaneousPurchasesOfLastUnitHaveExactlyOneWinner(int trial) throws Exception {
        long product = product("8.50", 1, true).get("id").asLong();
        List<ApiClient.Result> results = concurrent(alice, bob, "/api/orders", items(product, 1));
        assertEquals(List.of(201, 409), results.stream().map(r -> r.response.statusCode()).sorted().toList(),
                "Trial " + trial);
        results.stream().filter(r -> r.response.statusCode() == 409).findFirst().orElseThrow()
                .error(409, "INSUFFICIENT_STOCK");
        assertEquals(0, stock(product));
        long count = pages(admin, "/api/admin/orders").stream()
                .filter(o -> o.get("items").findValues("productId").stream()
                        .anyMatch(value -> value.asLong() == product)).count();
        assertEquals(1, count);
    }

    @Test
    void simultaneousCancellationByOwnerAndAdminRestoresOnlyOnce() throws Exception {
        long product = product("6.25", 3, true).get("id").asLong();
        long id = order(alice, product, 2).get("id").asLong();
        List<ApiClient.Result> results = concurrent(alice, admin, "/api/orders/" + id + "/cancel", null);
        assertEquals(results.get(0).expect(200).body, results.get(1).expect(200).body);
        assertEquals("CANCELLED", results.get(0).body.get("status").asText());
        assertEquals(3, stock(product));
    }

    @Test
    void filtersPagesAndSummaryHonorExactTimeBoundsAndExcludeCancelled() throws Exception {
        long product = product("10.10", 10, true).get("id").asLong();
        JsonNode first = order(alice, product, 2);
        JsonNode cancelled = order(bob, product, 1);
        JsonNode atEnd = order(alice, product, 3);
        bob.write("POST", "/api/orders/" + cancelled.get("id").asLong() + "/cancel", null).expect(200);
        String period = "start=" + first.get("createdAt").asText() + "&end=" + atEnd.get("createdAt").asText();
        assertTrue(Instant.parse(first.get("createdAt").asText())
                .isBefore(Instant.parse(atEnd.get("createdAt").asText())));
        JsonNode page = admin.get("/api/admin/orders?" + period + "&size=1").expect(200).body;
        assertEquals(2, page.get("totalElements").asInt());
        assertEquals(2, page.get("totalPages").asInt());
        assertEquals(cancelled.get("id"), page.at("/content/0/id"));
        JsonNode secondPage = admin.get("/api/admin/orders?" + period + "&size=1&page=1").expect(200).body;
        assertEquals(first.get("id"), secondPage.at("/content/0/id"));
        JsonNode beyond = admin.get("/api/admin/orders?" + period + "&size=1&page=2").expect(200).body;
        assertTrue(beyond.get("content").isEmpty());
        assertEquals(2, beyond.get("totalElements").asInt());
        JsonNode filtered = admin.get("/api/admin/orders?" + period + "&customerId=2&status=CREATED").expect(200).body;
        assertEquals(1, filtered.get("totalElements").asInt());
        assertEquals(first.get("id"), filtered.at("/content/0/id"));
        JsonNode summary = admin.get("/api/admin/reports/orders?" + period).expect(200).body;
        assertEquals(Set.of("orderCount", "total"), fields(summary));
        assertEquals(1, summary.get("orderCount").asInt());
        money(summary, "total", "20.20");
        JsonNode empty = admin.get("/api/admin/reports/orders?" + period + "&customerId=3").expect(200).body;
        assertEquals(0, empty.get("orderCount").asInt());
        money(empty, "total", "0.00");
        assertTrue(admin.get("/api/admin/orders?customerId=9223372036854775807").expect(200)
                .body.get("content").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/admin/orders?status=PAID", "/api/admin/orders?customerId=0",
            "/api/admin/orders?start=invalid", "/api/admin/reports/orders?end=2026-01-01",
            "/api/admin/reports/orders?start=2026-01-01T00:00:00Z&end=2026-01-01T00:00:00Z"})
    void rejectsInvalidFilters(String path) throws Exception {
        admin.get(path).error(400, "INVALID_REQUEST");
    }

    private JsonNode product(String price, int stock, boolean active) throws Exception {
        return admin.write("POST", "/api/admin/products",
                productBody("EVAL-" + UUID.randomUUID(), price, stock, active)).expect(201).body;
    }

    private static String productBody(String sku, String price, int stock, boolean active) {
        return "{\"sku\":\"" + sku + "\",\"name\":\"Produto de avaliacao\",\"price\":" + price
                + ",\"stockQuantity\":" + stock + ",\"active\":" + active + "}";
    }

    private JsonNode order(ApiClient actor, long product, int quantity) throws Exception {
        return actor.write("POST", "/api/orders", items(product, quantity)).expect(201).body;
    }

    private static String items(long product, int quantity) {
        return "{\"items\":[{\"productId\":" + product + ",\"quantity\":" + quantity + "}]}";
    }

    private int stock(long product) throws Exception {
        return anonymous.get("/api/products/" + product).expect(200).body.get("stockQuantity").asInt();
    }

    private static Set<String> fields(JsonNode node) {
        Set<String> fields = new HashSet<>();
        node.fieldNames().forEachRemaining(fields::add);
        return fields;
    }

    private static void money(JsonNode node, String field, String expected) {
        assertTrue(node.get(field).isNumber(), field + " deve ser numero JSON");
        assertEquals(0, new BigDecimal(expected).compareTo(node.get(field).decimalValue()), field);
    }

    private static List<JsonNode> pages(ApiClient actor, String path) throws Exception {
        List<JsonNode> result = new ArrayList<>();
        for (int page = 0; page < 100; page++) {
            JsonNode response = actor.get(path + "?size=100&page=" + page).expect(200).body;
            response.get("content").forEach(result::add);
            if (page + 1 >= response.get("totalPages").asInt()) { return result; }
        }
        throw new AssertionError("Volume inesperado para banco isolado de avaliacao");
    }

    private static List<ApiClient.Result> concurrent(ApiClient first, ApiClient second,
                                                      String path, String payload) throws Exception {
        String firstToken = first.csrfToken();
        String secondToken = second.csrfToken();
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ApiClient.Result> a = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return first.writePrepared("POST", path, payload, firstToken);
            });
            Future<ApiClient.Result> b = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return second.writePrepared("POST", path, payload, secondToken);
            });
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
