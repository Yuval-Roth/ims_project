package com.imsproject.common.networking

import java.io.IOException
import java.net.Socket
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManager
import javax.net.ssl.X509ExtendedTrustManager

class RestApiClient {
    private var uri: String? = null
    private val headers: MutableMap<String, String> = HashMap()
    private val params: MutableMap<String, String> = HashMap()
    private var body: String = ""
    private var isPost = false
    private var trustAllCertificates = false

    @Throws(IOException::class, InterruptedException::class, IllegalStateException::class)
    fun send(): String {

        val uri = this.uri ?: throw IllegalStateException("URI is required")

        // build full URI
        val fullUri = params.entries.fold(uri) { acc, entry ->
            val separator = if (acc.contains("?")) "&" else "?"
            val encodedKey = URLEncoder.encode(entry.key, StandardCharsets.UTF_8.toString())
            val encodedValue = URLEncoder.encode(entry.value, StandardCharsets.UTF_8.toString())
            "$acc$separator$encodedKey=$encodedValue"
        }

        // build request
        val request = HttpRequest.newBuilder()
            .uri(URI.create(fullUri))
            .header("Content-Type", "application/json")
            .apply{
                if (isPost) POST(HttpRequest.BodyPublishers.ofString(body))
                headers.forEach { (name, value) -> header(name, value) }
            }.build()

        // send request
        val client = if (trustAllCertificates) createHttpClientTrustAllCertificates() else HttpClient.newHttpClient()!!
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    /**
     * Set the body of the request. Must be used with [.withPost] to have
     * any effect
     */
    fun withBody(body: String) = apply {
        this.body = body
    }

    /**
     * Set the request method to POST. If [.withBody] is not called,
     * the body will be empty
     */
    fun withPost() = apply {
        isPost = true
    }

    fun withUri(uri: String) = apply {
        assert(uri.isNotBlank()) { "URI should not be blank" }
        assert(!uri.contains("?")) { "URI should not contain query parameters" }
        assert(!uri.contains("&")) { "URI should not contain query parameters" }
        assert(!uri.contains("=")) { "URI should not contain query parameters" }
        assert(!uri.contains(" ")) { "URI should not contain spaces" }

        this.uri = uri
    }

    fun withHeader(key: String, value: String) = apply {
        assert(!headers.containsKey(key)) { "Header %s set more than once".format(key) }
        headers[key] = value
    }

    fun withParam(key: String, value: String) = apply {
        assert(!params.containsKey(key)) { "Param %s set more than once".format(key) }
        params[key] = value.replace(" ".toRegex(), "%20") // replace spaces with %20
    }

    fun withParams(params: Map<String, String>) = apply {
        params.forEach { (key: String, value: String) -> this.withParam(key, value) }
    }

    @SafeVarargs
    fun withParams(vararg params: Pair<String, String>) = apply {
        Arrays.stream(params).forEach { p: Pair<String, String> -> this.withParam(p.first, p.second) }
    }

    fun trustAllCertificates() = apply {
        trustAllCertificates = true
    }

    private fun createHttpClientTrustAllCertificates(): HttpClient {
        try {
            val sslContext = SSLContext.getInstance("TLS")
            val trustAllCerts = arrayOf<TrustManager>(TrustAllCertificates())
            sslContext.init(null, trustAllCerts, SecureRandom())
            return HttpClient.newBuilder()
                .sslContext(sslContext)
                .build()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: KeyManagementException) {
            throw RuntimeException(e)
        }
    }

    private class TrustAllCertificates : X509ExtendedTrustManager() {
        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }

        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) {
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) {
        }

        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine) {
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine) {
        }
    }
}
