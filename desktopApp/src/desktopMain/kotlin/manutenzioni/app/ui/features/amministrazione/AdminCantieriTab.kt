package manutenzioni.app.ui.features.amministrazione

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.UUID
import manutenzioni.domain.model.Cantiere
import manutenzioni.domain.model.Cliente
import manutenzioni.app.ui.features.operativita.ClienteSelector
import manutenzioni.app.ui.theme.SlateBorder

@Composable
fun AdminCantieriTab(
    clienti: List<Cliente>,
    selectedCliente: Cliente?,
    cantieri: List<Cantiere>,
    onClienteSelected: (Cliente) -> Unit,
    onAddCantiere: (Cantiere) -> Unit,
    onRenameCantiere: (String, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var cantiereInModifica by remember { mutableStateOf<Cantiere?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showDialog && selectedCliente != null) {
        NuovoCantiereDialog(
            clienteId = selectedCliente.id,
            onDismiss = { showDialog = false },
            onConfirm = { cantiere ->
                onAddCantiere(cantiere)
                showDialog = false
            }
        )
    }

    if (showRenameDialog && cantiereInModifica != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rinomina Cantiere") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Nome Cantiere") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameText.isNotBlank()) {
                        onRenameCantiere(cantiereInModifica!!.id, renameText)
                        showRenameDialog = false
                    }
                }) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Gestione Cantieri", style = MaterialTheme.typography.h6)
                Text("Seleziona un cliente per visualizzarne i cantieri", style = MaterialTheme.typography.caption)
            }
            
            ClienteSelector(
                clienti = clienti,
                selectedCliente = selectedCliente,
                onClienteSelected = onClienteSelected
            )
        }

        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedCliente == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessun cliente selezionato.", color = MaterialTheme.colors.secondary)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nuovo Cantiere")
                }
            }

            if (cantieri.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessun cantiere per questo cliente.", color = MaterialTheme.colors.secondary)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(cantieri) { cantiere ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 0.dp,
                            border = BorderStroke(1.dp, SlateBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cantiere.nome, style = MaterialTheme.typography.body1)
                                IconButton(onClick = {
                                    cantiereInModifica = cantiere
                                    renameText = cantiere.nome
                                    showRenameDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = MaterialTheme.colors.primary)
                                }
                            }
                        }
                    }
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
        title = { Text("Nuovo Cantiere") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it; nomeError = false },
                    label = { Text("Nome *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nomeError
                )
                if (nomeError) {
                    Text("Il nome è obbligatorio", color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nome.isBlank()) {
                    nomeError = true
                } else {
                    onConfirm(Cantiere(id = UUID.randomUUID().toString(), nome = nome, clienteId = clienteId))
                }
            }) {
                Text("Crea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
