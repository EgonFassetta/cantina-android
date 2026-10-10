package dev.pages.mywinecellar

import dev.pages.mywinecellar.data.WineCache
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WineCacheTest {
    private fun tempFile(): File = File.createTempFile("wines", ".json").also { it.delete() }

    @Test
    fun senzaFileNonCiSonoVini() {
        assertNull(WineCache(tempFile()).load())
    }

    @Test
    fun salvaERilegge() {
        val f = tempFile()
        val cache = WineCache(f)
        cache.save("""[{"id":"w1","cantina":"Zenato","nome":"Amarone"},{"id":"w2","nome":"Soave"}]""")
        val back = cache.load()!!
        assertEquals(listOf("w1", "w2"), back.map { it.id })
        assertEquals("Zenato", back[0].cantina)
        f.delete()
    }

    @Test
    fun fileRottoNonFaCrollare() {
        val f = tempFile()
        f.writeText("non è json")
        assertNull(WineCache(f).load())
        f.delete()
    }
}
