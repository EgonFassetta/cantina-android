package dev.pages.mywinecellar.data

import android.content.Context

/**
 * Stato dell'accesso Admin ricordato sul telefono: ruolo e cantina dell'Admin.
 * Lo usano sia l'area Admin (che lo scrive) sia il catalogo (che apre la cantina dell'Admin).
 */
object AdminPrefs {
    private fun prefs(context: Context) = context.getSharedPreferences("admin", Context.MODE_PRIVATE)

    fun role(context: Context): String? = prefs(context).getString("role", null)

    fun cellar(context: Context): Cellar? {
        val p = prefs(context)
        val slug = p.getString("cellar_slug", null)?.takeIf { it.isNotBlank() } ?: return null
        return Cellar(slug, p.getString("cellar_nome", null).orEmpty())
    }

    fun save(context: Context, role: String, cellar: Cellar) {
        prefs(context).edit()
            .putString("role", role)
            .putString("cellar_slug", cellar.slug)
            .putString("cellar_nome", cellar.nome)
            .apply()
    }

    fun saveCellar(context: Context, cellar: Cellar) {
        prefs(context).edit().putString("cellar_slug", cellar.slug).putString("cellar_nome", cellar.nome).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove("role").remove("cellar_slug").remove("cellar_nome").apply()
    }
}
