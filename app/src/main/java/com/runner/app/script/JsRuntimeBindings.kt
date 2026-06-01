package com.runner.app.script

import com.whl.quickjs.wrapper.JSArray
import com.whl.quickjs.wrapper.JSCallFunction
import com.whl.quickjs.wrapper.JSObject
import com.whl.quickjs.wrapper.QuickJSContext

object JsRuntimeBindings {

    private val PRELUDE = """
        globalThis.fetch = function(url, options) {
            options = options || {};
            return __runnerHttpRequest(String(url), options);
        };
    """.trimIndent()

    fun install(context: QuickJSContext) {
        context.getGlobalObject().setProperty("__runnerHttpRequest", JSCallFunction { args ->
            val url = args.getOrNull(0)?.toString()
                ?: throw IllegalArgumentException("fetch: URL requerida")
            val options = args.getOrNull(1)
            val method = readOption(context, options, "method") ?: "GET"
            val headers = readHeaders(context, options)
            val body = readOption(context, options, "body")
            val response = NativeHttp.request(url, method, headers, body)
            createResponseObject(context, response)
        })
    }

    /**
     * QuickJS no tiene event loop como Node.js. Si el script usa async/await,
     * lo convertimos a ejecución síncrona y fetch responde al instante.
     */
    fun wrapSource(source: String): String {
        val body = if (source.contains("async ") || source.contains("await ")) {
            stripAsyncAwait(source)
        } else {
            source
        }
        return "$PRELUDE\n$body"
    }

    private fun stripAsyncAwait(source: String): String {
        return source
            .replace(Regex("""\basync\s+function\b"""), "function")
            .replace(Regex("""\basync\s*\("""), "(")
            .replace(Regex("""\bawait\s+"""), "")
    }

    private fun readOption(context: QuickJSContext, options: Any?, key: String): String? {
        if (options !is JSObject) return null
        return context.getProperty(options, key)?.toString()
    }

    private fun readHeaders(context: QuickJSContext, options: Any?): Map<String, String> {
        if (options !is JSObject) return emptyMap()
        val headersObj = context.getProperty(options, "headers") ?: return emptyMap()
        if (headersObj !is JSObject) return emptyMap()

        val names = context.getOwnPropertyNames(headersObj) as? JSArray ?: return emptyMap()
        val headers = mutableMapOf<String, String>()
        for (i in 0 until context.length(names)) {
            val key = context.get(names, i)?.toString() ?: continue
            val value = context.getProperty(headersObj, key)?.toString() ?: continue
            headers[key] = value
        }
        return headers
    }

    private fun createResponseObject(context: QuickJSContext, response: HttpResponse): JSObject {
        val body = response.body
        val jsResponse = context.createNewJSObject()
        jsResponse.setProperty("ok", response.ok)
        jsResponse.setProperty("status", response.statusCode)
        jsResponse.setProperty("statusText", response.statusMessage)

        jsResponse.setProperty("text", JSCallFunction { body })

        jsResponse.setProperty("json", JSCallFunction {
            if (body.isBlank()) {
                context.createNewJSObject()
            } else {
                context.parse(body)
            }
        })

        return jsResponse
    }
}
