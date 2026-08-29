package manutenzioni.domain.service

import manutenzioni.domain.model.Attivita
import manutenzioni.domain.model.ImpiantoStandard
import manutenzioni.domain.model.Periodo
import manutenzioni.domain.model.TipoPeriodo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AntincendioAttivitaResolverTest {

    private val attivitaBase = Attivita(
        nAttivita = 1,
        tipoAttivita = "Prova fumo",
        descrizione = "Prova Rivelatori Fumo",
        frequenza = Periodo(TipoPeriodo.M, 6),
        targetImpiantoCod = null
    )

    private val attivitaGe = Attivita(
        nAttivita = 2,
        tipoAttivita = "Prova SGANCIO",
        descrizione = "Sgancio GE",
        frequenza = Periodo(TipoPeriodo.M, 6),
        targetImpiantoCod = "GE"
    )

    private val attivitaPem = Attivita(
        nAttivita = 3,
        tipoAttivita = "Prova PEM",
        descrizione = "Sgancio PEM",
        frequenza = Periodo(TipoPeriodo.M, 6),
        targetImpiantoCod = "PEM"
    )
    
    private val attivitaGas = Attivita(
        nAttivita = 4,
        tipoAttivita = "Prova GAS",
        descrizione = "Prova GAS",
        frequenza = Periodo(TipoPeriodo.A, 1),
        targetImpiantoCod = "RIG"
    )

    private val impiantoRi = ImpiantoStandard(
        codIntervento = "RI",
        nomeCompleto = "Rilevazione Incendi",
        listaAttivita = listOf(attivitaBase, attivitaGe, attivitaPem, attivitaGas)
    )

    @Test
    fun `resolveAttivita con cantiere vuoto mostra solo base per 6 mesi`() {
        val filtrate = AntincendioAttivitaResolver.resolveAttivita(
            impianto = impiantoRi,
            impiantiNelCantiere = emptyList(),
            frequenza = Periodo(TipoPeriodo.M, 6)
        )
        
        assertEquals(1, filtrate.size)
        assertEquals("Prova fumo", filtrate.first().tipoAttivita)
    }

    @Test
    fun `resolveAttivita con cantiere che ha GE mostra base e GE per 6 mesi`() {
        val ge = ImpiantoStandard(codIntervento = "GE", nomeCompleto = "Gruppo")
        
        val filtrate = AntincendioAttivitaResolver.resolveAttivita(
            impianto = impiantoRi,
            impiantiNelCantiere = listOf(impiantoRi, ge),
            frequenza = Periodo(TipoPeriodo.M, 6)
        )
        
        assertEquals(2, filtrate.size)
        assertTrue(filtrate.any { it.targetImpiantoCod == null })
        assertTrue(filtrate.any { it.targetImpiantoCod == "GE" })
    }

    @Test
    fun `resolveAttivita per 1 anno include base, GE e GAS se RIG e GE sono presenti`() {
        val ge = ImpiantoStandard(codIntervento = "GE", nomeCompleto = "Gruppo")
        val rig = ImpiantoStandard(codIntervento = "RIG", nomeCompleto = "Rilevazione Gas")
        
        val filtrate = AntincendioAttivitaResolver.resolveAttivita(
            impianto = impiantoRi,
            impiantiNelCantiere = listOf(impiantoRi, ge, rig),
            frequenza = Periodo(TipoPeriodo.A, 1) // 12 mesi include 6 mesi e 12 mesi
        )
        
        assertEquals(3, filtrate.size)
        assertTrue(filtrate.any { it.targetImpiantoCod == null }) // Base (6m)
        assertTrue(filtrate.any { it.targetImpiantoCod == "GE" }) // GE (6m)
        assertTrue(filtrate.any { it.targetImpiantoCod == "RIG" }) // GAS (1a)
    }
}
