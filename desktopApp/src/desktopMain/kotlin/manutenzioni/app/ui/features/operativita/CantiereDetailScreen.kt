package manutenzioni.app.ui.features.operativita

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import manutenzioni.domain.model.Impianto
import manutenzioni.domain.model.Periodo
import manutenzioni.app.ui.ImpiantoEditor
import manutenzioni.app.ui.ImpiantoSelectionList
import manutenzioni.app.ui.ManutenzioniUiState
import manutenzioni.app.ui.theme.SlateBorder

@Composable
fun CantiereDetailScreen(
    state: ManutenzioniUiState,
    onBack: () -> Unit,
    onImpiantoSelectionChanged: (String, Boolean) -> Unit,
    onEditImpianto: (Impianto) -> Unit,
    onSaveImpianto: (Impianto) -> Unit,
    onFrequenzaSelected: (Periodo) -> Unit,
    onGeneraPdf: () -> Unit,
    onOpenPdf: () -> Unit,
    onCreateNewImpianto: (Impianto?, Int) -> Unit,
    onDeleteImpianto: (String) -> Unit
) {
    // Gestione visualizzazione ImpiantoEditor
    var impiantoInModifica by remember { mutableStateOf<Impianto?>(null) }
    var showNuovoImpiantoDialog by remember { mutableStateOf(false) }
    var showDeleteImpiantoDialog by remember { mutableStateOf(false) }
    var impiantoInEliminazione by remember { mutableStateOf<Impianto?>(null) }

    if (impiantoInModifica != null) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Card(elevation = 8.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxSize()) {
                ImpiantoEditor(
                    impianto = impiantoInModifica!!,
                    isReadOnlyAdminFields = true,
                    onSave = { impianto, _ -> 
                        onSaveImpianto(impianto)
                        impiantoInModifica = null
                    },
                    onCancel = {
                        impiantoInModifica = null
                    }
                )
            }
        }
        return // Non disegniamo il resto se siamo nell'editor
    }

    if (showNuovoImpiantoDialog) {
        NuovoImpiantoDialog(
            impiantiGlobali = state.impiantiGlobali,
            onDismiss = { showNuovoImpiantoDialog = false },
            onConfirm = { template, quantita ->
                onCreateNewImpianto(template, quantita)
                showNuovoImpiantoDialog = false
            }
        )
    }

    if (showDeleteImpiantoDialog && impiantoInEliminazione != null) {
        AlertDialog(
            onDismissRequest = { showDeleteImpiantoDialog = false },
            title = { Text("Elimina Impianto") },
            text = {
                Text(
                    "Sei sicuro di voler eliminare l'impianto '${impiantoInEliminazione!!.nomeCompleto}'?\n\n" +
                    "Questa operazione lo eliminerà definitivamente dal database. L'azione non è reversibile."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteImpianto(impiantoInEliminazione!!.id)
                        showDeleteImpiantoDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error, contentColor = MaterialTheme.colors.onError)
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteImpiantoDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Cantiere: ${state.selectedCantiere?.nome}",
                    style = MaterialTheme.typography.h5,
                    color = MaterialTheme.colors.onBackground
                )
                Text(
                    text = "Seleziona gli impianti per la manutenzione",
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.secondary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { showNuovoImpiantoDialog = true },
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary, contentColor = Color.White)
            ) {
                Text("+ Aggiungi Impianto")
            }
        }

        Divider(color = SlateBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // Colonna 1: Lista Impianti
            Box(modifier = Modifier.weight(2f).fillMaxHeight()) {
                ImpiantoSelectionList(
                    impianti = state.impiantiDelCantiere,
                    selectionState = state.impiantiSelezionati,
                    onSelectionChanged = onImpiantoSelectionChanged,
                    onEditImpianto = { impianto -> 
                        impiantoInModifica = impianto 
                        onEditImpianto(impianto)
                    },
                    onDeleteImpianto = { impianto ->
                        impiantoInEliminazione = impianto
                        showDeleteImpiantoDialog = true
                    },
                    onQuantitaChanged = { impianto, nuovaQ ->
                        onSaveImpianto(impianto.copy(quantita = nuovaQ))
                    }
                )
            }

            // Colonna 2: Frequenza e Generazione
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 0.dp,
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("1. Scegli Frequenza", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        state.frequenzeDisponibili.forEach { periodo ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = state.selectedFrequenza == periodo,
                                    onClick = { onFrequenzaSelected(periodo) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colors.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = periodo.label(), style = MaterialTheme.typography.body2)
                            }
                        }
                        
                        if (state.frequenzeDisponibili.isEmpty()) {
                            Text("Nessuna frequenza disponibile. Seleziona almeno un impianto.", color = MaterialTheme.colors.error, style = MaterialTheme.typography.body2)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 0.dp,
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("2. Generazione PDF", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val isEnabled = state.selectedFrequenza != null && state.impiantiSelezionati.any { it.value }
                        Button(
                            onClick = onGeneraPdf,
                            enabled = isEnabled && !state.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary, contentColor = Color.White)
                        ) {
                            Text("Genera PDF", fontWeight = FontWeight.Bold)
                        }
                        
                        if (state.pdfFile != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = onOpenPdf,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.primary)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Apri PDF Creato")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Percorso: ${state.pdfFile.name}", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.secondary)
                        }
                    }
                }
            }
        }
    }
}
