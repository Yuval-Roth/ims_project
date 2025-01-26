package com.imsproject.watch.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.net.ssl.*

class RestApiClient {
    private var uri: String? = null
    private val headers: MutableMap<String, String> = HashMap()
    private val params: MutableMap<String, String> = HashMap()
    private var body: String = ""
    private var isPost = false

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
        val requestBuilder = Request.Builder()
            .url(fullUri)
            .header("Content-Type", "application/json")

        headers.forEach { (name, value) -> requestBuilder.addHeader(name, value) }

        if (isPost) {
            requestBuilder.post(body.toRequestBody("application/json".toMediaType()))
        }

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(60, java.util.concurrent.TimeUnit.MINUTES)
            .readTimeout(60, java.util.concurrent.TimeUnit.MINUTES)
            .writeTimeout(60, java.util.concurrent.TimeUnit.MINUTES)
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
        params[key] = value.replace(" ", "%20")
    }

    fun withParams(params: Map<String, String>) = apply {
        params.forEach { (key, value) -> this.withParam(key, value) }
    }
}
