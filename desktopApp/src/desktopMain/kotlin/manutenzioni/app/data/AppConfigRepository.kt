package manutenzioni.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.Key
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@Serializable
data class DbConfig(
    val host: String = "localhost",
    val port: String = "27017",
    val dbName: String = "manutenzioni_db",
    val username: String = "",
    val encryptedPasswordBase64: String = ""
)

object AppConfigRepository {
    private val USER_HOME = System.getProperty("user.home")
    private val APP_DIR = File(USER_HOME, ".manutenzioni_maker")
    private val CONFIG_FILE = File(APP_DIR, "config.json")
    
    // A simple hardcoded key for obfuscating local passwords.
    // In a real high-security enterprise app, this should be derived from a master password or OS keystore.
    private val ALGORITHM = "AES"
    private val SECRET_KEY = "ManutenzioniM4k3r!K3y2026123456".toByteArray().copyOf(16) // 128 bit key

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    init {
        if (!APP_DIR.exists()) {
            APP_DIR.mkdirs()
        }
    }

    private fun getCipher(mode: Int): Cipher {
        val key: Key = SecretKeySpec(SECRET_KEY, ALGORITHM)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(mode, key)
        return cipher
    }

    fun encryptPassword(password: String): String {
        if (password.isBlank()) return ""
        return try {
            val encryptedBytes = getCipher(Cipher.ENCRYPT_MODE).doFinal(password.toByteArray())
            Base64.getEncoder().encodeToString(encryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun decryptPassword(encryptedBase64: String): String {
        if (encryptedBase64.isBlank()) return ""
        return try {
            val decryptedBytes = getCipher(Cipher.DECRYPT_MODE).doFinal(Base64.getDecoder().decode(encryptedBase64))
            String(decryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun saveConfig(config: DbConfig) {
        val jsonString = json.encodeToString(config)
        CONFIG_FILE.writeText(jsonString)
    }

    fun loadConfig(): DbConfig? {
        if (!CONFIG_FILE.exists()) return null
        return try {
            json.decodeFromString<DbConfig>(CONFIG_FILE.readText())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun buildConnectionString(config: DbConfig): String {
        val authPart = if (config.username.isNotBlank()) {
            val pass = decryptPassword(config.encryptedPasswordBase64)
            if (pass.isNotBlank()) {
                "${config.username}:${pass}@"
            } else {
                "${config.username}@"
            }
        } else {
            ""
        }
        return "mongodb://$authPart${config.host}:${config.port}/?authSource=admin"
    }
}
