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
    onConfirm: (Impianto?) -> Unit,
    onMassiveQuadroRequested: (Impianto) -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi Nuovo Impianto") },
        text = {
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
                                            onConfirm(template)
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
                    onClick = { onConfirm(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Crea impianto vuoto")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
