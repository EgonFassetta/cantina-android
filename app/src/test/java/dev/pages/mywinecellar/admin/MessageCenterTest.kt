package dev.pages.mywinecellar.admin

import dev.pages.mywinecellar.data.parseWines
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Test

/** Centro messaggi: stesse informazioni che mostra il sito. */
class MessageCenterTest {
    private fun arr(s: String): JsonArray = Json.parseToJsonElement(s).jsonArray
    private val wines = parseWines("""[{"id":"w2","cantina":"Cantina Prova","nome":"Bianco","annata":2020}]""").associateBy { it.id }

    @Test
    fun ordini() {
        val m = parseMessages(MsgKind.ORDERS, arr("""[{"id":"o1","creato_il":"2026-11-01T10:00:00Z","totale":51,
            "items":[{"nome":"Rosso","quantita":2},{"nome":"Bianco","quantita":1}],"contatto_email":"a@b.it","contatto_telefono":"333"}]"""), wines)
        assertEquals(1, m.size)
        assertEquals("01/11/2026 · € 51,00", m[0].title)
        assertEquals("Rosso x2, Bianco x1", m[0].text)
        assertEquals("a@b.it", m[0].email)
    }

    @Test
    fun richiesteDiDisponibilitaEVinoNonPiuPresente() {
        val m = parseMessages(MsgKind.AVAILABILITY, arr("""[{"id":"a1","creato_il":"2026-11-02","wine_id":"w2","note":null},
            {"id":"a2","creato_il":"2026-11-02","wine_id":"zz","note":"Ancora?"},{"id":null}]"""), wines)
        assertEquals(2, m.size)
        assertEquals("Bianco (Cantina Prova, 2020) · 02/11/2026", m[0].title)
        assertEquals("(nessuna nota)", m[0].text)
        assertEquals("Vino non più presente · 02/11/2026", m[1].title)
    }

    @Test
    fun supporto() {
        val m = parseMessages(MsgKind.SUPPORT, arr("""[{"id":"s1","creato_il":"2026-11-03T10:00:00Z","messaggio":"Ciao"}]"""), emptyMap())
        assertEquals("03/11/2026", m[0].title)
        assertEquals("Ciao", m[0].text)
        assertEquals("", m[0].phone)
    }
}
