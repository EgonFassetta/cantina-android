package dev.pages.mywinecellar.data

import android.content.Context

/** Un'unica istanza della comunicazione col server per tutta l'app (così la sessione è sempre la stessa). */
object Backend {
    @Volatile
    private var instance: SupabaseApi? = null

    fun api(context: Context): SupabaseApi =
        instance ?: synchronized(this) {
            instance ?: SupabaseApi(SharedPrefsSessionStore(context.applicationContext)).also { instance = it }
        }
}
