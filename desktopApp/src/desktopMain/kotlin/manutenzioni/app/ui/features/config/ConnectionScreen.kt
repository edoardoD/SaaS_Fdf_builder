package manutenzioni.app.ui.features.config

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import manutenzioni.app.data.AppConfigRepository
import manutenzioni.app.data.DbConfig
import manutenzioni.app.data.MongoManutenzioneRepository
import java.util.UUID

@Composable
fun ConnectionScreen(
    initialConfig: DbConfig?,
    onConnected: (MongoManutenzioneRepository, DbConfig) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    var host by remember { mutableStateOf(initialConfig?.host ?: "localhost") }
    var port by remember { mutableStateOf(initialConfig?.port ?: "27017") }
    var dbName by remember { mutableStateOf(initialConfig?.dbName ?: "manutenzioni_db") }
    var username by remember { mutableStateOf(initialConfig?.username ?: "") }
    // Decrypt the initial password if it exists
    var password by remember { 
        mutableStateOf(
            if (initialConfig?.encryptedPasswordBase64?.isNotBlank() == true) {
                AppConfigRepository.decryptPassword(initialConfig.encryptedPasswordBase64)
            } else ""
        ) 
    }
    
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testIsSuccess by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                elevation = 4.dp,
                modifier = Modifier.width(500.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Configurazione Database",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    
                    Divider()

                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Host (es. localhost o indirizzo IP)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Porta") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dbName,
                            onValueChange = { dbName = it },
                            label = { Text("Nome Database") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Utente (opzionale)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (opzionale)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (testResult != null) {
                        Surface(
                            color = if (testIsSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text(
                                text = testResult!!,
                                color = if (testIsSuccess) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.body2
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp))
                        }
                        
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isTesting = true
                                    testResult = null
                                    
                                    val config = DbConfig(
                                        host = host,
                                        port = port,
                                        dbName = dbName,
                                        username = username,
                                        encryptedPasswordBase64 = AppConfigRepository.encryptPassword(password)
                                    )
                                    val connectionString = AppConfigRepository.buildConnectionString(config)
                                    
                                    try {
                                        val repo = withContext(Dispatchers.IO) {
                                            MongoManutenzioneRepository(connectionString, dbName)
                                        }
                                        // Try a simple operation to test
                                        withContext(Dispatchers.IO) {
                                            repo.caricaClienti() // Simple ping/fetch
                                        }
                                        testIsSuccess = true
                                        testResult = "Connessione stabilita con successo!"
                                    } catch (e: Exception) {
                                        testIsSuccess = false
                                        testResult = "Errore di connessione: \${e.message}"
                                    } finally {
                                        isTesting = false
                                    }
                                }
                            },
                            enabled = !isTesting
                        ) {
                            Text("Test Connessione")
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isTesting = true
                                    val config = DbConfig(
                                        host = host,
                                        port = port,
                                        dbName = dbName,
                                        username = username,
                                        encryptedPasswordBase64 = AppConfigRepository.encryptPassword(password)
                                    )
                                    val connectionString = AppConfigRepository.buildConnectionString(config)
                                    
                                    try {
                                        val repo = withContext(Dispatchers.IO) {
                                            MongoManutenzioneRepository(connectionString, dbName)
                                        }
                                        // Salva la configurazione
                                        withContext(Dispatchers.IO) {
                                            AppConfigRepository.saveConfig(config)
                                        }
                                        onConnected(repo, config)
                                    } catch (e: Exception) {
                                        testIsSuccess = false
                                        testResult = "Errore di connessione: \${e.message}"
                                    } finally {
                                        isTesting = false
                                    }
                                }
                            },
                            enabled = !isTesting
                        ) {
                            Text("Salva e Connetti")
                        }
                    }
                }
            }
        }
    }
}
