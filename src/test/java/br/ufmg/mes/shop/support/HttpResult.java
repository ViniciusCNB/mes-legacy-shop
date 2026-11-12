package br.ufmg.mes.shop.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;

/**
 * Status, headers and body of a real HTTP exchange performed with Apache HttpClient.
 */
public final class HttpResult {

    private static final RequestConfig TIMEOUTS = RequestConfig.custom()
            .setConnectTimeout(20000).setSocketTimeout(20000).setConnectionRequestTimeout(20000).build();

    private final int statusCode;
    private final Header[] headers;
    private final String body;

    private HttpResult(int statusCode, Header[] headers, String body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }

    public static HttpResult execute(CloseableHttpClient client, String method, String url, String payload,
                                     HttpHeaders requestHeaders) throws IOException {
        RequestBuilder builder = RequestBuilder.create(method).setUri(url).setConfig(TIMEOUTS);
        if (payload != null) {
            builder.setEntity(new StringEntity(payload, StandardCharsets.UTF_8));
        }
        for (String name : requestHeaders.keySet()) {
            for (String value : requestHeaders.get(name)) {
                builder.addHeader(name, value);
            }
        }
        try (CloseableHttpResponse response = client.execute(builder.build())) {
            HttpEntity entity = response.getEntity();
            String body = entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
            return new HttpResult(response.getStatusLine().getStatusCode(), response.getAllHeaders(), body);
        }
    }

    public int statusCode() {
        return statusCode;
    }

    public String body() {
        return body;
    }

    public List<String> headers(String name) {
        List<String> values = new ArrayList<>();
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                values.add(header.getValue());
            }
        }
        return values;
    }

    public String header(String name) {
        List<String> values = headers(name);
        return values.isEmpty() ? null : values.get(0);
    }
}
