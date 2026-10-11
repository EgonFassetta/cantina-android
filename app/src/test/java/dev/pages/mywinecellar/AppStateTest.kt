package dev.pages.mywinecellar

import dev.pages.mywinecellar.data.parseWines
import dev.pages.mywinecellar.ui.AppState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStateTest {
    private val wines = parseWines(
        """
        [
          {"id":"a","cantina":"Zeta","nome":"Uno","annata":"2010","quantita":3,"disponibile_vendita":true,"valore mercato":"25.50","colore_vino":"Rosso"},
          {"id":"b","cantina":"Alfa","nome":"Due","annata":"2020","quantita":2,"disponibile_vendita":true,"colore_vino":"Bianco"},
          {"id":"c","cantina":"Beta","nome":"Tre","annata":"2015","quantita":0,"disponibile_vendita":true,"colore_vino":"Rosso"}
        ]
        """.trimIndent(),
    )

    @Test
    fun totaleSoloSeCiSonoPrezzi() {
        val conPrezzo = AppState(wines = wines, cart = mapOf("a" to 2, "b" to 1))
        assertTrue(conPrezzo.cartHasPrices)
        assertEquals(51.0, conPrezzo.cartTotal, 0.001)
        assertTrue(conPrezzo.cartTotalText.startsWith("Totale stimato"))

        val senzaPrezzo = AppState(wines = wines, cart = mapOf("b" to 2))
        assertFalse(senzaPrezzo.cartHasPrices)
        assertEquals("Prezzo da concordare con il gestore", senzaPrezzo.cartTotalText)
    }

    @Test
    fun contaLeBottigliePresenti() {
        val st = AppState(wines = wines, cart = mapOf("a" to 2, "b" to 1, "zzz" to 5))
        assertEquals(3, st.cartCount)          // "zzz" non esiste nel catalogo e non conta
        assertEquals(2, st.cartLines.size)
    }

    @Test
    fun soloDisponibiliEscludeQuantitaZero() {
        val st = AppState(wines = wines, onlyAvailable = true)
        assertEquals(listOf("b", "a"), st.visible.map { it.id }) // ordine per cantina: Alfa, Zeta
    }

    @Test
    fun filtriAttiviEPreferiti() {
        val st = AppState(wines = wines, colore = "Rosso", favorites = setOf("c"))
        assertEquals(1, st.activeFilters)
        assertEquals(listOf("c", "a"), st.visible.map { it.id }) // Beta, Zeta
        assertEquals(listOf("c"), st.favoriteWines.map { it.id })
        assertTrue(st.isGuest)
        assertFalse(st.copy(email = "a@b.it").isGuest)
    }
}
