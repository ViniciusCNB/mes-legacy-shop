package br.ufmg.mes.shop.product;

import java.math.BigDecimal;
import java.util.stream.Stream;

import javax.validation.ConstraintViolationException;

import br.ufmg.mes.shop.ShopApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import br.ufmg.mes.shop.support.PostgresTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql("/db/catalog-fixture.sql")
class CatalogIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = PostgresTestDatabase.create("catalog_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private ProductRepository products;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void listsOnlyActiveProductsInStableOrderWithDefaultPagination() {
        JsonNode page = get("/api/products");
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.get("page").asInt()).isZero();
        assertThat(page.get("size").asInt()).isEqualTo(20);
        assertThat(page.get("totalElements").asLong()).isEqualTo(3);
        assertThat(page.get("totalPages").asInt()).isEqualTo(1);
        assertThat(page.findValuesAsText("id")).containsExactly("10", "30", "40");
        assertThat(page.findValuesAsText("sku")).doesNotContain("HIDDEN-20");
    }

    @Test
    void paginatesWithoutCountingInactiveProductsAndPreservesTotalsBeyondLastPage() {
        JsonNode first = get("/api/products?page=0&size=2");
        JsonNode second = get("/api/products?page=1&size=2");
        JsonNode beyond = get("/api/products?page=2&size=2");
        assertThat(first.findValuesAsText("id")).containsExactly("10", "30");
        assertThat(second.findValuesAsText("id")).containsExactly("40");
        assertThat(second.at("/content/0/stockQuantity").asInt()).isZero();
        assertThat(second.get("page").asInt()).isEqualTo(1);
        assertThat(second.get("size").asInt()).isEqualTo(2);
        assertThat(beyond.get("content")).isEmpty();
        for (JsonNode page : new JsonNode[] {first, second, beyond}) {
            assertThat(page.get("totalElements").asLong()).isEqualTo(3);
            assertThat(page.get("totalPages").asInt()).isEqualTo(2);
        }
    }

    @Test
    void returnsAnEmptyPageWhenThereAreNoActiveProducts() {
        jdbc.update("UPDATE shop.products SET active = false");
        JsonNode page = get("/api/products");
        assertThat(page.get("content")).isEmpty();
        assertThat(page.get("totalElements").asLong()).isZero();
        assertThat(page.get("totalPages").asInt()).isZero();
    }

    @Test
    void returnsOnlyThePublicProductFields() {
        JsonNode product = get("/api/products/10");
        assertThat(product.size()).isEqualTo(5);
        assertThat(product.get("id").asLong()).isEqualTo(10);
        assertThat(product.get("sku").asText()).isEqualTo("CATALOG-10");
        assertThat(product.get("name").asText()).isEqualTo("Primeiro produto");
        assertThat(product.get("price").decimalValue()).isEqualByComparingTo("19.90");
        assertThat(product.get("stockQuantity").asInt()).isEqualTo(12);
    }

    @ParameterizedTest
    @ValueSource(longs = {20, 999})
    void doesNotDistinguishInactiveFromMissingProducts(long id) {
        ResponseEntity<JsonNode> response = http.getForEntity("/api/products/" + id, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().size()).isEqualTo(2);
        assertThat(response.getBody().get("code").asText()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(response.getBody().get("message").asText()).isEqualTo("Produto não encontrado");
    }

    @ParameterizedTest
    @CsvSource({
            "/api/products?page=-1, page",
            "/api/products?page=1000001, page",
            "/api/products?size=0, size",
            "/api/products?size=101, size",
            "/api/products?page=abc, page",
            "/api/products?size=2147483648, size",
            "/api/products/0, id",
            "/api/products/-1, id",
            "/api/products/abc, id",
            "/api/products/9223372036854775808, id"
    })
    void rejectsInvalidParametersWithTheDocumentedError(String path, String field) {
        ResponseEntity<JsonNode> response = http.getForEntity(path, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode error = response.getBody();
        assertThat(error.size()).isEqualTo(3);
        assertThat(error.get("code").asText()).isEqualTo("INVALID_REQUEST");
        assertThat(error.get("message").asText()).isEqualTo("Parâmetros inválidos");
        assertThat(error.get("fieldErrors").size()).isEqualTo(1);
        assertThat(error.at("/fieldErrors/0/field").asText()).isEqualTo(field);
        assertThat(error.at("/fieldErrors/0/message").asText()).isNotBlank();
    }

    @Test
    void persistsMoneyExactlyAndGeneratesAnId() {
        BigDecimal price = new BigDecimal("12345678901234567.89");
        Product saved = products.saveAndFlush(new Product("PRECISE-PRICE", "Valor exato", price, 2, true));
        Product reloaded = products.findById(saved.getId()).orElseThrow();
        assertThat(saved.getId()).isGreaterThan(0L);
        assertThat(reloaded.getPrice()).isEqualTo(price);
        assertThat(reloaded.getPrice().scale()).isEqualTo(2);
        assertThat(get("/api/products/" + saved.getId()).get("sku").asText()).isEqualTo("PRECISE-PRICE");
    }

    @ParameterizedTest
    @MethodSource("invalidProducts")
    void validatesProductsBeforePersisting(Product product) {
        assertThatThrownBy(() -> products.saveAndFlush(product))
                .isInstanceOf(ConstraintViolationException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM shop.products", Long.class)).isEqualTo(4L);
    }

    static Stream<Product> invalidProducts() {
        return Stream.of(
                new Product("", "Produto", BigDecimal.ONE, 0, true),
                new Product("\t", "Produto", BigDecimal.ONE, 0, true),
                new Product("X".repeat(65), "Produto", BigDecimal.ONE, 0, true),
                new Product("INVALID", " ", BigDecimal.ONE, 0, true),
                new Product("INVALID", "Produto", null, 0, true),
                new Product("INVALID", "Produto", BigDecimal.ZERO, 0, true),
                new Product("INVALID", "Produto", new BigDecimal("1.001"), 0, true),
                new Product("INVALID", "Produto", new BigDecimal("100000000000000000.00"), 0, true),
                new Product("INVALID", "Produto", BigDecimal.ONE, -1, true));
    }

    @ParameterizedTest
    @CsvSource({
            "NEW-SKU, Produto, 0.00, 1, products_price_positive",
            "NEW-SKU, Produto, 1.00, -1, products_stock_nonnegative",
            "CATALOG-10, Produto, 1.00, 1, products_sku_unique",
            "'\t', Produto, 1.00, 1, products_sku_not_blank",
            "NEW-SKU, '   ', 1.00, 1, products_name_not_blank"
    })
    void enforcesDatabaseConstraintsEvenWhenBypassingJpa(String sku, String name, BigDecimal price,
                                                       int stock, String constraint) {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO shop.products (sku, name, price, stock_quantity, active) VALUES (?, ?, ?, ?, ?)",
                sku, name, price, stock, true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(constraint);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM shop.products", Long.class)).isEqualTo(4L);
    }

    @Test
    void refusesToStartWithAColumnMissingFromTheMappedSchema() {
        jdbc.execute("CREATE SCHEMA validation_probe");
        jdbc.execute("CREATE TABLE validation_probe.products (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE validation_probe.app_users (LIKE shop.app_users INCLUDING ALL)");
        try {
            new WebApplicationContextRunner()
                    .withInitializer(new ConfigDataApplicationContextInitializer())
                    .withUserConfiguration(ShopApplication.class)
                    .withPropertyValues(
                            "spring.profiles.active=test",
                            "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                            "spring.datasource.username=" + POSTGRES.getUsername(),
                            "spring.datasource.password=" + POSTGRES.getPassword(),
                            "spring.jpa.properties.hibernate.default_schema=validation_probe")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasRootCauseInstanceOf(SchemaManagementException.class)
                                .hasStackTraceContaining("Schema-validation: missing column");
                    });
        } finally {
            jdbc.execute("DROP SCHEMA validation_probe CASCADE");
        }
    }

    private JsonNode get(String path) {
        ResponseEntity<JsonNode> response = http.getForEntity(path, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }
}
