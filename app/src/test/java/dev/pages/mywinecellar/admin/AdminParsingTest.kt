package dev.pages.mywinecellar.admin

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminParsingTest {
    private fun obj(s: String) = Json.parseToJsonElement(s).jsonObject

    @Test
    fun leggeICampiELeSegnalazioniDelServer() {
        val r = parseLabelReading(
            obj(
                """{"ok":true,"leggibilita":"bassa","note":"Retro mancante","campi":{"cantina":" Zenato ","nome":null,"annata":2015,
                "grado":13.5,"colore_vino":"Rosso","effervescenza":"Fermo","dolcezza":"Secco"},
                "simili":[{"id":"w1","cantina":"Zenato","nome":"Amarone","annata":"2015","quantita":3,"posizione":"3.4","corrispondenza":"stessa-annata"},
                {"id":"w2","cantina":"Zenato","nome":"Amarone","annata":"2012","quantita":"2","corrispondenza":"altra-annata"},{"id":""}]}""",
            ),
        )
        assertEquals("Zenato", r.campi.cantina)
        assertEquals("", r.campi.nome)
        assertEquals("2015", r.campi.annata)
        assertEquals("13.5", r.campi.grado)
        assertEquals("bassa", r.leggibilita)
        assertEquals(2, r.simili.size)                 // la riga senza id viene scartata
        assertTrue(r.simili[0].stessaAnnata)
        assertFalse(r.simili[1].stessaAnnata)
        assertEquals(2, r.simili[1].quantita)
        assertEquals("3.4", r.simili[0].posizione)
    }

    @Test
    fun rispostaVuotaNonFaCrollare() {
        val r = parseLabelReading(obj("""{"ok":true}"""))
        assertEquals("", r.campi.cantina)
        assertTrue(r.simili.isEmpty())
    }

    @Test
    fun icampiVannoAlServerConIChiaviDelSito() {
        val j = labelFieldsToJson(LabelFields(cantina = "A", coloreVino = "Rosso", sitoProduttore = "x.it", dolcezza = "Brut"))
        assertEquals("A", j["cantina"]!!.toString().trim('"'))
        assertEquals("Rosso", j["colore_vino"]!!.toString().trim('"'))
        assertEquals("x.it", j["sito_produttore"]!!.toString().trim('"'))
        assertEquals("Brut", j["dolcezza"]!!.toString().trim('"'))
        assertEquals(12, j.size)
    }
}
