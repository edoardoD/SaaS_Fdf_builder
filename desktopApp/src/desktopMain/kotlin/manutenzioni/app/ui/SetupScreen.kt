package manutenzioni.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import manutenzioni.app.data.Cantiere
import manutenzioni.app.data.Cliente
import manutenzioni.app.data.Impianto

@Composable
fun SetupScreen(
    uiState: ManutenzioniUiState,
    viewModel: ManutenzioniViewModel,
    modifier: Modifier = Modifier
) {
    var showNuovoClienteDialog by remember { mutableStateOf(false) }
    var showNuovoCantiereDialog by remember { mutableStateOf(false) }
    var showNuovoImpiantoDialog by remember { mutableStateOf(false) }

    if (showNuovoClienteDialog) {
        NuovoClienteDialog(
            onDismiss = { showNuovoClienteDialog = false },
            onConfirm = { cliente ->
                viewModel.addCliente(cliente)
                showNuovoClienteDialog = false
            }
        )
    }

    if (showNuovoCantiereDialog && uiState.selectedCliente != null) {
        NuovoCantiereDialog(
            clienteId = uiState.selectedCliente.id,
            onDismiss = { showNuovoCantiereDialog = false },
            onConfirm = { cantiere ->
                viewModel.addCantiere(cantiere)
                showNuovoCantiereDialog = false
            }
        )
    }

    if (showNuovoImpiantoDialog) {
        val templates = uiState.impianti.distinctBy { it.codIntervento }
        NuovoImpiantoDialog(
            templates = templates,
            onDismiss = { showNuovoImpiantoDialog = false },
            onConfirm = { template ->
                viewModel.createNewImpianto(template)
                viewModel.setViewMode(ViewMode.IMPIANTO_EDITOR)
                showNuovoImpiantoDialog = false
            }
        )
    }

    Row(modifier = modifier) {
        // Pannello Sinistro: Selezione Cliente e Cantiere, lista impianti
        Column(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .fillMaxHeight()
                .background(Color(0xFFF0F4FA))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Struttura Dati",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )

            Divider()

            Text("1. Seleziona o crea Cliente", style = MaterialTheme.typography.subtitle2)
            ClienteDropdown(
                clienti = uiState.clienti,
                selected = uiState.selectedCliente,
                showError = false,
                onSelected = viewModel::selectCliente,
                onAddNew = { showNuovoClienteDialog = true }
            )

            Text("2. Seleziona o crea Cantiere", style = MaterialTheme.typography.subtitle2)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    CantiereDropdown(
                        cantieri = uiState.cantieriDisponibili,
                        selected = uiState.selectedCantiere,
                        enabled = uiState.selectedCliente != null,
                        onSelected = viewModel::selectCantiere
                    )
                }
                IconButton(
                    onClick = { showNuovoCantiereDialog = true },
                    enabled = uiState.selectedCliente != null
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuovo Cantiere", tint = MaterialTheme.colors.primary)
                }
            }

            Divider()

            if (uiState.selectedCantiere != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("3. Impianti in questo Cantiere", style = MaterialTheme.typography.subtitle2)
                    IconButton(onClick = { showNuovoImpiantoDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Nuovo Impianto", tint = MaterialTheme.colors.primary)
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.impiantiDelCantiere) { impianto ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(impianto.nomeCompleto, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Quantità: ${impianto.quantita}", fontSize = 11.sp, color = Color.Gray)
                                }
                                IconButton(onClick = { 
                                    viewModel.selectImpianto(impianto) 
                                    viewModel.setViewMode(ViewMode.IMPIANTO_EDITOR)
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Modifica", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Color(0xFFDDDDDD))

        // Pannello Destro: Editor Impianto
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (uiState.viewMode == ViewMode.IMPIANTO_EDITOR && uiState.selectedImpianto != null) {
                ImpiantoEditor(
                    impianto = uiState.selectedImpianto,
                    isReadOnlyAdminFields = false, // Permette la modifica della quantità e altri campi base
                    onSave = { updatedImpianto ->
                        // Assicura che l'impianto sia associato al cantiere selezionato
                        val impiantoConCantiere = updatedImpianto.copy(cantiereId = uiState.selectedCantiere?.id)
                        viewModel.saveImpianto(impiantoConCantiere)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Seleziona o crea un Impianto per gestirne i dettagli e le quantità", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun NuovoCantiereDialog(
    clienteId: String,
    onDismiss: () -> Unit,
    onConfirm: (Cantiere) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var nomeError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo Cantiere", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = nome,
                    onValueChange = {
                        nome = it
                        nomeError = false
                    },
                    label = { Text("Nome Cantiere *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nomeError
                )
                if (nomeError) {
                    Text("Il nome è obbligatorio", color = Color.Red, fontSize = 10.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nome.isBlank()) {
                    nomeError = true
                } else {
                    onConfirm(Cantiere(id = UUID.randomUUID().toString(), nome = nome.trim(), clienteId = clienteId))
                }
            }) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NuovoImpiantoDialog(
    templates: List<Impianto>,
    onDismiss: () -> Unit,
    onConfirm: (Impianto?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<Impianto?>(null) }
    
    val dropdownText = selectedTemplate?.let { "${it.codIntervento} - ${it.nomeCompleto}" } ?: "➕ Crea Impianto da zero"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo Impianto", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Seleziona un template esistente o crea da zero:")
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = dropdownText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            selectedTemplate = null
                            expanded = false
                        }) {
                            Text("➕ Crea Impianto da zero", fontWeight = FontWeight.Bold)
                        }
                        Divider()
                        templates.forEach { template ->
                            DropdownMenuItem(onClick = {
                                selectedTemplate = template
                                expanded = false
                            }) {
                                Text("${template.codIntervento} - ${template.nomeCompleto}")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedTemplate) }) {
                Text("Prosegui")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
