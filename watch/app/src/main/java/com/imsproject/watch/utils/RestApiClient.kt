package com.imsproject.watch.utils

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class RestApiClient {
    private var uri: String? = null
    private val headers: MutableMap<String, String> = HashMap()
    private val params: MutableMap<String, String> = HashMap()
    private var body: String = ""
    private var isPost = false
    private var onProgress: ((bytesWritten: Long, totalBytes: Long, uploadRateBps: Double) -> Unit)? = null

    companion object {
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .writeTimeout(0L, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

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

        if (!headers.containsKey("Content-Type")) {
            headers["Content-Type"] = "application/json"
        }
        headers.forEach { (name, value) -> requestBuilder.addHeader(name, value) }

        if (isPost) {
            val baseBody = body.toRequestBody("application/json".toMediaType())
            val requestBody = if (onProgress != null) {
                ProgressRequestBody(baseBody, onProgress!!)
            } else baseBody
            requestBuilder.post(requestBody)
        }

        return defaultClient.newCall(requestBuilder.build()).execute().use { response ->
            response.body?.string() ?: throw IOException("Empty response")
        }
    }

    // ------------------ Builder-style modifiers ------------------

    fun withBody(body: String) = apply { this.body = body }

    fun withPost() = apply { isPost = true }

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

    fun withProgress(onProgress: (bytesWritten: Long, totalBytes: Long, uploadRateBps: Double) -> Unit) = apply {
        this.onProgress = onProgress
    }

    // ------------------ Internal helper ------------------

    private class ProgressRequestBody(
        private val delegate: RequestBody,
        private val onProgress: (bytesWritten: Long, totalBytes: Long, uploadRateBps: Double) -> Unit
    ) : RequestBody() {

        override fun contentType() = delegate.contentType()

        override fun contentLength() = delegate.contentLength()

        override fun writeTo(sink: BufferedSink) {
            val totalBytes = contentLength()
            var bytesWritten = 0L
            var lastUpdateTime = System.nanoTime()
            val startTime = lastUpdateTime

            val countingSink = object : ForwardingSink(sink) {
                override fun write(source: Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    bytesWritten += byteCount

                    val now = System.nanoTime()
                    // Limit callback rate to ~5 updates per second
                    if (now - lastUpdateTime > 200_000_000L) {
                        val elapsedSec = (now - startTime) / 1e9
                        val rateBps = if (elapsedSec > 0) bytesWritten / elapsedSec else 0.0
                        onProgress(bytesWritten, totalBytes, rateBps)
                        lastUpdateTime = now
                    }
                }
            }

            val bufferedCountingSink = countingSink.buffer()
            delegate.writeTo(bufferedCountingSink)
            bufferedCountingSink.flush()

            // final update
            val elapsedSec = (System.nanoTime() - startTime) / 1e9
            val rateBps = if (elapsedSec > 0) bytesWritten / elapsedSec else 0.0
            onProgress(bytesWritten, totalBytes, rateBps)
        }
    }
}
