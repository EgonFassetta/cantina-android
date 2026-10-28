package dev.pages.mywinecellar.data

/**
 * Chiave della posizione, come sul sito: "3.4" = scaffale 3, colonna 4; "box2" = box 2.
 * Restituisce null se la posizione non è valida (il vino non compare nella mappa).
 */
fun shelfKey(posizione: String): String? {
    val s = posizione.trim()
    Regex("(?i)^box\\s*([1-6])$").find(s)?.let { return "box" + it.groupValues[1] }
    Regex("^([1-9])\\.?([1-9])$").find(s)?.let { return it.groupValues[1] + "." + it.groupValues[2] }
    return null
}

/** Nome leggibile di una chiave: "Scaffale 3.4" oppure "Box 2". */
fun shelfLabel(key: String): String =
    if (key.startsWith("box")) "Box ${key.removePrefix("box")}" else "Scaffale $key"
