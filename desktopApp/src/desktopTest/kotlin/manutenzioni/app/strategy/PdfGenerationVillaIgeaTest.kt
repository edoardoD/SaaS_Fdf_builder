package manutenzioni.app.strategy

import kotlinx.coroutines.runBlocking
import manutenzioni.app.data.MongoManutenzioneRepository
import manutenzioni.domain.model.Impianto
import manutenzioni.domain.model.Periodo
import manutenzioni.domain.service.FrequencyFilter
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test di integrazione — Generazione PDF cantiere Villa Igea.
 *
 * Percorso dati: Ospedali Privati → Villa Igea → [AMB, PEM]
 *
 * Regola verificata:
 *   Per ogni impianto deve essere prodotto **esattamente 1 PDF** per chiamata,
 *   indipendentemente dal valore del campo `quantita` salvato in MongoDB.
 *
 * Se il test fallisce con successCount != 1, il messaggio diagnostico mostra
 * la `quantita` dell'impianto nel DB: se è > 1, quella è la causa
 * (il ViewModel passa `copies = impianto.quantita` → batch indesiderato).
 *
 * Prerequisito: MongoDB in ascolto su localhost:27017 con DB manutenzioni_db.
 */
class PdfGenerationVillaIgeaTest {

    companion object {
        private const val MONGO_URI    = "mongodb://localhost:27017"
        private const val DB_NAME      = "manutenzioni_db"
        private const val CLIENTE_NOME = "Ospedali Privati"
        private const val CANTIERE_NOME = "Villa Igea"
    }

    private val repo     = MongoManutenzioneRepository(MONGO_URI, DB_NAME)
    private val strategy = HtmlToPdfStrategy()

    // ─── Navigazione dati ────────────────────────────────────────────────────

    private suspend fun findCliente() =
        repo.caricaClienti().firstOrNull { it.nome.equals(CLIENTE_NOME, ignoreCase = true) }

    private suspend fun findCantiere(clienteId: String) =
        repo.getCantieriForCliente(clienteId)
            .firstOrNull { it.nome.equals(CANTIERE_NOME, ignoreCase = true) }

    private suspend fun findImpianto(cantiereId: String, cod: String): Impianto? =
        repo.getImpiantiForCantiere(cantiereId)
            .firstOrNull { it.codIntervento.equals(cod, ignoreCase = true) }

    private fun primeFrequenza(impianto: Impianto): Periodo {
        val disponibili = FrequencyFilter.frequenzeDisponibili(impianto.listaAttivita)
        assertTrue(
            disponibili.isNotEmpty(),
            "Impianto '${impianto.codIntervento}' non ha attività registrate: " +
            "impossibile determinare una frequenza."
        )
        return disponibili.first()
    }

    // ─── Test di bonifica globale del DB ─────────────────────────────────────

    @Test
    fun `bonifica quantita a 1 nel database per TUTTI gli impianti`() = runBlocking {
        val tuttiImpianti = repo.caricaImpianti()
        var bonificati = 0
        for (imp in tuttiImpianti) {
            if (imp.quantita != 1) {
                val aggiornato = imp.copyWithBasicParams(quantita = 1)
                repo.salvaImpianto(aggiornato)
                println("✓ Bonificato impianto '${imp.codIntervento}' (${imp.nomeCompleto}, cantiereId=${imp.cantiereId}): quantita ${imp.quantita} -> 1")
                bonificati++
            }
        }
        println("Totale impianti bonificati a quantita=1: $bonificati su ${tuttiImpianti.size}")
    }

    // ─── Test per AMB ────────────────────────────────────────────────────────

    @Test
    fun `genera esattamente 1 PDF per AMB in Villa Igea`() = runBlocking {
        generaEVerifica("AMB")
    }

    // ─── Test per PEM ────────────────────────────────────────────────────────

    @Test
    fun `genera esattamente 1 PDF per PEM in Villa Igea`() = runBlocking {
        generaEVerifica("PEM")
    }

    // ─── Logica comune ───────────────────────────────────────────────────────

