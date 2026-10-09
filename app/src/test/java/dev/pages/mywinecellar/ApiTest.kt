package dev.pages.mywinecellar

import dev.pages.mywinecellar.data.ApiException
import dev.pages.mywinecellar.data.AuthSession
import dev.pages.mywinecellar.data.AuthUser
import dev.pages.mywinecellar.data.MemorySessionStore
import dev.pages.mywinecellar.data.SupabaseApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** Prove della comunicazione con il server, con un server finto (nessuna rete reale). */
class ApiTest {
    private lateinit var server: MockWebServer
    private lateinit var store: MemorySessionStore
    private lateinit var api: SupabaseApi

    private val anonSession = """{"access_token":"AT1","refresh_token":"RT1","user":{"id":"u1","email":"","is_anonymous":true}}"""

    private fun newApi(initial: AuthSession? = null): SupabaseApi {
        store = MemorySessionStore(initial)
        return SupabaseApi(store, baseUrl = server.url("/").toString().trimEnd('/'), apiKey = "KEY")
    }

    private val logged = AuthSession("AT1", "RT1", AuthUser("u1", "", true))

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun creaSessioneAnonimaELaSalva() = runTest {
        api = newApi()
        server.enqueue(MockResponse().setBody(anonSession))
        val s = api.startSession()
        assertEquals("AT1", s.accessToken)
        assertNull(s.email)
        assertEquals("AT1", store.load()?.accessToken)
        val req = server.takeRequest()
        assertEquals("/auth/v1/signup", req.path)
        assertEquals("KEY", req.getHeader("apikey"))
    }

    @Test
    fun preferitoNuovoFaPatchPoiInserisce() = runTest {
        api = newApi(logged)
        server.enqueue(MockResponse().setBody("[]"))          // nessuna riga da aggiornare
        server.enqueue(MockResponse().setResponseCode(201))   // inserimento
        api.setFavorite("w1", true)
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        assertTrue(patch.path!!.contains("wine_user_state?user_id=eq.u1&wine_id=eq.w1"))
        assertEquals("Bearer AT1", patch.getHeader("Authorization"))
        assertTrue(patch.body.readUtf8().contains("\"preferito\":true"))
        val ins = server.takeRequest()
        assertEquals("POST", ins.method)
        assertTrue(ins.path!!.contains("on_conflict=user_id,wine_id"))
        val body = ins.body.readUtf8()
        assertTrue(body.contains("\"user_id\":\"u1\""))
        assertTrue(body.contains("\"wine_id\":\"w1\""))
        assertTrue(body.contains("\"preferito\":true"))
    }

    @Test
    fun preferitoEsistenteBastaIlPatch() = runTest {
        api = newApi(logged)
        server.enqueue(MockResponse().setBody("""[{"wine_id":"w1"}]"""))
        api.setFavorite("w1", false)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun leggeIPreferiti() = runTest {
        api = newApi(logged)
        server.enqueue(MockResponse().setBody("""[{"wine_id":"w1"},{"wine_id":"w2"}]"""))
        assertEquals(setOf("w1", "w2"), api.fetchFavorites())
    }

    @Test
    fun carrelloAggiungeERimuove() = runTest {
        api = newApi(logged)
        server.enqueue(MockResponse().setResponseCode(201))
        server.enqueue(MockResponse().setResponseCode(204))
        api.setCartQty("w1", 2)
        api.setCartQty("w1", 0)
        val add = server.takeRequest()
        assertEquals("POST", add.method)
        assertTrue(add.body.readUtf8().contains("\"quantity\":2"))
        val del = server.takeRequest()
        assertEquals("DELETE", del.method)
        assertTrue(del.path!!.contains("wine_user_cart?user_id=eq.u1&wine_id=eq.w1"))
    }

    @Test
    fun leggeIlCarrello() = runTest {
        api = newApi(logged)
        server.enqueue(MockResponse().setBody("""[{"wine_id":"w1","quantity":2},{"wine_id":"w2","quantity":0}]"""))
        assertEquals(mapOf("w1" to 2), api.fetchCart())
    }

    @Test
    fun tokenScadutoVieneRinnovatoELaRichiestaRipetuta() = runTest {
        api = newApi(logged)
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"JWT expired"}"""))
        server.enqueue(MockResponse().setBody("""{"access_token":"AT2","refresh_token":"RT2","user":{"id":"u1","email":"","is_anonymous":true}}"""))
        server.enqueue(MockResponse().setBody("[]"))
        assertTrue(api.fetchFavorites().isEmpty())
        assertEquals("Bearer AT1", server.takeRequest().getHeader("Authorization"))
        val refresh = server.takeRequest()
        assertTrue(refresh.path!!.contains("grant_type=refresh_token"))
        assertTrue(refresh.body.readUtf8().contains("RT1"))
        assertEquals("Bearer AT2", server.takeRequest().getHeader("Authorization"))
        assertEquals("AT2", store.load()?.accessToken)
    }

    @Test
    fun erroreDelLimiteRichiesteArrivaComeMessaggio() = runTest {
        api = newApi(logged)
        server.enqueue(
            MockResponse().setResponseCode(429)
                .setBody("""{"ok":false,"error":"Hai inviato troppe richieste ravvicinate."}"""),
        )
        try {
            api.requestAvailability("w1", "a@b.it", "", "")
            fail("doveva fallire")
        } catch (e: ApiException) {
            assertEquals("Hai inviato troppe richieste ravvicinate.", e.message)
            assertEquals(429, e.status)
        }
    }

    @Test
    fun richiestaCarrelloInviaSoloViniEQuantita() = runTest {
        api = newApi(logged)
        server.enqueue(MockResponse().setBody("""{"ok":true}"""))
        api.requestCart("a@b.it", "3331234567", mapOf("w1" to 2))
        val req = server.takeRequest()
        assertEquals("/functions/v1/submit-request", req.path)
        val body = req.body.readUtf8()
        assertTrue(body.contains("\"type\":\"cart\""))
        assertTrue(body.contains("\"wine_id\":\"w1\""))
        assertTrue(body.contains("\"quantity\":2"))
    }

    @Test
    fun emailGiaRegistrataDaUnMessaggioChiaro() = runTest {
        api = newApi(logged)
        server.enqueue(MockResponse().setBody("""{"ok":false,"error":"Error updating user"}"""))
        try {
            api.linkEmail("a@b.it")
            fail("doveva fallire")
        } catch (e: ApiException) {
            assertTrue(e.message!!.contains("già registrata"))
        }
    }

    @Test
    fun accessoConEmailSalvaLaNuovaSessione() = runTest {
        api = newApi(logged)
        server.enqueue(
            MockResponse().setBody(
                """{"ok":true,"session":{"access_token":"AT9","refresh_token":"RT9","user":{"id":"u9","email":"a@b.it"}}}""",
            ),
        )
        api.loginEmail("a@b.it")
        assertEquals("a@b.it", api.session?.email)
        assertEquals("AT9", store.load()?.accessToken)
    }
}
