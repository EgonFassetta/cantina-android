package dev.pages.mywinecellar.data

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AuthUser(
    val id: String,
    val email: String? = null,
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,
)

@Serializable
data class AuthSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    val user: AuthUser? = null,
) {
    /** Email dell'account, oppure null se l'utente è ancora ospite. */
    val email: String? get() = user?.email?.trim()?.takeIf { it.isNotEmpty() }
}

/** Dove viene ricordata la sessione tra una apertura e l'altra dell'app. */
interface SessionStore {
    fun load(): AuthSession?
    fun save(session: AuthSession?)
}

class SharedPrefsSessionStore(context: Context) : SessionStore {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override fun load(): AuthSession? =
        prefs.getString("s", null)?.let { raw ->
            runCatching { json.decodeFromString<AuthSession>(raw) }.getOrNull()
        }

    override fun save(session: AuthSession?) {
        prefs.edit().apply {
            if (session == null) remove("s") else putString("s", json.encodeToString(session))
        }.apply()
    }
}

class MemorySessionStore(private var value: AuthSession? = null) : SessionStore {
    override fun load(): AuthSession? = value
    override fun save(session: AuthSession?) {
        value = session
    }
}
