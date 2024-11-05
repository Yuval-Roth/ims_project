package com.imsproject.utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("UnusedReturnValue")
public class APIFetcher {

    private String uri;
    private final Map<String, String> headers;
    private final Map<String, String> params;
    private String body;
    private boolean isPost;

    private APIFetcher() {
        headers = new HashMap<>();
        params = new HashMap<>();
        body = "";
    }

    public String fetch() throws IOException, InterruptedException {

        assert uri != null : "URI is required";

        // build full URI
        String fullUri = params.entrySet().stream()
                .reduce(uri,
                        (acc, entry) -> "%s%s%s=%s".formatted(
                                acc,
                                (acc.contains("?") ? "&" : "?"),
                                entry.getKey(),
                                entry.getValue()),
                        (acc, ignored) -> acc);

        // build request
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(fullUri));
        if (isPost) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.GET();
        }
        headers.forEach(builder::header);
        HttpRequest request = builder.build();

        // send request
        HttpResponse<String> response;
        try (HttpClient client = createHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        return response.body();
    }

    /**
     * Set the body of the request. Must be used with {@link #withPost()} to have
     * any effect
     */
    public APIFetcher withBody(String body) {
        this.body = body;
        return this;
    }

    /**
     * Set the request method to POST. If {@link #withBody(String)} is not called,
     * the body will be empty
     */
    public APIFetcher withPost() {
        isPost = true;
        return this;
    }

    public APIFetcher withUri(String uri) {
        assert !uri.isBlank() : "URI should not be blank";
        assert !uri.contains("?") : "URI should not contain query parameters";
        assert !uri.contains("&") : "URI should not contain query parameters";
        assert !uri.contains("=") : "URI should not contain query parameters";
        assert !uri.contains(" ") : "URI should not contain spaces";

        this.uri = uri;
        return this;
    }

    public APIFetcher withHeader(String key, String value) {
        assert !headers.containsKey(key) : "Header %s set more than once".formatted(key);

        headers.put(key, value);
        return this;
    }

    public APIFetcher withParam(String key, String value) {
        assert !params.containsKey(key) : "Param %s set more than once".formatted(key);

        value = value.replaceAll(" ", "%20");
        params.put(key, value);
        return this;
    }

    public APIFetcher withParams(Map<String, String> params) {
        params.forEach(this::withParam);
        return this;
    }

    @SafeVarargs
    public final APIFetcher withParams(Pair<String, String>... params) {
        Arrays.stream(params).forEach(p -> this.withParam(p.first(), p.second()));
        return this;
    }

    public static APIFetcher create() {
        return new APIFetcher();
    }

    private HttpClient createHttpClient(){
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustAllCerts = new TrustManager[]{new TrustAllCertificates()};
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private static class TrustAllCertificates extends X509ExtendedTrustManager {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {

        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

        }
    }
}
