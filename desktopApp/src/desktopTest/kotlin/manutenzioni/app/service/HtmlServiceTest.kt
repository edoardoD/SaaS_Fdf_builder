package manutenzioni.app.service

import manutenzioni.domain.model.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertContains
import kotlin.test.assertFalse

/**
 * Test per HtmlService — il template engine che genera l'HTML
 * da cui iText7 produce il PDF AcroForm.
 *
 * Nota: questi test verificano la logica di sostituzione dei placeholder
 * e la generazione delle righe <tr>. NON testano la conversione PDF.
 */
class HtmlServiceTest {

    private val htmlService = HtmlService()

    // Fixture comune
    private val impiantoGE = ImpiantoStandard(
        id = "test-ge",
        codIntervento = "GE",
        nomeCompleto = "Gruppo Elettrogeno",
        premessa = "Premessa di sicurezza per GE.",
        listaAttivita = listOf(
            Attivita(1, "Controllo visivo", "Controllo stato generale", Periodo(TipoPeriodo.M, 1)),
            Attivita(2, "Prova funzionale", "Prova avviamento", Periodo(TipoPeriodo.M, 3)),
            Attivita(3, "Manutenzione", "Cambio olio e filtri", Periodo(TipoPeriodo.A, 1))
        ),
        listaNormative = listOf(Normativa("CEI 11-20", "Impianti di produzione"))
    )

    @Test
    fun `buildHtml sostituisce COD_SCHEDA con codIntervento`() {
        val html = htmlService.buildHtml(
            impianto = impiantoGE,
            attivitaFiltrate = impiantoGE.listaAttivita,
            frequenza = Periodo(TipoPeriodo.A, 1),
            clienteNome = null
        )
        assertContains(html, "GE")
    }

    @Test
    fun `buildHtml sostituisce OGGETTO con nomeCompleto`() {
        val html = htmlService.buildHtml(
            impianto = impiantoGE,
            attivitaFiltrate = impiantoGE.listaAttivita,
            frequenza = Periodo(TipoPeriodo.A, 1),
            clienteNome = null
        )
        assertContains(html, "Gruppo Elettrogeno")
    }

    @Test
    fun `buildHtml inietta nome cliente`() {
        val html = htmlService.buildHtml(
            impianto = impiantoGE,
            attivitaFiltrate = impiantoGE.listaAttivita,
            frequenza = Periodo(TipoPeriodo.A, 1),
            clienteNome = "Acme S.r.l."
        )
        assertContains(html, "Cliente: Acme S.r.l.")
    }

    @Test
    fun `buildHtml senza cliente mostra placeholder generico`() {
        val html = htmlService.buildHtml(
            impianto = impiantoGE,
            attivitaFiltrate = impiantoGE.listaAttivita,
            frequenza = Periodo(TipoPeriodo.A, 1),
            clienteNome = null
        )
        assertContains(html, "<p>Cliente</p>")
    }

    @Test
    fun `buildHtml genera righe con radio button per esiti`() {
        val attivitaFiltrate = listOf(
            Attivita(1, "Controllo", "Test attività", Periodo(TipoPeriodo.M, 1))
        )
        val html = htmlService.buildHtml(
            impianto = impiantoGE,
            attivitaFiltrate = attivitaFiltrate,
            frequenza = Periodo(TipoPeriodo.M, 1),
            clienteNome = null
        )

        // Verifica che i radio button abbiano nomi univoci
        assertContains(html, "esito_GE_1")
        // Verifica che ci siano tutti e 6 gli esiti
        listOf("P", "PI", "NA", "NP", "VN", "B").forEach { esito ->
            assertContains(html, "value=\"$esito\"")
        }
    }

    @Test
    fun `buildHtml genera campo nota per ogni riga`() {
        val attivitaFiltrate = listOf(
            Attivita(1, "Controllo", "Test attività", Periodo(TipoPeriodo.M, 1))
        )
        val html = htmlService.buildHtml(
            impianto = impiantoGE,
            attivitaFiltrate = attivitaFiltrate,
            frequenza = Periodo(TipoPeriodo.M, 1),
            clienteNome = null
        )
        assertContains(html, "note_GE_1")
    }

    @Test
    fun `buildHtml con QuadroBT mostra sigla nel codice scheda`() {
        val quadro = QuadroBT(
            id = "test-q",
            codIntervento = "Q",
            nomeCompleto = "Quadri Elettrici BT",
            sigla = "QG1",
            descrizioneQuadro = "Quadro Generale Piano Terra",
            listaAttivita = listOf(
                Attivita(1, "Controllo", "Controllo visivo", Periodo(TipoPeriodo.M, 1))
            )
        )
        val html = htmlService.buildHtml(
            impianto = quadro,
            attivitaFiltrate = quadro.listaAttivita,
            frequenza = Periodo(TipoPeriodo.M, 1),
            clienteNome = null
        )
        assertContains(html, "Q - QG1")
    }

    @Test
    fun `buildHtml con QuadroBT mostra ubicazione e interruttori nella premessa`() {
        val quadro = QuadroBT(
            id = "test-q",
            codIntervento = "Q",
            nomeCompleto = "Quadri Elettrici BT",
            sigla = "QG1",
            descrizioneQuadro = "Piano Terra",
            listaInterruttori = listOf(
                InterruttoreBT(id = "i1", nome = "ABB 63A"),
                InterruttoreBT(id = "i2", nome = "Schneider 32A")
            ),
            listaAttivita = listOf(
                Attivita(1, "Controllo", "Test", Periodo(TipoPeriodo.M, 1))
            )
        )
        val html = htmlService.buildHtml(
            impianto = quadro,
            attivitaFiltrate = quadro.listaAttivita,
            frequenza = Periodo(TipoPeriodo.M, 1),
            clienteNome = null
        )
        assertContains(html, "Ubicazione Quadro: Piano Terra")
        assertContains(html, "ABB 63A")
        assertContains(html, "Schneider 32A")
    }

    @Test
    fun `buildHtml con lista vuota di attivita non genera righe`() {
        val html = htmlService.buildHtml(
            impianto = impiantoGE,
            attivitaFiltrate = emptyList(),
            frequenza = Periodo(TipoPeriodo.A, 1),
            clienteNome = null
        )
        // Nessun campo esito generato
        assertFalse(html.contains("esito_GE_"))
    }

    @Test
    fun `buildHtml escapa caratteri HTML speciali`() {
        val impiantoConCaratteriSpeciali = ImpiantoStandard(
            id = "test-special",
            codIntervento = "T&S",
            nomeCompleto = "Test <Script> & Alert",
            listaAttivita = listOf(
                Attivita(1, "Test", "Descrizione con \"virgolette\" e <tag>", Periodo(TipoPeriodo.M, 1))
            )
        )
        val html = htmlService.buildHtml(
            impianto = impiantoConCaratteriSpeciali,
            attivitaFiltrate = impiantoConCaratteriSpeciali.listaAttivita,
            frequenza = Periodo(TipoPeriodo.M, 1),
            clienteNome = "O'Brien & Associati"
        )
        // I caratteri speciali devono essere escapati
        assertContains(html, "&amp;")
        assertFalse(html.contains("<Script>")) // Il tag deve essere escapato
    }
}