    private suspend fun generaEVerifica(codImpianto: String) {
        // 1. Recupera cliente
        val cliente = findCliente()
        assertNotNull(
            cliente,
            "Cliente '$CLIENTE_NOME' non trovato in MongoDB. " +
            "Verificare che i dati siano stati migrati correttamente."
        )

        // 2. Recupera cantiere
        val cantiere = findCantiere(cliente.id)
        assertNotNull(
            cantiere,
            "Cantiere '$CANTIERE_NOME' non trovato per cliente '${cliente.nome}'. " +
            "Verificare che il cantiere esista nel DB."
        )

        // 3. Recupera impianto
        val impianto = findImpianto(cantiere.id, codImpianto)
        assertNotNull(
            impianto,
            "Impianto '$codImpianto' non trovato nel cantiere '${cantiere.nome}'. " +
            "Verificare che l'impianto sia stato aggiunto a questo cantiere."
        )

        // 4. Verifica che nel database non ci sia un parametro quantità che faccia stampare più copie
        val quantitaInDb = impianto.quantita
        assertEquals(
            expected = 1,
            actual = quantitaInDb,
            message = "Nel database l'impianto '${impianto.codIntervento}' ha quantita=$quantitaInDb invece di 1! " +
                    "Questo parametro quantità causa la stampa di copie multiple ($quantitaInDb copie anziché 1)."
        )

        // 5. Cartella output temporanea (rimossa dopo il test)
        val outputDir = Files.createTempDirectory("pdf_test_${codImpianto}_").toFile()
        try {
            // 6. Genera — 1 PDF per istanza di impianto
            val frequenza = primeFrequenza(impianto)
            val result = strategy.generateBatch(
                impianto    = impianto,
                frequenza   = frequenza,
                outputDir   = outputDir,
                copies      = 1,
                clienteNome = cliente.nome
            )

            // 7. Messaggio diagnostico (mostrato solo se il test fallisce)
            val diagnostica = buildString {
                appendLine()
                appendLine("═══════════════════════════════════════════════════════════")
                appendLine("  DIAGNOSTICA  —  Impianto: $codImpianto")
                appendLine("═══════════════════════════════════════════════════════════")
                appendLine("  quantita in DB   : $quantitaInDb")
                appendLine("  copies richieste : 1")
                appendLine("  PDF generati     : ${result.successCount}")
                appendLine("  PDF richiesti    : ${result.totalRequested}")
                if (result.errors.isNotEmpty()) {
                    appendLine("  Errori           : ${result.errors}")
                }
                if (quantitaInDb > 1) {
                    appendLine()
                    appendLine("  ⚠️  CAUSA PROBABILE: il campo 'quantita' dell'impianto")
                    appendLine("      è $quantitaInDb nel database.")
                    appendLine("      Il ViewModel chiama generateBatch(copies = impianto.quantita)")
                    appendLine("      producendo $quantitaInDb PDF invece di 1.")
                    appendLine("      Soluzione: impostare quantita = 1 su questo impianto nel DB,")
                    appendLine("      oppure verificare che createNewImpianto() non salvi quantita > 1.")
                }
                appendLine("═══════════════════════════════════════════════════════════")
            }

            // 8. Assert: esattamente 1 PDF prodotto
            assertEquals(
                expected = 1,
                actual   = result.totalRequested,
                message  = "totalRequested deve essere 1, ma è ${result.totalRequested}.$diagnostica"
            )
            assertEquals(
                expected = 1,
                actual   = result.successCount,
                message  = "successCount deve essere 1, ma è ${result.successCount}.$diagnostica"
            )
            assertTrue(
                actual  = result.isFullSuccess,
                message = "Il batch deve completarsi senza errori.$diagnostica"
            )

            // 9. Verifica che il file esista su disco e non sia vuoto
            val pdfFile = result.generatedFiles.first()
            assertTrue(
                actual  = pdfFile.exists(),
                message = "Il file PDF deve esistere su disco: ${pdfFile.absolutePath}$diagnostica"
            )
            assertTrue(
                actual  = pdfFile.length() > 0L,
                message = "Il file PDF non deve essere vuoto: ${pdfFile.absolutePath}$diagnostica"
            )

        } finally {
            // Pulizia file temporanei — non lasciare tracce sul filesystem
            outputDir.deleteRecursively()
        }
    }
}
