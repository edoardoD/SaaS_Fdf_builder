package manutenzioni.app.ui.features.operativita

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import manutenzioni.domain.model.Impianto
import manutenzioni.domain.model.InterruttoreBT
import manutenzioni.domain.model.QuadroBT
import java.util.UUID

data class QuadroDraft(
    val id: String = UUID.randomUUID().toString(),
    var sigla: String = "",
    var descrizione: String = "",
    var interruttori: List<InterruttoreBT> = emptyList()
)

@Composable
fun MassiveQuadroCreationDialog(
    cantiereId: String,
    templateQuadro: Impianto?,
    onDismiss: () -> Unit,
    onSave: (List<QuadroBT>) -> Unit,
    interruttoriDisponibili: List<InterruttoreBT> = emptyList()
) {
    val drafts = remember { mutableStateListOf(QuadroDraft()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiunta Rapida Quadri Elettrici") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                Text(
                    "Compila Sigla e Descrizione per i quadri che desideri aggiungere.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(drafts) { index, draft ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = draft.sigla,
                                onValueChange = { drafts[index] = draft.copy(sigla = it) },
                                label = { Text("Sigla *") },
                                modifier = Modifier.weight(0.3f),
                                singleLine = true
                            )
                            
                            OutlinedTextField(
                                value = draft.descrizione,
                                onValueChange = { drafts[index] = draft.copy(descrizione = it) },
                                label = { Text("Descrizione Quadro") },
                                modifier = Modifier.weight(0.4f),
                                singleLine = true
                            )

                            // Dropdown per aggiungere componenti
                            var expanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(0.2f)) {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                ) {
                                    Text("${draft.interruttori.size} Comp.")
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    if (interruttoriDisponibili.isEmpty()) {
                                        DropdownMenuItem(onClick = { expanded = false }) {
                                            Text("Nessun componente standard trovato")
                                        }
                                    } else {
                                        interruttoriDisponibili.forEach { interruttore ->
                                            DropdownMenuItem(
                                                onClick = {
                                                    val newList = draft.interruttori.toMutableList()
                                                    newList.add(interruttore.copy(id = UUID.randomUUID().toString()))
                                                    drafts[index] = draft.copy(interruttori = newList)
                                                    expanded = false
                                                }
                                            ) {
                                                Text("+ ${interruttore.nome}")
                                            }
                                        }
                                    }
                                }
                            }

                            IconButton(
                                onClick = { drafts.removeAt(index) },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Rimuovi riga", tint = MaterialTheme.colors.error)
                            }
                        }
                    }
                    
                    item {
                        TextButton(
                            onClick = { drafts.add(QuadroDraft()) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Aggiungi riga")
                            Spacer(Modifier.width(4.dp))
                            Text("Aggiungi un altro quadro")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validDrafts = drafts.filter { it.sigla.isNotBlank() }
                    val quadriCreati = validDrafts.map { d ->
                        QuadroBT(
                            cantiereId = cantiereId,
                            codIntervento = templateQuadro?.codIntervento ?: "Q",
                            nomeCompleto = templateQuadro?.nomeCompleto ?: "Quadri Elettrici BT",
                            sigla = d.sigla,
                            descrizioneQuadro = d.descrizione,
                            listaInterruttori = d.interruttori,
                            listaAttivita = templateQuadro?.listaAttivita ?: emptyList(),
                            listaNormative = templateQuadro?.listaNormative ?: emptyList(),
                            premessa = templateQuadro?.premessa
                        )
                    }
                    onSave(quadriCreati)
                }
            ) {
                Text("Salva Quadri")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        },
        modifier = Modifier.fillMaxWidth(0.9f)
    )
}
