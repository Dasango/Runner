package com.runner.app.script

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class HttpResponse(
    val statusCode: Int,
    val statusMessage: String,
    val body: String
) {
    val ok: Boolean get() = statusCode in 200..299
}

object NativeHttp {
    fun request(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        connectTimeoutMs: Int = 30_000,
        readTimeoutMs: Int = 30_000
    ): HttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method.uppercase()
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true
            doInput = true
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
            if (body != null) {
                doOutput = true
                if (getRequestProperty("Content-Type") == null) {
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }
        }

        try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }

            val stream = if (connection.responseCode >= 400) {
                connection.errorStream ?: connection.inputStream
            } else {
                connection.inputStream
            }

            val responseBody = stream?.let { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: ""

            return HttpResponse(
                statusCode = connection.responseCode,
                statusMessage = connection.responseMessage ?: "",
                body = responseBody
            )
        } finally {
            connection.disconnect()
        }
    }
}
