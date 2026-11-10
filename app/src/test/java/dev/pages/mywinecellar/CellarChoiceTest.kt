package dev.pages.mywinecellar

import dev.pages.mywinecellar.data.Cellar
import dev.pages.mywinecellar.data.CellarInfo
import dev.pages.mywinecellar.data.NOTE_CELLAR_GONE
import dev.pages.mywinecellar.data.StartDecision
import dev.pages.mywinecellar.data.decideStart
import dev.pages.mywinecellar.data.visibleCellars
import dev.pages.mywinecellar.ui.AppState
import dev.pages.mywinecellar.ui.etichetteText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Scelta della cantina all'apertura: stesse regole del sito. */
class CellarChoiceTest {
    private val list = listOf(CellarInfo("egon", "La cantina di Egon", 190), CellarInfo("marco", "Cantina di Marco", 5))

    @Test
    fun soloCantinePubblicheEAbilitate() {
        val app = listOf(Cellar("egon", "La cantina di Egon"), Cellar("privata", "Privata"), Cellar("marco", "Cantina di Marco"))
        val pub = listOf(Cellar("egon", "La cantina di Egon"), Cellar("marco", "Cantina di Marco"), Cellar("solosito", "Solo sito"))
        val r = visibleCellars(app, pub, mapOf("egon" to 190, "marco" to 1))
        assertEquals(listOf("marco", "egon"), r.map { it.slug })      // ordine per nome: «Cantina…» prima di «La…»
        assertEquals(1, r.first { it.slug == "marco" }.etichette)
        assertFalse(r.any { it.slug == "privata" || it.slug == "solosito" })
    }

    @Test
    fun seLaVistaPubblicaNonSiLeggeSiUsanoQuelleDellApp() {
        val r = visibleCellars(listOf(Cellar("egon", "")), null, emptyMap())
        assertEquals(listOf(CellarInfo("egon", "egon", 0)), r)
    }

    @Test
    fun primaAperturaMostraLaScelta() {
        assertEquals(StartDecision.Choose(null), decideStart(null, null, list))
        assertEquals(StartDecision.Choose(null), decideStart(null, null, null))   // senza rete: la scelta mostra «Riprova»
    }

    @Test
    fun riapreLUltimaCantinaScelta() {
        assertEquals(StartDecision.Open("marco", false), decideStart("marco", null, list))
        assertEquals(StartDecision.Open("marco", false), decideStart("marco", null, null))  // senza rete: catalogo salvato
    }

    @Test
    fun cantinaNonPiuDisponibileTornaAllaSceltaConAvviso() {
        assertEquals(StartDecision.Choose(NOTE_CELLAR_GONE), decideStart("chiusa", null, list))
        assertEquals(StartDecision.Choose(NOTE_CELLAR_GONE), decideStart("egon", null, emptyList()))
    }

    @Test
    fun lAdminEntraNellaPropriaCantinaAncheSePrivata() {
        assertEquals(StartDecision.Open("privata", true), decideStart(null, Cellar("privata", ""), list))
        assertEquals(StartDecision.Open("privata", true), decideStart("egon", Cellar("privata", ""), list))
    }

    @Test
    fun nomeCantinaECambioSoloPerIClienti() {
        val st = AppState(cellars = list, cellar = "marco")
        assertEquals("Cantina di Marco", st.cellarName)
        assertFalse(st.isAdminMode)
        val admin = st.copy(cellar = "privata", adminCellar = Cellar("privata", "La mia privata"))
        assertTrue(admin.isAdminMode)
        assertEquals("La mia privata", admin.cellarName)
        assertFalse(admin.copy(choosing = true).canCloseChooser)
        assertFalse(st.copy(choosing = true).canCloseChooser)        // nessun vino caricato: non si può tornare indietro
    }

    @Test
    fun contatoreEtichette() {
        assertEquals("1 etichetta", etichetteText(1))
        assertEquals("190 etichette", etichetteText(190))
        assertEquals("0 etichette", etichetteText(0))
    }
}
