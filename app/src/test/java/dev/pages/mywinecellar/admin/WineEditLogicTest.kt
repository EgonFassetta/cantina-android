package dev.pages.mywinecellar.admin

import dev.pages.mywinecellar.data.parseWines
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Modifica della scheda: stessi campi, controlli e valori del sito. */
class WineEditLogicTest {
    private val wine = parseWines(
        """[{"id":"w1","cantina":"Zenato","nome":"Amarone","annata":2015,"quantita":3,"posizione":3.4,"prezzo":30,
           "formato":"0,750 lt.","grado":15.5,"colore_vino":"Rosso","effervescenza":"Fermo","dolcezza":"Secco",
           "paese":"Italia","regione":"Veneto","giudizio_complessivo":80,"struttura":70,"valore mercato":45.5,
           "disponibile_vendita":true,"escluso_vetrina":false,"nota":"Ottimo"}]""",
    ).first()

    @Test
    fun moduloPrecompilatoDallaScheda() {
        val f = formFrom(wine)
        assertEquals("2015", f.annata)
        assertEquals("3.4", f.posizione)
        assertEquals("15,5", f.grado)
        assertEquals("45,5", f.valoreMercato)
        assertEquals(80, f.giudizio)
        assertEquals(70, f.profilo["struttura"])
        assertEquals(null, f.profilo["morbidezza"])
    }

    @Test
    fun salvaICampiConINomiDelSito() {
        val f = formFrom(wine).copy(quantita = "7", posizione = "box2", prezzo = "12,50", grado = "13,5")
        val p = buildWinePatch(wine, f).getOrThrow()
        assertEquals(JsonPrimitive(7), p.fields["quantita"])
        assertEquals(JsonPrimitive("Box2"), p.fields["posizione"])
        assertEquals(12.5, p.fields["prezzo"]!!.jsonPrimitive.content.toDouble(), 0.001)
        assertEquals(13.5, p.fields["grado"]!!.jsonPrimitive.content.toDouble(), 0.001)
        assertEquals(45.5, p.fields["valore mercato"]!!.jsonPrimitive.content.toDouble(), 0.001)
        assertFalse("i cursori mai indicati non si salvano", p.fields.containsKey("morbidezza"))
        assertEquals(JsonPrimitive(70), p.fields["struttura"])
        assertFalse("escluso_vetrina solo se cambia", p.fields.containsKey("escluso_vetrina"))
        assertFalse(p.privateChanged)
    }

    @Test
    fun scaffaleNumericoComeNumero() {
        val p = buildWinePatch(wine, formFrom(wine).copy(posizione = "56")).getOrThrow()
        assertEquals(5.6, p.fields["posizione"]!!.jsonPrimitive.content.toDouble(), 0.001)
    }

    @Test
    fun controlliComeSulSito() {
        val f = formFrom(wine)
        assertEquals(POSITION_ERROR, buildWinePatch(wine, f.copy(posizione = "Box9")).exceptionOrNull()?.message)
        assertTrue(buildWinePatch(wine, f.copy(posizione = "")).isFailure)
        assertTrue(buildWinePatch(wine, f.copy(quantita = "")).isFailure)
        assertTrue(buildWinePatch(wine, f.copy(prezzo = "abc")).isFailure)
        assertTrue(buildWinePatch(wine, f.copy(annata = "")).isFailure)
    }

    @Test
    fun campiVuotiDiventanoNullETipoNonAmmessoSiSvuota() {
        val f = formFrom(wine).copy(prezzo = "", regione = "Bordeaux", tipoVino = "Brut", sitoProduttore = "  ")
        val p = buildWinePatch(wine, f).getOrThrow()
        assertEquals(JsonNull, p.fields["prezzo"])
        assertEquals(JsonNull, p.fields["regione"])          // Bordeaux non è una regione italiana
        assertEquals(JsonNull, p.fields["dolcezza"])         // Brut non è ammesso per un vino Fermo
        assertEquals(JsonNull, p.fields["sito_produttore"])
    }

    @Test
    fun prezzoAcquistoEVetrinaSoloSeCambiano() {
        val p = buildWinePatch(wine, formFrom(wine).copy(prezzoAcquisto = "9,5", esclusoVetrina = true)).getOrThrow()
        assertTrue(p.privateChanged)
        assertEquals(9.5, p.prezzoAcquisto!!, 0.001)
        assertEquals(JsonPrimitive(true), p.fields["escluso_vetrina"])
    }

    @Test
    fun percorsiNelloStorageComeIlSito() {
        assertEquals("w1-1.jpg", storagePath("egon", "w1-1.jpg"))
        assertEquals("marco/w1-1.jpg", storagePath("marco", "w1-1.jpg"))
        assertEquals("Scheda_tecnica__2020_.pdf", safeFileName("Scheda tecnica (2020).pdf"))
        assertEquals(listOf("Secco", "Dolce"), tipiVinoPer("Fermo"))
        assertEquals("10", EDIT_GRADI.first())
        assertEquals("20", EDIT_GRADI.last())
    }
}
