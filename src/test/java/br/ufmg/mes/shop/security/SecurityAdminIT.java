package br.ufmg.mes.shop.security;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Pattern;

import br.ufmg.mes.shop.support.HttpResult;
import br.ufmg.mes.shop.support.PostgresTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = SecurityAdminIT.Initializer.class)
@Sql({"/db/catalog-fixture.sql", "/db/security-fixture.sql"})
public class SecurityAdminIT {

    private static final String ADMIN = "admin@test.local";
    private static final String CUSTOMER = "alice@test.local";
    private static final String PASSWORD = "test-password";
    private static final String CREATE = "{\"sku\":\"ADMIN-NEW\",\"name\":\"Novo produto\","
            + "\"price\":12.30,\"stockQuantity\":5,\"active\":true}";
    private static final String UPDATE = "{\"name\":\"Produto revisado\",\"price\":20.50,\"active\":false}";
    private static final String STOCK = "{\"stockQuantity\":7}";

    @ClassRule
    public static final PostgreSQLContainer<?> POSTGRES = PostgresTestDatabase.create("security_test");

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
    private PasswordEncoder passwords;

    private final CloseableHttpClient client = HttpClients.custom()
            .disableRedirectHandling().disableCookieManagement().build();

    @After
    public void closeClient() throws IOException {
        client.close();
    }

    @Test
    @Parameters({"admin@test.local, 1, ADMIN", "alice@test.local, 2, CUSTOMER", "bob@test.local, 3, CUSTOMER"})
    public void authenticatesDatabaseUsersAndExposesOnlyProfileFields(String email, long id, String role)
            throws Exception {
        HttpResult response = request("GET", "/api/me", null, credentials(email, PASSWORD));
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode profile = body(response);
        assertThat(profile.size()).isEqualTo(4);
        assertThat(profile.get("id").asLong()).isEqualTo(id);
        assertThat(profile.get("email").asText()).isEqualTo(email);
        assertThat(profile.get("name").asText()).isNotBlank();
        assertThat(profile.get("role").asText()).isEqualTo(role);
        assertThat(response.body()).doesNotContain("password", "hash", PASSWORD, "$2");
        assertThat(response.headers("Set-Cookie")).noneMatch(value -> value.contains("JSESSIONID"));
        String storedHash = jdbc.queryForObject("SELECT password_hash FROM shop.app_users WHERE id = ?",
                String.class, id);
        assertThat(storedHash).startsWith("$2a$10$").isNotEqualTo(PASSWORD);
        assertThat(passwords.matches(PASSWORD, storedHash)).isTrue();
    }

    @Test
    @Parameters({"unknown@test.local, test-password", "admin@test.local, wrong", "disabled@test.local, test-password"})
    public void rejectsUnknownWrongPasswordAndInactiveUsersWithoutRevealingWhich(String email, String password)
            throws Exception {
        HttpResult response = request("GET", "/api/me", null, credentials(email, password));
        assertError(response, 401, "AUTHENTICATION_REQUIRED");
        assertThat(response.header("WWW-Authenticate")).isEqualTo("Basic realm=\"mes-legacy-shop\"");
        assertThat(response.body()).doesNotContain(email, password, "Disabled", "UsernameNotFound");
    }

    @Test
    public void keepsCatalogPublicButRejectsInvalidCredentialsWhenSupplied() throws Exception {
        assertThat(request("GET", "/api/products", null, new HttpHeaders()).statusCode()).isEqualTo(200);
        assertError(request("GET", "/api/products", null, credentials(ADMIN, "wrong")),
                401, "AUTHENTICATION_REQUIRED");
        assertError(request("GET", "/api/me", null, new HttpHeaders()), 401, "AUTHENTICATION_REQUIRED");
        assertError(request("GET", "/api/csrf", null, new HttpHeaders()), 401, "AUTHENTICATION_REQUIRED");
        assertError(request("GET", "/actuator/env", null, credentials(ADMIN, PASSWORD)), 403, "FORBIDDEN");
    }

