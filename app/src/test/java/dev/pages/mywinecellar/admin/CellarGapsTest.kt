package dev.pages.mywinecellar.admin

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** «Cosa manca nella cantina»: lettura della risposta del server come fa il sito. */
class CellarGapsTest {
    private fun obj(s: String) = Json.parseToJsonElement(s).jsonObject

    @Test
    fun leggeLAnalisi() {
        val r = parseGapsReport(obj("""{"ok":true,"ospiti":8,"bottigliePerPortata":2,"totale":{"bottiglie":"190","etichette":120},
            "nonClassificati":{"bottiglie":3},
            "categorie":[{"id":"bollicine","label":"Bollicine","stato":"basso","mancano":4,"bottiglie":2,"obiettivo":6,"quota":1.05,
              "etichette":1,"produttori":1,"esempi":["A",null,"B"]},{"label":"senza id"}],
            "portate":[{"portata":"Aperitivo","categorie":["Bollicine"],"stato":"scarsa","bottiglie":0,"serate":0}],
            "stili":[{"stile":"Amarone","categoria":"rossi","bottiglie":5,"perche":"x"}]}"""))
        assertEquals(190, r.bottiglie)
        assertEquals(3, r.nonClassificate)
        assertEquals(1, r.categorie.size)                       // la categoria senza id viene scartata
        assertEquals(listOf("A", "B"), r.categorie[0].esempi)
        assertEquals("Pochi (mancano 4)", gapStatoText(r.categorie[0]))
        assertEquals("Scarsa", gapCourseText(r.portate[0].stato))
        assertEquals("Assenti", gapStatoText(r.categorie[0].copy(stato = "assente")))
        assertEquals("Ok", gapStatoText(r.categorie[0].copy(stato = "ok")))
    }

    @Test
    fun suggerimentiSoloConLinkWeb() {
        val s = parseGapsSuggestions(obj("""{"ok":true,"nota":"n","acquisti":[{"stile":"Ripasso","categoria":"rossi_medi",
            "colma":4,"abbinamenti":["a"],"bottiglie":4,"fonti":[{"url":"https://x.it/a","title":"t"},{"url":"javascript:alert(1)"}]}]}"""))
        assertEquals(1, s.acquisti.size)
        assertEquals("rossi_medi", s.acquisti[0].categoria)       // senza categoriaLabel si usa l'id
        assertEquals(listOf("https://x.it/a"), s.acquisti[0].fonti.map { it.url })
        assertEquals("n", s.nota)
    }
}
