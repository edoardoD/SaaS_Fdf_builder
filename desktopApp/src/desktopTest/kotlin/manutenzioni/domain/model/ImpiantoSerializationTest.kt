package manutenzioni.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test di serializzazione/deserializzazione per la gerarchia Impianto.
 * 
 * Vincolo critico: `ignoreUnknownKeys = true` e ogni nuovo campo
 * DEVE avere un default per la retrocompatibilità JSON.
 */
class ImpiantoSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        classDiscriminator = "_t"
    }

    // =========== IMPIANTOSTANDARDARD ===========

    @Test
    fun `ImpiantoStandard roundtrip serializzazione`() {
        val original = ImpiantoStandard(
            id = "test-ge-001",
            codIntervento = "GE",
            nomeCompleto = "Gruppo Elettrogeno",
            premessa = "Le verifiche periodiche devono essere eseguite.",
            listaAttivita = listOf(
                Attivita(1, "Controllo visivo", "Controllo stato generale", Periodo(TipoPeriodo.M, 1)),
                Attivita(2, "Prova", "Prova avviamento", Periodo(TipoPeriodo.M, 3))
            ),
            listaNormative = listOf(
                Normativa("CEI 11-20", "Impianti di produzione")
            ),
            quantita = 2
        )
        val serialized = json.encodeToString<Impianto>(original)
        val deserialized = json.decodeFromString<Impianto>(serialized)

        assertTrue(deserialized is ImpiantoStandard)
        assertEquals(original.codIntervento, deserialized.codIntervento)
        assertEquals(original.nomeCompleto, deserialized.nomeCompleto)
        assertEquals(original.listaAttivita.size, deserialized.listaAttivita.size)
        assertEquals(original.quantita, deserialized.quantita)
    }

    // =========== QUADRO BT ===========

    @Test
    fun `QuadroBT serializza con sigla e interruttori`() {
        val quadro = QuadroBT(
            id = "test-q-001",
            codIntervento = "Q",
            nomeCompleto = "Quadri Elettrici BT",
            sigla = "QG1",
            descrizioneQuadro = "Quadro Generale Piano Terra",
            listaInterruttori = listOf(
                InterruttoreBT(id = "int-1", nome = "ABB 63A"),
                InterruttoreBT(id = "int-2", nome = "Schneider 32A")
            ),
            listaAttivita = listOf(
                Attivita(1, "Controllo", "Controllo visivo", Periodo(TipoPeriodo.M, 1))
            )
        )
        val serialized = json.encodeToString<Impianto>(quadro)
        val deserialized = json.decodeFromString<Impianto>(serialized)

        assertTrue(deserialized is QuadroBT)
        assertEquals("QG1", deserialized.sigla)
        assertEquals("Quadro Generale Piano Terra", deserialized.descrizioneQuadro)
        assertEquals(2, deserialized.listaInterruttori.size)
        assertEquals("ABB 63A", deserialized.listaInterruttori[0].nome)
    }

    // =========== RETROCOMPATIBILITÀ ===========

    @Test
    fun `deserializzazione con campi mancanti usa default`() {
        // Simula un JSON legacy senza i nuovi campi (sigla, descrizioneQuadro, listaInterruttori)
        val legacyJson = """
        {
            "_t": "QuadroBT",
            "id": "legacy-001",
            "codIntervento": "Q",
            "nomeCompleto": "Quadri BT Legacy",
            "listaAttivita": [],
            "listaNormative": [],
            "quantita": 1
        }
        """.trimIndent()

        val deserialized = json.decodeFromString<Impianto>(legacyJson)
        assertTrue(deserialized is QuadroBT)
        assertEquals("", deserialized.sigla) // Default vuoto
        assertEquals("", deserialized.descrizioneQuadro) // Default vuoto
        assertTrue(deserialized.listaInterruttori.isEmpty()) // Default vuota
    }

    @Test
    fun `deserializzazione ImpiantoStandard con campi extra ignora campi sconosciuti`() {
        val jsonConCampoExtra = """
        {
            "_t": "ImpiantoStandard",
            "id": "future-001",
            "codIntervento": "X",
            "nomeCompleto": "Impianto Futuro",
            "listaAttivita": [],
            "listaNormative": [],
            "quantita": 1,
            "campoInesistente": "valore"
        }
        """.trimIndent()

        // Non deve lanciare eccezione grazie a ignoreUnknownKeys = true
        val deserialized = json.decodeFromString<Impianto>(jsonConCampoExtra)
        assertNotNull(deserialized)
        assertEquals("X", deserialized.codIntervento)
    }

    // =========== POLIMORFISMO ===========

    @Test
    fun `polimorfismo serializza e deserializza tutti i sottotipi`() {
        val impianti: List<Impianto> = listOf(
            ImpiantoStandard(id = "1", codIntervento = "GE", nomeCompleto = "Gruppo Elettrogeno"),
            QuadroBT(id = "2", codIntervento = "Q", nomeCompleto = "Quadri BT", sigla = "QG1"),
            ImpiantoEmergenza(id = "3", codIntervento = "IS", nomeCompleto = "Illuminazione Sicurezza"),
            RilevazioneAntincendio(id = "4", codIntervento = "RI", nomeCompleto = "Rilevazione Incendio"),
            RilevazioneGas(id = "5", codIntervento = "RIG", nomeCompleto = "Rilevazione Gas"),
            QuadroMQT(id = "6", codIntervento = "QMT", nomeCompleto = "Quadro MT", tipoInterruttore = MqtSwitchType.SF6)
        )

        val serialized = json.encodeToString(impianti)
        val deserialized = json.decodeFromString<List<Impianto>>(serialized)

        assertEquals(6, deserialized.size)
        assertTrue(deserialized[0] is ImpiantoStandard)
        assertTrue(deserialized[1] is QuadroBT)
        assertTrue(deserialized[2] is ImpiantoEmergenza)
        assertTrue(deserialized[3] is RilevazioneAntincendio)
        assertTrue(deserialized[4] is RilevazioneGas)
        assertTrue(deserialized[5] is QuadroMQT)
        assertEquals(MqtSwitchType.SF6, (deserialized[5] as QuadroMQT).tipoInterruttore)
    }

    // =========== copyWithBasicParams ===========

    @Test
    fun `copyWithBasicParams preserva il tipo concreto`() {
        val quadro = QuadroBT(
            id = "q-1", codIntervento = "Q", nomeCompleto = "Quadro BT",
            sigla = "QG1", descrizioneQuadro = "Piano Terra"
        )
        val copia = quadro.copyWithBasicParams(nomeCompleto = "Quadro BT Aggiornato")

        assertTrue(copia is QuadroBT)
        assertEquals("Quadro BT Aggiornato", copia.nomeCompleto)
        // I campi specifici di QuadroBT devono essere conservati
        assertEquals("QG1", (copia as QuadroBT).sigla)
        assertEquals("Piano Terra", copia.descrizioneQuadro)
    }
}
