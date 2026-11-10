package dev.pages.mywinecellar.admin

import dev.pages.mywinecellar.data.parseWines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** «Segna come bevuta» / «Ripristina»: stessi testi e controlli del sito. */
class DrunkTest {
    @Test
    fun avvisiSullaQuantita() {
        assertEquals("Proseguendo, la quantità verrà ridotta di 1 unità (da 3 a 2).", drinkWarningText(3))
        assertEquals("La quantità è già 0 e resterà invariata.", drinkWarningText(0))
        assertEquals("Proseguendo, la quantità verrà aumentata di 1 unità (da 0 a 1).", restoreWarningText(0))
    }

    @Test
    fun date() {
        assertEquals("2026-11-03", parseDateIt("3/11/2026"))
        assertEquals("2026-11-03", parseDateIt(" 03-11-2026 "))
        assertNull(parseDateIt("31/02/2026"))      // giorno inesistente
        assertNull(parseDateIt("2026-11-03"))
        assertNull(parseDateIt(""))
        assertEquals(2, daysBetween("2026-11-01", "2026-11-03"))
        assertEquals(0, daysBetween("2026-11-01", "2026-11-01"))
        assertNull(daysBetween(null, "2026-11-03"))
    }

    @Test
    fun testoDellaBottigliaBevuta() {
        val w = parseWines("""[{"id":"w1","nome":"A","quantita":2}]""").first()
            .copy(bevuto = true, dataApertura = "2026-11-01", dataFine = "2026-11-02")
        assertEquals("Aperta il 01/11/2026, finita il 02/11/2026 — 1 giorno.", drunkText(w))
        assertEquals("Aperta il 01/11/2026, finita il 03/11/2026 — 2 giorni.", drunkText(w.copy(dataFine = "2026-11-03")))
    }
}
