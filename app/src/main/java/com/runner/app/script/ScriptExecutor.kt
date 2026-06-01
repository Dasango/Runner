package com.runner.app.script

import android.util.Log
import com.whl.quickjs.wrapper.QuickJSContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScriptExecutor {

    suspend fun execute(source: String, fileName: String = "script.js"): ExecutionResult = withContext(Dispatchers.IO) {
        val context = QuickJSContext.create()
        val logs = mutableListOf<String>()

        try {
            context.setConsole(object : QuickJSContext.Console {
                override fun log(message: String) {
                    logs.add(message)
                }

                override fun info(message: String) {
                    logs.add("[info] $message")
                }

                override fun warn(message: String) {
                    logs.add("[warn] $message")
                }

                override fun error(message: String) {
                    logs.add("[error] $message")
                }
            })

            JsRuntimeBindings.install(context)
            val wrapped = JsRuntimeBindings.wrapSource(source)
            context.evaluate(wrapped, fileName)
            val output = logs.joinToString("\n").ifBlank { "Script ejecutado correctamente" }
            ExecutionResult(success = true, message = output)
        } catch (e: Exception) {
            Log.e(TAG, "Script error", e)
            val errorMsg = buildString {
                append(e.message ?: "Error desconocido")
                if (logs.isNotEmpty()) {
                    append("\n--- console ---\n")
                    append(logs.joinToString("\n"))
                }
            }
            ExecutionResult(success = false, message = errorMsg)
        } finally {
            context.destroy()
        }
    }

    companion object {
        private const val TAG = "ScriptExecutor"
    }
}

data class ExecutionResult(val success: Boolean, val message: String)
