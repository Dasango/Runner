package com.runner.app.data

import android.content.Context
import java.io.File

class ScriptRepository(private val context: Context, private val scriptDao: ScriptDao) {
    private val scriptsDir: File
        get() = File(context.filesDir, "scripts").also { it.mkdirs() }

    fun observeScripts() = scriptDao.observeAll()

    suspend fun getScript(id: Long) = scriptDao.getById(id)

    suspend fun saveScript(name: String, fileName: String, content: String): Long {
        val isTypeScript = fileName.endsWith(".ts", ignoreCase = true)
        val id = scriptDao.insert(
            ScriptEntity(name = name, fileName = fileName, isTypeScript = isTypeScript)
        )
        File(scriptsDir, "$id.js").writeText(content)
        return id
    }

    suspend fun updateScript(id: Long, name: String, content: String) {
        val existing = scriptDao.getById(id) ?: return
        scriptDao.insert(existing.copy(name = name)) // insert handles update if it has same ID and DAO is configured so, or I should check ScriptDao
        File(scriptsDir, "$id.js").writeText(content)
    }

    suspend fun readScriptContent(scriptId: Long): String? {
        val file = File(scriptsDir, "$scriptId.js")
        return if (file.exists()) file.readText() else null
    }

    suspend fun deleteScript(id: Long) {
        File(scriptsDir, "$id.js").delete()
        scriptDao.deleteById(id)
    }
}
