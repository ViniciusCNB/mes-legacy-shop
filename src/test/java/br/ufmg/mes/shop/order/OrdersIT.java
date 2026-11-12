package br.ufmg.mes.shop.order;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import br.ufmg.mes.shop.support.HttpResult;
import br.ufmg.mes.shop.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(JUnitParamsRunner.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = OrdersIT.Initializer.class)
@Sql({"/db/catalog-fixture.sql", "/db/security-fixture.sql"})
public class OrdersIT {
    private static final String ALICE = "alice@test.local";
    private static final String BOB = "bob@test.local";
    private static final String ADMIN = "admin@test.local";
    private static final String ONE = "{\"items\":[{\"productId\":10,\"quantity\":1}]}";

    @ClassRule
    public static final PostgreSQLContainer<?> POSTGRES = PostgresTestDatabase.create("orders_test");

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            PostgresTestDatabase.applyTo(POSTGRES, context);
        }
    }

    @LocalServerPort
    private int port;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;
    private final CloseableHttpClient client = HttpClients.custom()
            .disableRedirectHandling().disableCookieManagement()
            .setMaxConnPerRoute(10).setMaxConnTotal(10).build();

    @After
    public void closeClient() throws IOException {
        client.close();
    }

    @Test
    public void calculatesAndPersistsExactMoneyAndStockWithStableItemOrder() throws Exception {
        HttpResult response = write(ALICE, "/api/orders",
                "{\"items\":[{\"productId\":30,\"quantity\":2},{\"productId\":10,\"quantity\":3}]}");
        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode order = body(response);
        long id = order.get("id").asLong();
        assertThat(response.header("Location")).isEqualTo("/api/orders/" + id);
        assertThat(order.size()).isEqualTo(7);
        assertThat(order.get("customerId").asLong()).isEqualTo(2);
        assertThat(order.get("status").asText()).isEqualTo("CREATED");
        assertThat(Instant.parse(order.get("createdAt").asText())).isBeforeOrEqualTo(Instant.now());
        assertThat(order.get("cancelledAt").isNull()).isTrue();
        assertThat(order.get("total").decimalValue()).isEqualByComparingTo("139.50");
        assertThat(order.get("items").findValuesAsText("productId")).containsExactly("10", "30");
        assertThat(order.at("/items/0").size()).isEqualTo(4);
        assertThat(order.at("/items/0/quantity").asInt()).isEqualTo(3);
        assertThat(order.at("/items/0/unitPrice").decimalValue()).isEqualByComparingTo("19.90");
        assertThat(order.at("/items/0/subtotal").decimalValue()).isEqualByComparingTo("59.70");
        assertThat(response.body()).contains("\"total\":139.50", "\"unitPrice\":19.90")
                .doesNotContain("password", "hash", "email", "hibernate");
        assertThat(stock(10)).isEqualTo(9);
        assertThat(stock(30)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT total FROM shop.purchase_orders WHERE id = ?", BigDecimal.class, id))
                .isEqualTo(new BigDecimal("139.50"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM shop.order_items WHERE order_id = ?", Long.class, id))
                .isEqualTo(2);
        assertThat(body(get(ALICE, "/api/orders/" + id))).isEqualTo(order);
    }

    @Test
    public void preservesPurchasedPriceAfterAdministrativePriceAndActivityChanges() throws Exception {
        JsonNode order = create(ALICE, ONE);
        long id = order.get("id").asLong();
        HttpResult updated = request("PUT", "/api/admin/products/10",
                "{\"name\":\"Reajustado\",\"price\":99.99,\"active\":false}", csrf(ADMIN));
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(body(get(ADMIN, "/api/orders/" + id))).isEqualTo(order);
        JsonNode summary = body(get(ADMIN, "/api/admin/reports/orders"));
        assertThat(summary.get("orderCount").asLong()).isEqualTo(1);
        assertThat(summary.get("total").decimalValue()).isEqualByComparingTo("19.90");
        assertThat(write(ALICE, "/api/orders/" + id + "/cancel", null).statusCode()).isEqualTo(200);
        assertThat(stock(10)).isEqualTo(12);
    }

    @Test
    @Parameters(method = "invalidBodies")
    public void rejectsInvalidInputWithoutCreatingOrdersOrChangingStock(String payload) throws Exception {
        error(write(ALICE, "/api/orders", payload), 400, "INVALID_REQUEST");
        unchanged();
    }

    @SuppressWarnings("unused")
    private Object[] invalidBodies() {
        return new Object[] {
                new Object[] {"{}"},
                new Object[] {"{\"items\":null}"},
                new Object[] {"{\"items\":[]}"},
                new Object[] {"{\"items\":[null]}"},
                new Object[] {"{\"items\":[{}]}"},
                new Object[] {"{\"items\":[{\"productId\":0,\"quantity\":1}]}"},
                new Object[] {"{\"items\":[{\"productId\":10,\"quantity\":0}]}"},
                new Object[] {"{\"items\":[{\"productId\":10,\"quantity\":-1}]}"},
                new Object[] {"{\"items\":[{\"productId\":10,\"quantity\":1.5}]}"},
                new Object[] {"{\"items\":[{\"productId\":10,\"quantity\":\"1\"}]}"},
                new Object[] {"{\"items\":[{\"productId\":10,\"quantity\":2147483648}]}"},
                new Object[] {"{\"items\":[{\"productId\":10,\"quantity\":1,\"unitPrice\":0.01}]}"},
                new Object[] {ONE.replace("]}", "],\"customerId\":3}")},
                new Object[] {ONE.replace("]}", "],\"total\":0.01}")},
                new Object[] {"{\"items\":[{\"productId\":10,\"quantity\":1},{\"productId\":10,\"quantity\":1}]}"},
                new Object[] {"{\"items\":[" + String.join("", Collections.nCopies(100,
                        "{\"productId\":10,\"quantity\":1},")) + "{\"productId\":10,\"quantity\":1}]}"},
                new Object[] {"{"}
        };
    }

    @Test
    @Parameters(method = "invalidProducts")
    public void rollsBackAllStockChangesWhenALaterItemFails(long productId, int quantity, int status, String code)
            throws Exception {
        error(write(ALICE, "/api/orders", "{\"items\":[{\"productId\":10,\"quantity\":2},"
                + "{\"productId\":" + productId + ",\"quantity\":" + quantity + "}]}"), status, code);
        unchanged();
    }

    @SuppressWarnings("unused")
    private Object[] invalidProducts() {
        return new Object[] {
                new Object[] {20L, 1, 409, "PRODUCT_INACTIVE"},
                new Object[] {999L, 1, 404, "PRODUCT_NOT_FOUND"},
                new Object[] {30L, 6, 409, "INSUFFICIENT_STOCK"},
                new Object[] {40L, 1, 409, "INSUFFICIENT_STOCK"}
        };
    }

    @Test
    public void isolatesCustomersAndRestrictsRolesAndCsrf() throws Exception {
        long alice = create(ALICE, ONE).get("id").asLong();
        long bob = create(BOB, ONE).get("id").asLong();
        assertThat(body(get(ALICE, "/api/orders")).get("content").findValuesAsText("id"))
                .containsExactly(Long.toString(alice));
        assertThat(body(get(BOB, "/api/orders")).get("content").findValuesAsText("id"))
                .containsExactly(Long.toString(bob));
        HttpResult hidden = get(BOB, "/api/orders/" + alice);
        error(hidden, 404, "ORDER_NOT_FOUND");
        assertThat(hidden.body()).isEqualTo(get(BOB, "/api/orders/999").body());
        error(write(BOB, "/api/orders/" + alice + "/cancel", null), 404, "ORDER_NOT_FOUND");
        assertThat(get(ADMIN, "/api/orders/" + alice).statusCode()).isEqualTo(200);
        error(write(ADMIN, "/api/orders", ONE), 403, "FORBIDDEN");
        error(get(ADMIN, "/api/orders"), 403, "FORBIDDEN");
        error(get(ALICE, "/api/admin/orders"), 403, "FORBIDDEN");
        error(get(ALICE, "/api/admin/reports/orders"), 403, "FORBIDDEN");
        error(request("GET", "/api/orders", null, new HttpHeaders()), 401, "AUTHENTICATION_REQUIRED");
        error(request("GET", "/api/orders/" + alice, null, new HttpHeaders()), 401, "AUTHENTICATION_REQUIRED");
        error(request("POST", "/api/orders", ONE, credentials(ALICE)), 403, "CSRF_INVALID");
        error(request("POST", "/api/orders/" + alice + "/cancel", null, credentials(ALICE)), 403, "CSRF_INVALID");
        HttpHeaders tokenOnly = csrf(ALICE);
        tokenOnly.remove(HttpHeaders.AUTHORIZATION);
        error(request("POST", "/api/orders", ONE, tokenOnly), 401, "AUTHENTICATION_REQUIRED");
        assertThat(stock(10)).isEqualTo(10);
        assertThat(countOrders()).isEqualTo(2);
    }

    @Test
    public void cancelsOnceForOwnerOrAdminAndKeepsCancellationInstant() throws Exception {
        long id = create(ALICE, "{\"items\":[{\"productId\":30,\"quantity\":2},{\"productId\":10,\"quantity\":3}]}")
                .get("id").asLong();
        HttpResult first = write(ADMIN, "/api/orders/" + id + "/cancel", null);
        assertThat(first.statusCode()).isEqualTo(200);
        JsonNode cancelled = body(first);
        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(Instant.parse(cancelled.get("cancelledAt").asText()))
                .isAfterOrEqualTo(Instant.parse(cancelled.get("createdAt").asText()));
        HttpResult again = write(ALICE, "/api/orders/" + id + "/cancel", null);
        assertThat(again.statusCode()).isEqualTo(200);
        assertThat(body(again)).isEqualTo(cancelled);
        assertThat(stock(10)).isEqualTo(12);
        assertThat(stock(30)).isEqualTo(5);
        assertThat(countOrders()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM shop.order_items", Long.class)).isEqualTo(2);
        assertThat(body(get(ADMIN, "/api/admin/reports/orders")).get("total").decimalValue())
                .isEqualByComparingTo("0.00");
        error(write(ALICE, "/api/orders/999/cancel", null), 404, "ORDER_NOT_FOUND");
    }

    @Test
    public void rollsBackCancellationWhenRestockingWouldOverflowAndAllowsCorrection() throws Exception {
        long id = create(ALICE, "{\"items\":[{\"productId\":10,\"quantity\":1},{\"productId\":30,\"quantity\":1}]}")
                .get("id").asLong();
        jdbc.update("UPDATE shop.products SET stock_quantity = 2147483647 WHERE id = 30");
        error(write(ALICE, "/api/orders/" + id + "/cancel", null), 409, "STOCK_LIMIT_EXCEEDED");
        assertThat(stock(10)).isEqualTo(11);
        assertThat(stock(30)).isEqualTo(Integer.MAX_VALUE);
        assertThat(body(get(ALICE, "/api/orders/" + id)).get("status").asText()).isEqualTo("CREATED");
        assertThat(body(get(ALICE, "/api/orders/" + id)).get("cancelledAt").isNull()).isTrue();
        assertThat(request("PATCH", "/api/admin/products/30/stock", "{\"stockQuantity\":4}", csrf(ADMIN))
                .statusCode()).isEqualTo(200);
        assertThat(write(ALICE, "/api/orders/" + id + "/cancel", null).statusCode()).isEqualTo(200);
        assertThat(stock(10)).isEqualTo(12);
        assertThat(stock(30)).isEqualTo(5);
    }

    @Test
    public void admitsOnlyOneConcurrentPurchaseOfTheLastUnit() throws Exception {
        jdbc.update("UPDATE shop.products SET stock_quantity = 1 WHERE id = 10");
        List<HttpResult> responses = concurrentWrites("/api/orders", ONE,
                "SELECT id FROM shop.products WHERE id = 10 FOR UPDATE");
        assertThat(responses).extracting(HttpResult::statusCode).containsExactlyInAnyOrder(201, 409);
        error(responses.stream().filter(r -> r.statusCode() == 409).findFirst().get(),
                409, "INSUFFICIENT_STOCK");
        assertThat(stock(10)).isZero();
        assertThat(countOrders()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM shop.order_items", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT sum(quantity) FROM shop.order_items", Long.class)).isEqualTo(1);
    }

    @Test
    public void concurrentCancellationsRestoreStockOnlyOnce() throws Exception {
        long id = create(ALICE, ONE).get("id").asLong();
        // The administrator and owner can both cancel this order.
        List<HttpResult> responses = concurrentWrites("/api/orders/" + id + "/cancel", null,
                "SELECT id FROM shop.purchase_orders WHERE id = " + id + " FOR UPDATE", ALICE, ADMIN);
        assertThat(responses).extracting(HttpResult::statusCode).containsExactly(200, 200);
        assertThat(body(responses.get(0))).isEqualTo(body(responses.get(1)));
        assertThat(body(responses.get(0)).get("status").asText()).isEqualTo("CANCELLED");
        assertThat(stock(10)).isEqualTo(12);
        assertThat(countOrders()).isEqualTo(1);
    }

    @Test
    public void filtersPagesAndSummarizesNonCancelledOrdersAtExactTimeBoundaries() throws Exception {
        long first = create(ALICE, ONE.replace("\"quantity\":1", "\"quantity\":2")).get("id").asLong();
        long second = create(BOB, "{\"items\":[{\"productId\":30,\"quantity\":1}]}").get("id").asLong();
        long cancelled = create(ALICE, ONE).get("id").asLong();
        long atEnd = create(ALICE, ONE).get("id").asLong();
        assertThat(write(ALICE, "/api/orders/" + cancelled + "/cancel", null).statusCode()).isEqualTo(200);
        jdbc.update("UPDATE shop.purchase_orders SET created_at = '2026-01-01T00:00:00Z' WHERE id = ?", first);
        jdbc.update("UPDATE shop.purchase_orders SET created_at = '2026-01-01T12:00:00Z' WHERE id IN (?, ?)",
                second, cancelled);
        jdbc.update("UPDATE shop.purchase_orders SET created_at = '2026-01-02T00:00:00Z' WHERE id = ?", atEnd);
        String period = "start=2026-01-01T00:00:00Z&end=2026-01-02T00:00:00Z";
        JsonNode summary = body(get(ADMIN, "/api/admin/reports/orders?" + period));
        assertThat(summary.size()).isEqualTo(2);
        assertThat(summary.get("orderCount").asLong()).isEqualTo(2);
        assertThat(summary.get("total").decimalValue()).isEqualByComparingTo("79.70");
        JsonNode own = body(get(ALICE, "/api/orders?size=2"));
        assertThat(own.size()).isEqualTo(5);
        assertThat(own.get("totalElements").asLong()).isEqualTo(3);
        assertThat(own.get("totalPages").asInt()).isEqualTo(2);
        assertThat(own.get("content").findValuesAsText("id"))
                .containsExactly(Long.toString(atEnd), Long.toString(cancelled));
        assertThat(body(get(ALICE, "/api/orders?page=1&size=2")).get("content").findValuesAsText("id"))
                .containsExactly(Long.toString(first));
        JsonNode beyond = body(get(ALICE, "/api/orders?page=2&size=2"));
        assertThat(beyond.get("content")).isEmpty();
        assertThat(beyond.get("totalElements").asLong()).isEqualTo(3);
        JsonNode filtered = body(get(ADMIN, "/api/admin/orders?" + period + "&customerId=2&status=CREATED"));
        assertThat(filtered.get("content").findValuesAsText("id")).containsExactly(Long.toString(first));
        assertThat(filtered.get("totalElements").asLong()).isEqualTo(1);
        JsonNode cancelledPage = body(get(ADMIN, "/api/admin/orders?status=CANCELLED"));
        assertThat(cancelledPage.get("content").findValuesAsText("id")).containsExactly(Long.toString(cancelled));
        assertThat(body(get(ADMIN, "/api/admin/orders?customerId=999")).get("content")).isEmpty();
        JsonNode customerSummary = body(get(ADMIN, "/api/admin/reports/orders?" + period + "&customerId=2"));
        assertThat(customerSummary.get("orderCount").asLong()).isEqualTo(1);
        assertThat(customerSummary.get("total").decimalValue()).isEqualByComparingTo("39.80");
        assertThat(body(get(ADMIN, "/api/admin/reports/orders?start=2026-01-02T00:00:00Z"))
                .get("orderCount").asLong()).isEqualTo(1);
        assertThat(body(get(ADMIN, "/api/admin/reports/orders?end=2026-01-01T00:00:00Z"))
                .get("orderCount").asLong()).isZero();
    }

    @Test
    public void returnsEmptyPagesAndExactZeroSummaryWithoutOrders() throws Exception {
        JsonNode page = body(get(ALICE, "/api/orders"));
        assertThat(page.get("content")).isEmpty();
        assertThat(page.get("totalPages").asInt()).isZero();
        assertThat(page.get("page").asInt()).isZero();
        assertThat(page.get("size").asInt()).isEqualTo(20);
        assertThat(page.get("totalElements").asLong()).isZero();
        assertThat(get(ADMIN, "/api/admin/reports/orders").body()).contains("\"orderCount\":0", "\"total\":0.00");
    }

    @Test
    @Parameters(method = "invalidQueries")
    public void rejectsInvalidQueryParameters(String path) throws Exception {
        error(get(path.startsWith("/api/admin") ? ADMIN : ALICE, path), 400, "INVALID_REQUEST");
    }

    @SuppressWarnings("unused")
    private Object[] invalidQueries() {
        return new Object[] {
                new Object[] {"/api/orders?page=-1"},
                new Object[] {"/api/orders?size=101"},
                new Object[] {"/api/orders?page=1000001"},
                new Object[] {"/api/orders/0"},
                new Object[] {"/api/orders/abc"},
                new Object[] {"/api/admin/orders?status=PAID"},
                new Object[] {"/api/admin/orders?customerId=0"},
                new Object[] {"/api/admin/orders?start=yesterday"},
                new Object[] {"/api/admin/reports/orders?end=2026-01-01"},
                new Object[] {"/api/admin/orders?start=2026-01-02T00:00:00Z&end=2026-01-01T00:00:00Z"},
                new Object[] {"/api/admin/reports/orders?start=2026-01-01T00:00:00Z&end=2026-01-01T00:00:00Z"}
        };
    }

    @Test
    public void supportsMaximumUnitPriceAndQuantityWithoutRoundingOrNumericOverflow() throws Exception {
        BigDecimal price = new BigDecimal("99999999999999999.99");
        jdbc.update("UPDATE shop.products SET price = ?, stock_quantity = 2147483647 WHERE id = 10", price);
        JsonNode order = create(ALICE, ONE.replace("\"quantity\":1", "\"quantity\":2147483647"));
        BigDecimal expected = price.multiply(BigDecimal.valueOf(Integer.MAX_VALUE));
        assertThat(order.get("total").decimalValue()).isEqualByComparingTo(expected);
        assertThat(jdbc.queryForObject("SELECT total FROM shop.purchase_orders", BigDecimal.class)).isEqualTo(expected);
        assertThat(stock(10)).isZero();
    }

    @Test
    @Parameters(method = "constraintViolations")
    public void enforcesOrderConstraintsInPostgres(final String statement) throws Exception {
        create(ALICE, ONE);
        assertThatThrownBy(() -> jdbc.update(statement)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(stock(10)).isEqualTo(11);
        assertThat(countOrders()).isEqualTo(1);
    }

    @SuppressWarnings("unused")
    private Object[] constraintViolations() {
        return new Object[] {
                new Object[] {"UPDATE shop.order_items SET quantity = 0"},
                new Object[] {"UPDATE shop.order_items SET subtotal = 0.01"},
                new Object[] {"UPDATE shop.order_items SET product_id = 999"},
                new Object[] {"UPDATE shop.purchase_orders SET status = 'PAID'"},
                new Object[] {"UPDATE shop.purchase_orders SET status = 'CANCELLED'"},
                new Object[] {"UPDATE shop.purchase_orders SET total = 0"},
                new Object[] {"INSERT INTO shop.order_items (order_id, product_id, quantity, unit_price, subtotal) "
                        + "SELECT order_id, product_id, quantity, unit_price, subtotal FROM shop.order_items"}
        };
    }

    private List<HttpResult> concurrentWrites(final String path, final String payload, String lockSql,
                                              String... emails) throws Exception {
        String[] actors = emails.length == 0 ? new String[] {ALICE, BOB} : emails;
        final HttpHeaders first = csrf(actors[0]);
        final HttpHeaders second = csrf(actors[1]);
        final CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (Connection held = dataSource.getConnection()) {
            held.setAutoCommit(false);
            try {
                try (PreparedStatement lock = held.prepareStatement(lockSql)) {
                    lock.execute();
                }
                Future<HttpResult> a = executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    return request("POST", path, payload, first);
                });
                Future<HttpResult> b = executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    return request("POST", path, payload, second);
                });
                // Release only after both HTTP transactions actually contend in PostgreSQL.
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                long waiting;
                do {
                    waiting = jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity "
                            + "WHERE datname = current_database() AND wait_event_type = 'Lock'", Long.class);
                    if (waiting < 2) { Thread.sleep(20); }
                } while (waiting < 2 && System.nanoTime() < deadline);
                assertThat(waiting).as("Both HTTP transactions must reach the database lock").isGreaterThanOrEqualTo(2);
                held.commit();
                return Arrays.asList(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
            } finally {
                held.rollback();
            }
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private JsonNode create(String email, String payload) throws Exception {
        HttpResult response = write(email, "/api/orders", payload);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return body(response);
    }

    private HttpResult write(String email, String path, String payload) throws Exception {
        return request("POST", path, payload, csrf(email));
    }

    private HttpResult get(String email, String path) throws Exception {
        HttpResult response = request("GET", path, null, credentials(email));
        if (response.statusCode() == 200) {
            assertThat(response.body()).doesNotContain("password", "hash", "hibernate");
        }
        return response;
    }

    private HttpHeaders csrf(String email) throws Exception {
        HttpResult response = get(email, "/api/csrf");
        assertThat(response.statusCode()).isEqualTo(200);
        String token = body(response).get("token").asText();
        String cookie = response.headers("Set-Cookie").stream()
                .filter(value -> value.startsWith("XSRF-TOKEN=" + token + ";")).findFirst()
                .orElseThrow(() -> new AssertionError("XSRF-TOKEN cookie not issued"));
        HttpHeaders headers = credentials(email);
        headers.set(HttpHeaders.COOKIE, cookie.substring(0, cookie.indexOf(';')));
        headers.set(body(response).get("headerName").asText(), token);
        return headers;
    }

    private HttpHeaders credentials(String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(email, "test-password");
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        return headers;
    }

    private HttpResult request(String method, String path, String payload, HttpHeaders headers) throws IOException {
        return HttpResult.execute(client, method, "http://127.0.0.1:" + port + path, payload, headers);
    }

    private JsonNode body(HttpResult response) throws IOException {
        return json.reader().with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(response.body());
    }

    private void error(HttpResult response, int status, String code) throws IOException {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(body(response).get("code").asText()).isEqualTo(code);
        assertThat(response.body()).doesNotContain("password_hash", "$2a$", "org.springframework", "stackTrace");
    }

    private int stock(long id) {
        return jdbc.queryForObject("SELECT stock_quantity FROM shop.products WHERE id = ?", Integer.class, id);
    }

    private long countOrders() {
        return jdbc.queryForObject("SELECT count(*) FROM shop.purchase_orders", Long.class);
    }

    private void unchanged() {
        assertThat(countOrders()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM shop.order_items", Long.class)).isZero();
        assertThat(stock(10)).isEqualTo(12);
        assertThat(stock(30)).isEqualTo(5);
    }
}
