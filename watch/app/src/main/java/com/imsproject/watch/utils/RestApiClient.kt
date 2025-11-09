package com.imsproject.watch.utils

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class RestApiClient {
    private var uri: String? = null
    private val headers: MutableMap<String, String> = HashMap()
    private val params: MutableMap<String, String> = HashMap()
    private var body: String = ""
    private var isPost = false
    private var timeoutMs = 0L

    @Throws(IOException::class, IllegalStateException::class)
    fun send(): String {
        val uri = this.uri ?: throw IllegalStateException("URI is required")

        // Build full URI with query parameters
        val fullUri = params.entries.fold(uri) { acc, entry ->
            val separator = if (acc.contains("?")) "&" else "?"
            val encodedKey = URLEncoder.encode(entry.key, StandardCharsets.UTF_8.toString())
            val encodedValue = URLEncoder.encode(entry.value, StandardCharsets.UTF_8.toString())
            "$acc$separator$encodedKey=$encodedValue"
        }

        // Build OkHttp request
        val requestBuilder = Request.Builder().url(fullUri)
        if(! headers.contains("Content-Type")){
            headers.put("Content-Type", "application/json")
        }
        headers.forEach { (name, value) -> requestBuilder.addHeader(name, value) }

        if (isPost) {
            requestBuilder.post(body.toRequestBody("application/json".toMediaType()))
        }

        val client = OkHttpClient().newBuilder()
            .connectTimeout(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .callTimeout(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        val response = client.newCall(requestBuilder.build()).execute()
        return response.body?.string() ?: throw IOException("Empty response")
    }

    fun withBody(body: String) = apply {
        this.body = body
    }

    fun withPost() = apply {
        isPost = true
    }

    fun withUri(uri: String) = apply {
        require(uri.isNotBlank()) { "URI should not be blank" }
        require(!uri.contains("?") && !uri.contains("&") && !uri.contains("=") && !uri.contains(" ")) {
            "URI should not contain query parameters or spaces"
        }
        this.uri = uri
    }

    fun withHeader(key: String, value: String) = apply {
        require(!headers.containsKey(key)) { "Header $key set more than once" }
        headers[key] = value
    }

    fun withParam(key: String, value: String) = apply {
        require(!params.containsKey(key)) { "Param $key set more than once" }
        params[key] = value
    }

    fun withParams(params: Map<String, String>) = apply {
        params.forEach { (key, value) -> this.withParam(key, value) }
    }

    fun withTimeout(timeoutMs: Long) = apply {
        require(timeoutMs >= 0) { "Timeout must be non-negative" }
        this.timeoutMs = timeoutMs
    }
}
