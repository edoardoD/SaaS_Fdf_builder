package manutenzioni.app.ui.features.amministrazione

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.UUID
import manutenzioni.domain.model.Cliente
import manutenzioni.app.ui.theme.SlateBorder

@Composable
fun AdminClientiTab(
    clienti: List<Cliente>,
    onAddCliente: (Cliente) -> Unit,
    onRenameCliente: (String, String) -> Unit,
    onDeleteCliente: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var clienteInModifica by remember { mutableStateOf<Cliente?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var clienteInEliminazione by remember { mutableStateOf<Cliente?>(null) }

    if (showDialog) {
        ClienteDialog(
            initialCliente = null,
            onDismiss = { showDialog = false },
            onConfirm = { cliente ->
                onAddCliente(cliente)
                showDialog = false
            }
        )
    }

    if (showRenameDialog && clienteInModifica != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rinomina Cliente") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Nome Cliente") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameText.isNotBlank()) {
                        onRenameCliente(clienteInModifica!!.id, renameText)
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

    if (showDeleteConfirmDialog && clienteInEliminazione != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Elimina Cliente") },
            text = {
                Text(
                    "Sei sicuro di voler eliminare il cliente '${clienteInEliminazione!!.nome}'?\n\n" +
                    "Questa operazione eliminerà definitivamente a cascata tutti i suoi cantieri e impianti associati. L'azione non è reversibile."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCliente(clienteInEliminazione!!.id)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error, contentColor = MaterialTheme.colors.onError)
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
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
            Text("Gestione Clienti", style = MaterialTheme.typography.h6)
            Button(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nuovo Cliente")
            }
        }

        if (clienti.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessun cliente inserito.", color = MaterialTheme.colors.secondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(clienti) { cliente ->
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
                            Text(cliente.nome, style = MaterialTheme.typography.body1)
                            Row {
                                IconButton(onClick = {
                                    clienteInModifica = cliente
                                    renameText = cliente.nome
                                    showRenameDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = MaterialTheme.colors.primary)
                                }
                                IconButton(onClick = {
                                    clienteInEliminazione = cliente
                                    showDeleteConfirmDialog = true
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colors.error)
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
fun ClienteDialog(
    initialCliente: Cliente? = null,
    onDismiss: () -> Unit,
    onConfirm: (Cliente) -> Unit
) {
    var nome by remember { mutableStateOf(initialCliente?.nome ?: "") }
    var indirizzo by remember { mutableStateOf(initialCliente?.indirizzo ?: "") }
    var partitaIva by remember { mutableStateOf(initialCliente?.partitaIva ?: "") }
    var nomeError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCliente == null) "Nuovo Cliente" else "Modifica Cliente") },
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
                OutlinedTextField(
                    value = indirizzo,
                    onValueChange = { indirizzo = it },
                    label = { Text("Indirizzo (Opzionale)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = partitaIva,
                    onValueChange = { partitaIva = it },
                    label = { Text("Partita IVA (Opzionale)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nome.isBlank()) {
                    nomeError = true
                } else {
                    val cliente = initialCliente?.copy(
                        nome = nome,
                        indirizzo = indirizzo.takeIf { it.isNotBlank() },
                        partitaIva = partitaIva.takeIf { it.isNotBlank() }
                    ) ?: Cliente(
                        id = UUID.randomUUID().toString(),
                        nome = nome,
                        indirizzo = indirizzo.takeIf { it.isNotBlank() },
                        partitaIva = partitaIva.takeIf { it.isNotBlank() }
                    )
                    onConfirm(cliente)
                }
            }) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
