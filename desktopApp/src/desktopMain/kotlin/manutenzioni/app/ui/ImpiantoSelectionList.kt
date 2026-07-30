package manutenzioni.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import manutenzioni.app.data.Impianto

@Composable
fun ImpiantoSelectionList(
    impianti: List<Impianto>,
    selectionState: Map<String, Boolean>,
    onSelectionChanged: (String, Boolean) -> Unit,
    onEditImpianto: (Impianto) -> Unit,
    modifier: Modifier = Modifier
) {
    if (impianti.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Nessun impianto trovato per questo cantiere.", color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
        }
        return
    }

    Column(modifier = modifier) {
        Text(
            text = "Seleziona Impianti da Includere",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(impianti, key = { it.codIntervento }) { impianto ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isSelected = selectionState[impianto.codIntervento] == true
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                onSelectionChanged(impianto.codIntervento, checked)
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = impianto.nomeCompleto,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Codice: ${impianto.codIntervento}",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        
                        IconButton(onClick = { onEditImpianto(impianto) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Modifica impianto",
                                tint = MaterialTheme.colors.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