    @Test
    public void issuesAnHttpOnlyCsrfCookieWithoutCreatingAnAuthenticationSession() throws Exception {
        HttpHeaders headers = csrf(ADMIN);
        headers.remove(HttpHeaders.AUTHORIZATION);
        assertError(request("GET", "/api/me", null, headers), 401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    public void reloadsRolesAndActiveStatusFromTheDatabaseOnEachRequest() throws Exception {
        HttpHeaders adminHeaders = csrf(ADMIN);
        jdbc.update("UPDATE shop.app_users SET role = 'CUSTOMER' WHERE email = ?", ADMIN);
        assertError(request("POST", "/api/admin/products", CREATE, adminHeaders), 403, "FORBIDDEN");
        jdbc.update("UPDATE shop.app_users SET active = false WHERE email = ?", CUSTOMER);
        assertError(request("GET", "/api/me", null, credentials(CUSTOMER, PASSWORD)),
                401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    @Parameters(method = "writeOperations")
    public void forbidsCustomersFromEachAdministrativeOperation(String method, String path, String payload)
            throws Exception {
        assertError(request(method, path, payload, csrf(CUSTOMER)), 403, "FORBIDDEN");
        assertOriginalProductsUnchanged();
    }

    @Test
    @Parameters(method = "writeOperations")
    public void requiresAuthenticationForWritesWithAValidCsrfToken(String method, String path, String payload)
            throws Exception {
        HttpHeaders headers = csrf(ADMIN);
        headers.remove(HttpHeaders.AUTHORIZATION);
        assertError(request(method, path, payload, headers), 401, "AUTHENTICATION_REQUIRED");
        headers.setBasicAuth(ADMIN, "wrong");
        assertError(request(method, path, payload, headers), 401, "AUTHENTICATION_REQUIRED");
        assertOriginalProductsUnchanged();
    }

    @Test
    @Parameters(method = "writeOperations")
    public void rejectsMissingOrMismatchedCsrfEvenForAdmin(String method, String path, String payload)
            throws Exception {
        assertError(request(method, path, payload, credentials(ADMIN, PASSWORD)), 403, "CSRF_INVALID");
        HttpHeaders headers = csrf(ADMIN);
        headers.set("X-XSRF-TOKEN", "invalid-token");
        assertError(request(method, path, payload, headers), 403, "CSRF_INVALID");
        assertOriginalProductsUnchanged();
    }

    @SuppressWarnings("unused")
    private Object[] writeOperations() {
        return new Object[] {
                new Object[] {"POST", "/api/admin/products", CREATE},
                new Object[] {"PUT", "/api/admin/products/10", UPDATE},
                new Object[] {"PATCH", "/api/admin/products/10/stock", STOCK}
        };
    }

    @Test
    public void createsUpdatesInactivatesAndSetsAbsoluteStockWithoutDeletingTheProduct() throws Exception {
        HttpResult created = request("POST", "/api/admin/products", CREATE, csrf(ADMIN));
        assertThat(created.statusCode()).isEqualTo(201);
        JsonNode product = body(created);
        assertThat(product.size()).isEqualTo(6);
        long id = product.get("id").asLong();
        assertThat(product.get("sku").asText()).isEqualTo("ADMIN-NEW");
        assertThat(product.get("name").asText()).isEqualTo("Novo produto");
        assertThat(product.get("price").decimalValue()).isEqualByComparingTo("12.30");
        assertThat(product.get("stockQuantity").asInt()).isEqualTo(5);
        assertThat(product.get("active").asBoolean()).isTrue();
        assertThat(request("GET", "/api/products/" + id, null, new HttpHeaders()).statusCode()).isEqualTo(200);

        HttpResult updated = request("PUT", "/api/admin/products/" + id, UPDATE, csrf(ADMIN));
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(body(updated).get("active").asBoolean()).isFalse();
        assertThat(body(updated).get("sku").asText()).isEqualTo("ADMIN-NEW");
        assertThat(body(updated).get("stockQuantity").asInt()).isEqualTo(5);
        assertError(request("GET", "/api/products/" + id, null, new HttpHeaders()), 404, "PRODUCT_NOT_FOUND");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM shop.products WHERE id = ?", Long.class, id))
                .isEqualTo(1L);

        HttpResult stock = request("PATCH", "/api/admin/products/" + id + "/stock", STOCK, csrf(ADMIN));
        assertThat(stock.statusCode()).isEqualTo(200);
        assertThat(body(stock).get("stockQuantity").asInt()).isEqualTo(7);
        assertThat(body(stock).get("price").decimalValue()).isEqualByComparingTo("20.50");
        assertThat(body(stock).get("active").asBoolean()).isFalse();
        assertThat(body(request("PATCH", "/api/admin/products/" + id + "/stock", STOCK, csrf(ADMIN)))
                .get("stockQuantity").asInt()).isEqualTo(7);
        HttpResult zero = request("PATCH", "/api/admin/products/" + id + "/stock",
                "{\"stockQuantity\":0}", csrf(ADMIN));
        assertThat(zero.statusCode()).isEqualTo(200);
        assertThat(body(zero).get("stockQuantity").asInt()).isZero();

        assertThat(request("PUT", "/api/admin/products/" + id, UPDATE.replace("false", "true"), csrf(ADMIN))
                .statusCode()).isEqualTo(200);
        JsonNode visible = body(request("GET", "/api/products/" + id, null, new HttpHeaders()));
        assertThat(visible.get("name").asText()).isEqualTo("Produto revisado");
        assertThat(visible.get("price").decimalValue()).isEqualByComparingTo("20.50");
        assertThat(visible.get("stockQuantity").asInt()).isZero();
    }

    @Test
    @Parameters({"0.1, 0.10", "12, 12.00"})
    public void representsValidMoneyWithTwoDecimalPlacesOnCreateAndUpdate(String price, String expected)
            throws Exception {
        HttpResult created = request("POST", "/api/admin/products", CREATE.replace("12.30", price), csrf(ADMIN));
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).containsPattern("\"price\":" + Pattern.quote(expected) + "[,}]");
        long id = body(created).get("id").asLong();
        HttpResult updated = request("PUT", "/api/admin/products/" + id,
                UPDATE.replace("20.50", price), csrf(ADMIN));
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(updated.body()).containsPattern("\"price\":" + Pattern.quote(expected) + "[,}]");
        assertThat(jdbc.queryForObject("SELECT price FROM shop.products WHERE id = ?", BigDecimal.class, id))
                .isEqualTo(new BigDecimal(expected));
    }

    @Test
    public void reportsDuplicateSkuAsConflictWithoutChangingExistingData() throws Exception {
        String duplicate = CREATE.replace("ADMIN-NEW", "CATALOG-10");
        assertError(request("POST", "/api/admin/products", duplicate, csrf(ADMIN)), 409, "SKU_ALREADY_EXISTS");
        assertOriginalProductsUnchanged();
    }

    @Test
    @Parameters({"PUT, /api/admin/products/999", "PATCH, /api/admin/products/999/stock"})
    public void reportsMissingProducts(String method, String path) throws Exception {
        assertError(request(method, path, method.equals("PUT") ? UPDATE : STOCK, csrf(ADMIN)),
                404, "PRODUCT_NOT_FOUND");
        assertOriginalProductsUnchanged();
    }

    @Test
    @Parameters(method = "invalidWrites")
    public void rejectsInvalidBodiesWithoutPartialChanges(String method, String path, String payload, String field)
            throws Exception {
        HttpResult response = request(method, path, payload, csrf(ADMIN));
        assertError(response, 400, "INVALID_REQUEST");
        if (field != null) {
            assertThat(body(response).get("fieldErrors").findValuesAsText("field")).contains(field);
        }
        assertOriginalProductsUnchanged();
    }

    @SuppressWarnings("unused")
    private Object[] invalidWrites() {
        return new Object[] {
                new Object[] {"POST", "/api/admin/products", CREATE.replace("ADMIN-NEW", " "), "sku"},
                new Object[] {"POST", "/api/admin/products", CREATE.replace("12.30", "0"), "price"},
                new Object[] {"POST", "/api/admin/products", CREATE.replace("12.30", "1.001"), "price"},
                new Object[] {"POST", "/api/admin/products",
                        CREATE.replace("\"stockQuantity\":5", "\"stockQuantity\":-1"), "stockQuantity"},
                new Object[] {"POST", "/api/admin/products",
                        CREATE.replace("\"stockQuantity\":5", "\"stockQuantity\":1.5"), null},
                new Object[] {"POST", "/api/admin/products",
                        CREATE.replace("\"name\":\"Novo produto\"", "\"name\":123"), null},
                new Object[] {"POST", "/api/admin/products",
                        CREATE.replace("\"active\":true", "\"active\":\"true\""), null},
                new Object[] {"POST", "/api/admin/products", "{", null},
                new Object[] {"PUT", "/api/admin/products/10", UPDATE.replace("Produto revisado", " "), "name"},
                new Object[] {"PUT", "/api/admin/products/10", UPDATE.replace("}", ",\"sku\":\"OTHER\"}"), null},
                new Object[] {"PUT", "/api/admin/products/10", UPDATE.replace("}", ",\"stockQuantity\":99}"), null},
                new Object[] {"PUT", "/api/admin/products/0", UPDATE, "id"},
                new Object[] {"PATCH", "/api/admin/products/10/stock", "{\"stockQuantity\":-1}", "stockQuantity"},
                new Object[] {"PATCH", "/api/admin/products/10/stock", "{}", "stockQuantity"},
                new Object[] {"PATCH", "/api/admin/products/10/stock", "{\"stockQuantity\":1.5}", null},
                new Object[] {"PATCH", "/api/admin/products/10/stock", "{\"stockQuantity\":\"1\"}", null}
        };
    }

    @Test
    public void refreshesCsrfWithARealCookieJarBeforeEachAuthenticatedWrite() throws Exception {
        CookieStore cookies = new BasicCookieStore();
        try (CloseableHttpClient browser = HttpClients.custom().setDefaultCookieStore(cookies).build()) {
            for (int stock : new int[] {7, 0}) {
                HttpResult token = request(browser, "GET", "/api/csrf", null, credentials(ADMIN, PASSWORD));
                assertThat(token.statusCode()).isEqualTo(200);
                HttpHeaders headers = credentials(ADMIN, PASSWORD);
                headers.set(body(token).get("headerName").asText(), body(token).get("token").asText());
                HttpResult response = request(browser, "PATCH", "/api/admin/products/10/stock",
                        "{\"stockQuantity\":" + stock + "}", headers);
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(body(response).get("stockQuantity").asInt()).isEqualTo(stock);
                assertThat(cookies.getCookies()).noneMatch(cookie -> cookie.getName().equals("JSESSIONID"));
            }
        }
    }

    private HttpHeaders csrf(String email) throws Exception {
        HttpResult response = request("GET", "/api/csrf", null, credentials(email, PASSWORD));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.header("Cache-Control")).contains("no-store");
        assertThat(response.headers("Set-Cookie")).noneMatch(value -> value.contains("JSESSIONID"));
        String token = body(response).get("token").asText();
        String headerName = body(response).get("headerName").asText();
        assertThat(token).isNotBlank();
        assertThat(headerName).isEqualTo("X-XSRF-TOKEN");
        String cookie = response.headers("Set-Cookie").stream()
                .filter(value -> value.startsWith("XSRF-TOKEN=" + token + ";"))
                .findFirst().orElseThrow(() -> new AssertionError("XSRF-TOKEN cookie not issued"));
        assertThat(cookie).contains("HttpOnly", "Path=/");
        HttpHeaders headers = credentials(email, PASSWORD);
        headers.set(HttpHeaders.COOKIE, cookie.substring(0, cookie.indexOf(';')));
        headers.set(headerName, token);
        return headers;
    }

    private HttpHeaders credentials(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(email, password);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpResult request(String method, String path, String payload, HttpHeaders headers)
            throws IOException {
        return request(client, method, path, payload, headers);
    }

    private HttpResult request(CloseableHttpClient httpClient, String method, String path, String payload,
                               HttpHeaders headers) throws IOException {
        return HttpResult.execute(httpClient, method, "http://127.0.0.1:" + port + path, payload, headers);
    }

    private JsonNode body(HttpResult response) throws IOException {
        return json.readTree(response.body());
    }

    private void assertError(HttpResult response, int status, String code) throws IOException {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(body(response).get("code").asText()).isEqualTo(code);
        assertThat(body(response).get("message").asText()).isNotBlank();
        assertThat(response.body()).doesNotContain("password_hash", "$2a$", "org.springframework", "stackTrace");
    }

    private void assertOriginalProductsUnchanged() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM shop.products", Long.class)).isEqualTo(4L);
        assertThat(jdbc.queryForObject("SELECT name FROM shop.products WHERE id = 10", String.class))
                .isEqualTo("Primeiro produto");
        assertThat(jdbc.queryForObject("SELECT stock_quantity FROM shop.products WHERE id = 10", Integer.class))
                .isEqualTo(12);
        assertThat(jdbc.queryForObject("SELECT price FROM shop.products WHERE id = 10", BigDecimal.class))
                .isEqualByComparingTo("19.90");
        assertThat(jdbc.queryForObject("SELECT active FROM shop.products WHERE id = 10", Boolean.class)).isTrue();
    }
}
