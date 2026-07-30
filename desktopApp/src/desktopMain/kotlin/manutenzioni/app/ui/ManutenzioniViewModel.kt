package manutenzioni.app.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import manutenzioni.app.data.Cantiere
import manutenzioni.app.data.Cliente
import manutenzioni.app.data.Impianto
import manutenzioni.app.data.Periodo
import manutenzioni.app.strategy.HtmlToPdfStrategy
import manutenzioni.domain.ManutenzioneRepository
import manutenzioni.domain.service.FrequencyFilter
import manutenzioni.domain.strategy.PdfBatchGenerator
import java.io.File
import javax.swing.JFileChooser

/** Modalità di visualizzazione dell'area principale */
enum class ViewMode {
    PDF_PREVIEW,
    IMPIANTO_EDITOR
}

/** Sezioni principali dell'applicazione */
enum class AppSection {
    SETUP,
    OPERATIVO
}

/**
 * Stato immutabile dell'interfaccia utente.
 * Ogni cambiamento genera una nuova istanza (unidirectional data flow).
 */
data class ManutenzioniUiState(
    val impianti: List<Impianto> = emptyList(),
    val selectedImpianto: Impianto? = null,
    val frequenzeDisponibili: List<Periodo> = emptyList(),
    val selectedFrequenza: Periodo? = null,
    val clienti: List<Cliente> = emptyList(),
    val selectedCliente: Cliente? = null,
    val cantieriDisponibili: List<Cantiere> = emptyList(),
    val selectedCantiere: Cantiere? = null,
    val impiantiDelCantiere: List<Impianto> = emptyList(),
    val impiantiSelezionati: Map<String, Boolean> = emptyMap(),
    val pdfFile: File? = null,
    val isLoading: Boolean = false,
    val statusMessage: String = "Seleziona un cliente per iniziare",
    val errorMessage: String? = null,
    val viewMode: ViewMode = ViewMode.PDF_PREVIEW,
    val currentSection: AppSection = AppSection.OPERATIVO,
    /** Progresso batch: "Generazione copia X di N..." (null se non in corso) */
    val batchProgress: String? = null,
    /** Lista dei file generati nell'ultimo batch */
    val generatedFiles: List<File> = emptyList()
)

/**
 * ViewModel che gestisce lo stato dell'applicazione.
 *
 * Utilizza StateFlow (Observer Pattern) per propagare gli aggiornamenti
 * alla UI Compose in modo reattivo.
 */
