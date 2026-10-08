package dev.pages.mywinecellar

import dev.pages.mywinecellar.data.filterWines
import dev.pages.mywinecellar.data.parseWines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogLogicTest {

    private val sample = """
        [
          {"id":"w1","cantina":"Zenato","nome":"Amarone","tipo":"Amarone della Valpolicella","annata":2015,
           "quantita":3,"grado":15.5,"valore mercato":"120.50","disponibile_vendita":true,
           "colore_vino":"Rosso","effervescenza":"Fermo","dolcezza":"Secco","regione":"Veneto","paese":"Italia","immagine_url":null},
          {"id":"w2","cantina":"Foo","nome":null,"tipo":"Prosecco","annata":"2020","quantita":0,"disponibile_vendita":false},
          {"id":"w3","cantina":"Bar","nome":"Rosso Esaurito","annata":"2018","quantita":5,"disponibile_vendita":false},
          {"id":null,"cantina":"Senza id"}
        ]
    """.trimIndent()

    @Test
    fun leggeNumeriTestiENull() {
        val wines = parseWines(sample)
        assertEquals(3, wines.size) // la riga senza id viene scartata
        val a = wines[0]
        assertEquals("2015", a.annata)
        assertEquals(3, a.quantita)
        assertEquals("15,5", a.grado)
        assertEquals(120.5, a.valoreMercato!!, 0.001)
        assertTrue(a.inVendita)
        assertNull(a.immagineUrl)
        assertEquals("Secco", a.tipoVino)
        assertEquals("Fermo", a.caratteristica)
    }

    @Test
    fun titoloUsaTipologiaSeMancaIlNome() {
        val wines = parseWines(sample)
        assertEquals("Prosecco", wines[1].titolo)
        assertFalse(wines[1].inVendita)
    }

    @Test
    fun soloDisponibiliRichiedeFlagEQuantita() {
        val wines = parseWines(sample)
        val onlyAvailable = filterWines(wines, "", true)
        assertEquals(listOf("w1"), onlyAvailable.map { it.id })
        // w3 ha quantità 5 ma il flag di vendita spento: non deve comparire
        assertFalse(onlyAvailable.any { it.id == "w3" })
    }

    @Test
    fun ricercaTrovaPerCantinaRegioneEAnnata() {
        val wines = parseWines(sample)
        assertEquals(1, filterWines(wines, "zenato", false).size)
        assertEquals(1, filterWines(wines, "VENETO", false).size)
        assertEquals(1, filterWines(wines, "2020", false).size)
        assertEquals(0, filterWines(wines, "xyz", false).size)
    }
}
