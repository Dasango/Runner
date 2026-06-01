package com.runner.app.script

/**
 * Transpila TypeScript básico a JavaScript eliminando anotaciones de tipo comunes.
 * Scripts complejos con generics avanzados deben subirse como .js precompilado.
 */
object TypeScriptTranspiler {
    fun transpile(source: String): String {
        var code = source

        // Remover imports de tipos: import type { X } from '...'
        code = code.replace(Regex("""import\s+type\s+[^;]+;"""), "")
        // Remover interfaces, types y enums
        code = code.replace(Regex("""(?m)^\s*(export\s+)?(interface|type)\s+\w+[\s\S]*?\n\}"""), "")
        code = code.replace(Regex("""(?m)^\s*(export\s+)?enum\s+\w+\s*\{[\s\S]*?\n\}"""), "")
        // Remover anotaciones de parámetros y retorno en funciones
        code = code.replace(Regex("""(\w+)\s*:\s*[A-Za-z_][\w<>\[\]|&,\s.?]*(?=\s*[,)=])"""), "$1")
        // Remover aserciones de tipo
        code = code.replace(Regex("""\s+as\s+[A-Za-z_][\w<>\[\]|&,\s.?]*"""), "")
        // Remover modificadores de acceso TS en clases
        code = code.replace(Regex("""(?m)^\s*(public|private|protected|readonly)\s+"""), "")
        // Remover genéricos en declaraciones
        code = code.replace(Regex("""<\s*[A-Za-z_][\w<>\[\]|&,\s.?]*\s*>"""), "")

        return code.trim()
    }
}
