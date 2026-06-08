package com.example.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.desktop.pdf.DesktopPdfService
import com.example.domain.Cantiere
import com.example.domain.Cliente
import com.example.domain.MockDatabase
import java.io.File

enum class GeneratorMode {
    MANUAL,
    BY_CANTIERE
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "FDF Builder") {
        var status by remember { mutableStateOf("Pronto per generare PDF") }
        var currentMode by remember { mutableStateOf(GeneratorMode.MANUAL) }
        val pdfService = DesktopPdfService()

        MaterialTheme {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // TABS
                TabRow(selectedTabIndex = currentMode.ordinal) {
                    Tab(
                        selected = currentMode == GeneratorMode.MANUAL,
                        onClick = { currentMode = GeneratorMode.MANUAL },
                        text = { Text("Generazione Manuale") }
                    )
                    Tab(
                        selected = currentMode == GeneratorMode.BY_CANTIERE,
                        onClick = { currentMode = GeneratorMode.BY_CANTIERE },
                        text = { Text("Ricerca per Cantiere") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CONTENT
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentMode) {
                        GeneratorMode.MANUAL -> ManualGenerationView(
                            pdfService = pdfService,
                            onStatusChange = { status = it }
                        )
                        GeneratorMode.BY_CANTIERE -> CantiereGenerationView(
                            pdfService = pdfService,
                            onStatusChange = { status = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // STATUS BAR
                Card(modifier = Modifier.fillMaxWidth(), elevation = 4.dp) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }
}

@Composable
fun ManualGenerationView(pdfService: DesktopPdfService, onStatusChange: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Modalità Manuale Esistente")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            try {
                val home = System.getProperty("user.home")
                val path = "$home/FleetDemo/sample.pdf"
                pdfService.sampleCreatePdf(path)
                onStatusChange("PDF generato manualmente in: $path")
            } catch (e: Exception) {
                onStatusChange("Errore: ${e.message}")
                e.printStackTrace()
            }
        }) {
            Text("Genera PDF (Singolo)")
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CantiereGenerationView(pdfService: DesktopPdfService, onStatusChange: (String) -> Unit) {
    var selectedCliente by remember { mutableStateOf<Cliente?>(null) }
    var selectedCantiere by remember { mutableStateOf<Cantiere?>(null) }
    var selectedFrequenza by remember { mutableStateOf("Semestrale") }

    var clienteExpanded by remember { mutableStateOf(false) }
    var cantiereExpanded by remember { mutableStateOf(false) }
    var frequenzaExpanded by remember { mutableStateOf(false) }

    val frequenzeList = listOf("Semestrale", "Annuale")

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Generazione Multipla per Cantiere", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(24.dp))

        // SELEZIONE CLIENTE
        Text("Cliente:")
        ExposedDropdownMenuBox(
            expanded = clienteExpanded,
            onExpandedChange = { clienteExpanded = !clienteExpanded }
        ) {
            OutlinedTextField(
                value = selectedCliente?.nome ?: "Seleziona Cliente",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clienteExpanded) },
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = clienteExpanded,
                onDismissRequest = { clienteExpanded = false }
            ) {
                MockDatabase.clienti.forEach { cliente ->
                    DropdownMenuItem(onClick = {
                        selectedCliente = cliente
                        selectedCantiere = null // reset
                        clienteExpanded = false
                    }) {
                        Text(cliente.nome)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SELEZIONE CANTIERE
        Text("Cantiere:")
        ExposedDropdownMenuBox(
            expanded = cantiereExpanded,
            onExpandedChange = {
                if (selectedCliente != null) cantiereExpanded = !cantiereExpanded
            }
        ) {
            OutlinedTextField(
                value = selectedCantiere?.nome ?: "Seleziona Cantiere",
                onValueChange = {},
                readOnly = true,
                enabled = selectedCliente != null,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cantiereExpanded) },
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = cantiereExpanded,
                onDismissRequest = { cantiereExpanded = false }
            ) {
                selectedCliente?.cantieri?.forEach { cantiere ->
                    DropdownMenuItem(onClick = {
                        selectedCantiere = cantiere
                        cantiereExpanded = false
                    }) {
                        Text(cantiere.nome)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SELEZIONE FREQUENZA
        Text("Frequenza Manutenzione:")
        ExposedDropdownMenuBox(
            expanded = frequenzaExpanded,
            onExpandedChange = { frequenzaExpanded = !frequenzaExpanded }
        ) {
            OutlinedTextField(
                value = selectedFrequenza,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequenzaExpanded) },
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = frequenzaExpanded,
                onDismissRequest = { frequenzaExpanded = false }
            ) {
                frequenzeList.forEach { freq ->
                    DropdownMenuItem(onClick = {
                        selectedFrequenza = freq
                        frequenzaExpanded = false
                    }) {
                        Text(freq)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            enabled = selectedCliente != null && selectedCantiere != null,
            onClick = {
                try {
                    val home = System.getProperty("user.home")
                    val basePath = "$home/FleetDemo/Cantieri/${selectedCantiere!!.nome}"

                    val generatedFiles = pdfService.generatePdfsForCantiere(
                        cantiere = selectedCantiere!!,
                        frequenza = selectedFrequenza,
                        basePath = basePath
                    )

                    if (generatedFiles.isEmpty()) {
                        onStatusChange("Nessun apparato trovato per ${selectedCantiere!!.nome} con frequenza $selectedFrequenza.")
                    } else {
                        onStatusChange("Generati ${generatedFiles.size} PDF in:\n$basePath")
                    }
                } catch (e: Exception) {
                    onStatusChange("Errore: ${e.message}")
                    e.printStackTrace()
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Genera PDF Cantiere")
        }
    }
}