class ManutenzioniViewModel(
    private val repository: ManutenzioneRepository,
    private val pdfStrategy: PdfBatchGenerator = HtmlToPdfStrategy()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(ManutenzioniUiState())
    val uiState: StateFlow<ManutenzioniUiState> = _uiState.asStateFlow()

    init {
        loadImpianti()
        loadClienti()
    }

    /** Carica gli impianti dal repository */
    private fun loadImpianti() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val impianti = repository.caricaImpianti()
                _uiState.update {
                    it.copy(
                        impianti = impianti,
                        isLoading = false,
                        statusMessage = "${impianti.size} impianti caricati"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Errore caricamento: ${e.message}"
                    )
                }
            }
        }
    }

    /** Carica i clienti dal repository */
    private fun loadClienti() {
        scope.launch {
            try {
                val clienti = repository.caricaClienti()
                _uiState.update { it.copy(clienti = clienti) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Errore caricamento clienti: ${e.message}")
                }
            }
        }
    }

    /** Seleziona un cliente esistente */
    fun selectCliente(cliente: Cliente) {
        _uiState.update {
            it.copy(
                selectedCliente = cliente,
                selectedCantiere = null,
                cantieriDisponibili = emptyList(),
                impiantiDelCantiere = emptyList(),
                impiantiSelezionati = emptyMap(),
                statusMessage = "Cliente: ${cliente.nome}. Ora seleziona un cantiere.",
                errorMessage = null
            )
        }
        loadCantieriForCliente(cliente.id)
    }

    private fun loadCantieriForCliente(clienteId: String) {
        scope.launch {
            try {
                val cantieri = repository.getCantieriForCliente(clienteId)
                _uiState.update { it.copy(cantieriDisponibili = cantieri) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Errore caricamento cantieri: ${e.message}")
                }
            }
        }
    }

    /** Aggiunge un nuovo cliente e lo seleziona automaticamente */
    fun addCliente(cliente: Cliente) {
        scope.launch {
            try {
                repository.salvaCliente(cliente)
                val clienti = repository.caricaClienti()
                _uiState.update {
                    it.copy(
                        clienti = clienti,
                        selectedCliente = cliente,
                        statusMessage = "✓ Cliente ${cliente.nome} aggiunto e selezionato",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Errore salvataggio cliente: ${e.message}")
                }
            }
        }
    }

    fun addCantiere(cantiere: Cantiere) {
        scope.launch {
            try {
                repository.salvaCantiere(cantiere)
                val cantieri = repository.getCantieriForCliente(cantiere.clienteId)
                _uiState.update {
                    it.copy(
                        cantieriDisponibili = cantieri,
                        selectedCantiere = cantiere,
                        statusMessage = "✓ Cantiere ${cantiere.nome} aggiunto",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Errore salvataggio cantiere: ${e.message}")
                }
            }
        }
    }

    fun selectCantiere(cantiere: Cantiere) {
        _uiState.update {
            it.copy(
                selectedCantiere = cantiere,
                impiantiDelCantiere = emptyList(),
                impiantiSelezionati = emptyMap(),
                statusMessage = "Cantiere: ${cantiere.nome}. Seleziona gli impianti.",
                errorMessage = null
            )
        }
        loadImpiantiForCantiere(cantiere.id)
    }

    private fun loadImpiantiForCantiere(cantiereId: String) {
        scope.launch {
            try {
                val impianti = repository.getImpiantiForCantiere(cantiereId)
                _uiState.update { it.copy(impiantiDelCantiere = impianti) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Errore caricamento impianti: ${e.message}")
                }
            }
        }
    }

    /** Seleziona un impianto e calcola le frequenze disponibili */
    fun selectImpianto(impianto: Impianto) {
        val frequenze = FrequencyFilter.frequenzeDisponibili(impianto.listaAttivita)
        _uiState.update {
            it.copy(
                selectedImpianto = impianto,
                frequenzeDisponibili = frequenze,
                selectedFrequenza = null,
                pdfFile = null,
                statusMessage = "${impianto.nomeCompleto} — seleziona una frequenza",
                errorMessage = null
            )
        }
    }

    fun toggleImpiantoSelection(impiantoId: String, isSelected: Boolean) {
        val updatedSelection = _uiState.value.impiantiSelezionati.toMutableMap()
        if (isSelected) {
            updatedSelection[impiantoId] = true
        } else {
            updatedSelection.remove(impiantoId)
        }
        _uiState.update { it.copy(impiantiSelezionati = updatedSelection) }
    }

    /** Seleziona una frequenza e genera automaticamente il PDF */
    fun selectFrequenza(frequenza: Periodo) {
        _uiState.update { it.copy(selectedFrequenza = frequenza) }
    }

    /** Cambia la sezione corrente dell'app */
    fun setSection(section: AppSection) {
        _uiState.update { it.copy(currentSection = section) }
    }

    /** Seleziona una cartella di output in modo cross-platform (macOS: FileDialog, altri: JFileChooser) */
    private fun selectOutputDirectoryCompatibile(): File? {
        return try {
            val os = System.getProperty("os.name").lowercase()
            if (os.contains("mac")) {
                System.setProperty("apple.awt.fileDialogForDirectories", "true")
                val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Seleziona o crea una cartella", java.awt.FileDialog.LOAD)
                dialog.isVisible = true
                val selectedDir = dialog.directory
                val selectedFile = dialog.file
                System.setProperty("apple.awt.fileDialogForDirectories", "false")
                if (selectedDir == null || selectedFile == null) return null
                val dir = File(selectedDir, selectedFile)
                if (!dir.exists()) dir.mkdirs()
                if (dir.isDirectory) dir else null
            } else {
                val chooser = JFileChooser().apply {
                    dialogTitle = "Seleziona o crea la cartella di destinazione"
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    approveButtonText = "Salva qui"
                }
                val result = chooser.showOpenDialog(null)
                if (result != JFileChooser.APPROVE_OPTION) return null
                val dir = chooser.selectedFile
                if (!dir.exists()) dir.mkdirs()
                if (dir.isDirectory) dir else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Genera il PDF con la strategia corrente — usa sempre il flusso batch */
    fun generatePdf() {
        val state = _uiState.value
        val impiantiSelezionatiIds = state.impiantiSelezionati.filter { it.value }.keys
        if (impiantiSelezionatiIds.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Nessun impianto selezionato.") }
            return
        }
        val frequenza = state.selectedFrequenza ?: return

        val outputDir = selectOutputDirectoryCompatibile() ?: return

        scope.launch {
            val impiantiDaGenerare = state.impiantiDelCantiere.filter { it.codIntervento in impiantiSelezionatiIds }

            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    batchProgress = "Avvio generazione...",
                    statusMessage = "Generazione di ${impiantiDaGenerare.size} PDF in corso...",
                    generatedFiles = emptyList()
                )
            }

            val allGeneratedFiles = mutableListOf<File>()
            var totalSuccessCount = 0
            val totalErrors = mutableMapOf<String, String>()

            for ((index, impianto) in impiantiDaGenerare.withIndex()) {
                try {
                    val batchResult = withContext(Dispatchers.IO) {
                        pdfStrategy.generateBatch(
                            impianto = impianto,
                            frequenza = frequenza,
                            outputDir = outputDir,
                            copies = impianto.quantita,
                            clienteNome = state.selectedCliente?.nome,
                            onProgress = { current, total ->
                                _uiState.update {
                                    it.copy(
                                        batchProgress = "Impianto ${index + 1}/${impiantiDaGenerare.size}: PDF $current di $total...",
                                        statusMessage = "Generazione ${impianto.codIntervento}..."
                                    )
                                }
                            }
                        )
                    }
                    allGeneratedFiles.addAll(batchResult.generatedFiles)
                    totalSuccessCount += batchResult.successCount
                    if (!batchResult.isFullSuccess) {
                        batchResult.errors.forEach { (copy, error) ->
                            totalErrors["${impianto.codIntervento} (copia $copy)"] = error
                        }
                    }
                } catch (e: Exception) {
                    totalErrors[impianto.codIntervento] = e.message ?: "Errore sconosciuto"
                }
            }

            val statusMsg = "✓ $totalSuccessCount PDF generati. ${totalErrors.size} errori."
            val errorMsg = if (totalErrors.isNotEmpty()) {
                "Errori: " + totalErrors.entries.joinToString("; ") { "${it.key}: ${it.value}" }
            } else null

                _uiState.update {
                    it.copy(
                        pdfFile = allGeneratedFiles.firstOrNull(),
                        generatedFiles = allGeneratedFiles,
                        isLoading = false,
                        batchProgress = null,
                        statusMessage = statusMsg,
                        errorMessage = errorMsg,
                        viewMode = ViewMode.PDF_PREVIEW
                    )
                }
        }
    }

    /** Cambia la modalità di visualizzazione */
    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    /** Apre il PDF nel viewer di sistema */
    fun openPdfInSystem() {
        val file = _uiState.value.pdfFile ?: return
        try {
            val desktop = java.awt.Desktop.getDesktop()
            desktop.open(file)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = "Impossibile aprire il PDF: ${e.message}")
            }
        }
    }

    /**
     * Crea un nuovo impianto vuoto, lo seleziona e apre l'editor.
     * L'utente potrà compilare codice, nome, premessa e attività dall'editor.
     */
    fun createNewImpianto(template: Impianto? = null) {
        val newImpianto = template?.copy(
            cantiereId = _uiState.value.selectedCantiere?.id,
            quantita = 1
        ) ?: Impianto(
            codIntervento = "",
            nomeCompleto = "",
            premessa = null,
            listaAttivita = emptyList(),
            listaNormative = emptyList(),
            quantita = 1
        )
        _uiState.update {
            it.copy(
                selectedImpianto = newImpianto,
                frequenzeDisponibili = emptyList(),
                selectedFrequenza = null,
                pdfFile = null,
                viewMode = ViewMode.IMPIANTO_EDITOR,
                statusMessage = "Nuovo impianto — compila i dati e salva",
                errorMessage = null
            )
        }
    }

    /** Salva un impianto (nuovo o modificato) e aggiorna la lista */
    fun saveImpianto(impianto: Impianto) {
        // Validazione campi obbligatori
        if (impianto.codIntervento.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Il codice intervento è obbligatorio")
            }
            return
        }
        if (impianto.nomeCompleto.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Il nome completo è obbligatorio")
            }
            return
        }

        scope.launch {
            try {
                repository.salvaImpianto(impianto)
                val impiantiAggiornati = repository.caricaImpianti()
                val frequenze = FrequencyFilter.frequenzeDisponibili(impianto.listaAttivita)
                _uiState.update {
                    it.copy(
                        impianti = impiantiAggiornati,
                        selectedImpianto = impianto,
                        frequenzeDisponibili = frequenze,
                        statusMessage = "✓ Impianto ${impianto.codIntervento} salvato",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Errore salvataggio: ${e.message}")
                }
            }
        }
    }

    /** Ricarica i dati dal repository */
    fun refresh() {
        loadImpianti()
        loadClienti()
        _uiState.value.selectedCliente?.let {
            loadCantieriForCliente(it.id)
        }
    }
}
