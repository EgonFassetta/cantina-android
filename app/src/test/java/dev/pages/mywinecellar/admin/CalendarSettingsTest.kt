package dev.pages.mywinecellar.admin

import dev.pages.mywinecellar.data.parseWines
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Punti 4, 10 e 11: calendario delle bevute, impostazioni e amministratori (stesse regole del sito). */
class CalendarSettingsTest {
    private val base = parseWines("""[{"id":"a","nome":"A","quantita":1},{"id":"b","nome":"B","quantita":1},{"id":"c","nome":"C","quantita":1}]""")
    private val wines = listOf(
        base[0].copy(bevuto = true, dataApertura = "2026-11-03", dataFine = "2026-11-05"),
        base[1].copy(bevuto = true, dataApertura = "2026-10-30", dataFine = "2026-11-01"),
        base[2].copy(bevuto = true, dataApertura = "2026-10-01", dataFine = "2026-10-02"),
    )

    @Test
    fun calendario() {
        // novembre 2026 inizia di domenica: 6 caselle vuote (lun-sab) prima del giorno 1
        val cells = monthCells(2026, 10)
        assertEquals(6, cells.takeWhile { it == null }.size)
        assertEquals(30, cells.filterNotNull().size)
        assertEquals("Novembre 2026", monthLabel(2026, 10))
        // finite in novembre, in ordine di apertura
        assertEquals(listOf("b", "a"), drunkInMonth(wines, 2026, 10).map { it.id })
        assertEquals(listOf("c"), drunkInMonth(wines, 2026, 9).map { it.id })
        val byDate = drunkByDate(wines)
        assertEquals(listOf("a"), byDate["2026-11-03"]!!.first.map { it.id })
        assertEquals(listOf("b"), byDate["2026-11-01"]!!.second.map { it.id })
    }

    @Test
    fun chiaviEIndirizzi() {
        assertEquals("site_title", settingKey("site_title", "egon"))
        assertEquals("marco/site_title", settingKey("site_title", "marco"))
        assertEquals("https://mywinecellar.pages.dev/?c=marco", cellarUrl("marco"))
        assertTrue(isValidSlug("marco"))
        assertTrue(isValidSlug("la-cantina-2"))
        assertFalse(isValidSlug("ab"))
        assertFalse(isValidSlug("-marco"))
        assertFalse(isValidSlug("Marco"))
    }

    @Test
    fun controlliDelleImpostazioni() {
        assertNull(settingsError("Titolo", "a@b.it", "", superAdmin = true, limit = "5"))
        assertEquals("Inserisci un titolo per il sito.", settingsError(" ", "", "", false, "5"))
        assertEquals("Inserisci un indirizzo email valido.", settingsError("T", "abc", "", false, "5"))
        assertEquals("L'email per le richieste non può essere vuota.", settingsError("T", "", "x@y.it", false, "5"))
        assertEquals("Il limite di richieste deve essere un numero da 1 a 200.", settingsError("T", "", "", true, "0"))
        assertNull(settingsError("T", "", "", false, "0"))          // il limite lo vede solo il Super Admin
        assertEquals("La password deve avere almeno 12 caratteri.", newAdminError("a@b.it", "corta", ""))
        assertNull(newAdminError("a@b.it", "abbastanza-lunga", ""))
        assertTrue(newAdminError("a@b.it", "abbastanza-lunga", "X")!!.startsWith("Indirizzo non valido"))
    }

    @Test
    fun elencoAmministratoriECantine() {
        val admins = parseAdmins(Json.parseToJsonElement("""{"ok":true,"admins":[{"email":"a@b.it","role":"super_admin"},{"email":"","role":"admin"}]}""").jsonObject)
        assertEquals(1, admins.size)
        val (cellars, main) = parseCellars(Json.parseToJsonElement(
            """{"ok":true,"principale":"egon","cantine":[{"email":"M@B.IT","nome":"Marco","slug":"marco","vini":"12","app_abilitata":true}]}""",
        ).jsonObject)
        assertEquals("egon", main)
        assertEquals(12, cellars["m@b.it"]!!.vini)
        assertTrue(cellars["m@b.it"]!!.appAbilitata)
    }
}
