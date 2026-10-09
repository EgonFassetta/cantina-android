package dev.pages.mywinecellar.data

private val EMAIL_RE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private val PHONE_RE = Regex("^[0-9+\\-\\s()]{6,20}$")

/** Stesse regole del sito e del server. */
fun isValidEmail(v: String): Boolean = EMAIL_RE.matches(v.trim())

fun isValidPhone(v: String): Boolean {
    val s = v.trim()
    return PHONE_RE.matches(s) && s.count { it.isDigit() } >= 6
}
