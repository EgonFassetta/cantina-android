package dev.pages.mywinecellar.data

import java.io.File

/** Ultimo catalogo scaricato, per poter aprire l'app anche senza connessione. */
class WineCache(private val file: File) {
    fun save(json: String) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json)
        }
    }

    fun load(): List<Wine>? = runCatching {
        if (!file.exists()) null else parseWines(file.readText()).takeIf { it.isNotEmpty() }
    }.getOrNull()
}
