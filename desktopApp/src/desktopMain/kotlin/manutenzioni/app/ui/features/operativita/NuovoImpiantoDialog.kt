package manutenzioni.app.ui.features.operativita

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import manutenzioni.domain.model.Impianto

@Composable
fun NuovoImpiantoDialog(
    impiantiGlobali: List<Impianto>,
    onDismiss: () -> Unit,
    onConfirm: (Impianto?, Int) -> Unit,
    onMassiveQuadroRequested: (Impianto) -> Unit = {}
) {
    var step by remember { mutableStateOf(1) }
    var selectedTemplate by remember { mutableStateOf<Impianto?>(null) }
    var quantitaStr by remember { mutableStateOf("1") }
    var isQuantitaError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 1) "Aggiungi Nuovo Impianto" else "Quantità Impianti") },
        text = {
            if (step == 1) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Seleziona un modello dalla libreria globale, oppure creane uno vuoto:")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (impiantiGlobali.isEmpty()) {
                        Text("Nessun modello globale disponibile.", color = MaterialTheme.colors.secondary)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(impiantiGlobali) { template ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { 
                                            if (template is manutenzioni.domain.model.QuadroBT || template.codIntervento == "Q") {
                                                onMassiveQuadroRequested(template)
                                                onDismiss()
                                            } else {
                                                selectedTemplate = template
                                                step = 2 
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors()
                                    ) {
                                        Text("${template.codIntervento} - ${template.nomeCompleto}")
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { 
                            selectedTemplate = null
                            step = 2 
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Crea impianto vuoto")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Quanti impianti di questo tipo sono presenti nel cantiere?")
                    OutlinedTextField(
                        value = quantitaStr,
                        onValueChange = { 
                            quantitaStr = it 
                            isQuantitaError = it.toIntOrNull() == null || it.toInt() <= 0
                        },
                        label = { Text("Quantità *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = isQuantitaError
                    )
                    if (isQuantitaError) {
                        Text("Inserisci un numero intero positivo", color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
                    }
                }
            }
        },
        confirmButton = {
            if (step == 2) {
                Button(
                    onClick = {
                        if (!isQuantitaError && quantitaStr.isNotBlank()) {
                            onConfirm(selectedTemplate, quantitaStr.toInt())
                        } else {
                            isQuantitaError = true
                        }
                    }
                ) {
                    Text("Conferma")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { 
                if (step == 2) {
                    step = 1 
                } else {
                    onDismiss()
                }
            }) {
                Text(if (step == 2) "Indietro" else "Annulla")
            }
        }
    )
}
