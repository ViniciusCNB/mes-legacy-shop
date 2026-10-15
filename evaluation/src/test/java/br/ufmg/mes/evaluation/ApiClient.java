package br.ufmg.mes.evaluation;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

final class ApiClient {
    static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private final String baseUrl;
    private final String email;
    private final String password;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).cookieHandler(cookies).build();

    ApiClient(String email) { this(email, "test-password"); }

    ApiClient(String email, String password) {
        this.email = email;
        this.password = password;
        baseUrl = System.getProperty("evaluation.baseUrl", "").replaceAll("/+$", "");
        URI uri = URI.create(baseUrl);
        if (!"http".equals(uri.getScheme()) || !("127.0.0.1".equals(uri.getHost())
                || "localhost".equals(uri.getHost())) || uri.getPort() < 1) {
            throw new IllegalArgumentException("Informe -Devaluation.baseUrl=http://127.0.0.1:PORTA "
                    + "para a aplicacao com fixtures isoladas de evaluation/");
        }
    }

    Result get(String path) throws Exception { return call("GET", path, null, null, true); }

    Result getWithoutAuthentication(String path) throws Exception {
        return call("GET", path, null, null, false);
    }

    Result write(String method, String path, String payload) throws Exception {
        return writePrepared(method, path, payload, csrfToken());
    }

    Result writePrepared(String method, String path, String payload, String token) throws Exception {
        return call(method, path, payload, token, true);
    }

    String csrfToken() throws Exception {
        Result csrf = get("/api/csrf").expect(200);
        assertTrue(csrf.response.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        String token = csrf.body.get("token").asText();
        assertEquals("X-XSRF-TOKEN", csrf.body.get("headerName").asText());
        assertTrue(csrf.response.headers().allValues("Set-Cookie").stream()
                .anyMatch(cookie -> cookie.startsWith("XSRF-TOKEN=" + token + ";")
                        && cookie.contains("HttpOnly") && cookie.contains("Path=/")));
        return token;
    }

    Result writeWithoutCsrf(String path, String payload) throws Exception {
        return call("POST", path, payload, null, true);
    }

    private Result call(String method, String path, String payload, String token, boolean authenticate)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .method(method, payload == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
        if (email != null && authenticate) {
            request.header("Authorization", "Basic " + Base64.getEncoder()
                    .encodeToString((email + ":" + password).getBytes(StandardCharsets.UTF_8)));
        }
        if (token != null) { request.header("X-XSRF-TOKEN", token); }
        HttpResponse<String> response = http.send(request.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertTrue(cookies.getCookieStore().getCookies().stream()
                .noneMatch(cookie -> cookie.getName().equals("JSESSIONID")), "Cookie CSRF nao autentica");
        return new Result(response, JSON.readTree(response.body()));
    }

    static final class Result {
        final HttpResponse<String> response;
        final JsonNode body;

        Result(HttpResponse<String> response, JsonNode body) {
            this.response = response;
            this.body = body;
        }

        Result expect(int status) {
            assertEquals(status, response.statusCode(),
                    () -> response.request().method() + " " + response.uri() + ": " + response.body());
            return this;
        }

        Result error(int status, String code) {
            expect(status);
            assertEquals(code, body.path("code").asText());
            assertFalse(body.path("message").asText().isBlank());
            for (String forbidden : new String[] {"password_hash", "$2a$", "stackTrace", "org.springframework"}) {
                assertFalse(response.body().contains(forbidden), "Detalhes internos na resposta");
            }
            return this;
        }
    }
}
