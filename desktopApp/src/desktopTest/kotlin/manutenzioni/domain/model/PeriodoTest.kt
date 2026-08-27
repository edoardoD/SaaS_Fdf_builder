package manutenzioni.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Test per Periodo — conversione mesi e label.
 * Copre la regola fondamentale: TipoPeriodo.M → valore diretto, TipoPeriodo.A → valore * 12
 */
class PeriodoTest {

    @Test
    fun `inMesi per mesi restituisce il valore diretto`() {
        assertEquals(1, Periodo(TipoPeriodo.M, 1).inMesi())
        assertEquals(3, Periodo(TipoPeriodo.M, 3).inMesi())
        assertEquals(6, Periodo(TipoPeriodo.M, 6).inMesi())
    }

    @Test
    fun `inMesi per anni restituisce valore per 12`() {
        assertEquals(12, Periodo(TipoPeriodo.A, 1).inMesi())
        assertEquals(24, Periodo(TipoPeriodo.A, 2).inMesi())
        assertEquals(36, Periodo(TipoPeriodo.A, 3).inMesi())
    }

    @Test
    fun `label singolare mese`() {
        assertEquals("1 Mese", Periodo(TipoPeriodo.M, 1).label())
    }

    @Test
    fun `label plurale mesi`() {
        assertEquals("3 Mesi", Periodo(TipoPeriodo.M, 3).label())
        assertEquals("6 Mesi", Periodo(TipoPeriodo.M, 6).label())
    }

    @Test
    fun `label singolare anno`() {
        assertEquals("1 Anno", Periodo(TipoPeriodo.A, 1).label())
    }

    @Test
    fun `label plurale anni`() {
        assertEquals("2 Anni", Periodo(TipoPeriodo.A, 2).label())
        assertEquals("3 Anni", Periodo(TipoPeriodo.A, 3).label())
    }

    @Test
    fun `periodi con stessi mesi ma tipi diversi hanno lo stesso inMesi`() {
        val m12 = Periodo(TipoPeriodo.M, 12)
        val a1 = Periodo(TipoPeriodo.A, 1)
        assertEquals(m12.inMesi(), a1.inMesi())
    }
}
