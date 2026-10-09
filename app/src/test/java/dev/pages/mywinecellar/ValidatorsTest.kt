package dev.pages.mywinecellar

import dev.pages.mywinecellar.data.isValidEmail
import dev.pages.mywinecellar.data.isValidPhone
import dev.pages.mywinecellar.ui.RequestMode
import dev.pages.mywinecellar.ui.validateRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test
    fun email() {
        assertTrue(isValidEmail("nome@dominio.it"))
        assertFalse(isValidEmail("nome@dominio"))
        assertFalse(isValidEmail("nome dominio.it"))
        assertFalse(isValidEmail(""))
    }

    @Test
    fun telefono() {
        assertTrue(isValidPhone("+39 333 1234567"))
        assertFalse(isValidPhone("12345"))
        assertFalse(isValidPhone("abc123456"))
    }

    @Test
    fun carrelloRichiedeEmailETelefono() {
        assertEquals("Email e telefono sono entrambi obbligatori.", validateRequest(RequestMode.CART, "a@b.it", "", ""))
        assertNull(validateRequest(RequestMode.CART, "a@b.it", "3331234567", ""))
    }

    @Test
    fun assistenzaRichiedeMessaggioEUnRecapito() {
        assertEquals("Scrivi un messaggio prima di inviare.", validateRequest(RequestMode.SUPPORT, "a@b.it", "", " "))
        assertEquals(
            "Inserisci almeno un indirizzo email o un numero di telefono.",
            validateRequest(RequestMode.SUPPORT, "", "", "ciao"),
        )
        assertNull(validateRequest(RequestMode.SUPPORT, "", "3331234567", "ciao"))
    }

    @Test
    fun disponibilitaBastaUnRecapito() {
        assertNull(validateRequest(RequestMode.AVAILABILITY, "a@b.it", "", ""))
        assertEquals("L'indirizzo email non sembra valido.", validateRequest(RequestMode.AVAILABILITY, "xx", "", ""))
    }
}
