package manutenzioni.app.data

import kotlinx.serialization.json.Json
import manutenzioni.domain.ManutenzioneRepository
import java.io.File

/**
 * Implementazione concreta del repository basata su file JSON.
 * Al primo avvio, copia il database demo dalle resources nella working directory.
 * Tutte le operazioni CRUD persistono su file locale.
 */
class JsonManutenzioneRepository(
    fileName: String = "manutenzioni_db.json"
) : ManutenzioneRepository {

    private val dbFile: File = resolveDbPath(fileName)

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Cache in-memory del database */
    private var cache: MutableList<Impianto>? = null
    private var cacheClienti: MutableList<Cliente>? = null
    private var cacheCantieri: MutableList<Cantiere>? = null

    init {
        println("📂 Database log: \${dbFile.absolutePath}")
        copyDefaultIfMissing()
    }

    /**
     * Risolve il percorso del database in modo che sia scrivibile su ogni SO.
     * Su Desktop usa la cartella home dell'utente.
     */
    private fun resolveDbPath(fileName: String): File {
        val userHome = System.getProperty("user.home")
        val appDataDir = File(userHome, ".manutenzioni-maker")
        if (!appDataDir.exists()) {
            appDataDir.mkdirs()
        }
        return File(appDataDir, fileName)
    }

    /**
     * Se il file JSON locale non esiste, copia quello di default dalle resources.
     */
    private fun copyDefaultIfMissing() {
        if (!dbFile.exists()) {
            val defaultJson = this::class.java.classLoader
                ?.getResourceAsStream("manutenzioni_db.json")
                ?.bufferedReader()
                ?.readText()

            if (defaultJson != null) {
                dbFile.writeText(defaultJson)
                println("✓ Database demo copiato in: ${dbFile.absolutePath}")
            } else {
                // Crea un database vuoto
                val emptyDb = ManutenzioniDatabase(emptyList(), emptyList(), emptyList())
                dbFile.writeText(json.encodeToString(ManutenzioniDatabase.serializer(), emptyDb))
                println("⚠ Database demo non trovato nelle resources. Creato database vuoto.")
            }
        }
    }

    private fun loadFromDisk(): ManutenzioniDatabase {
        return try {
            val content = dbFile.readText()
            json.decodeFromString(ManutenzioniDatabase.serializer(), content)
        } catch (e: Exception) {
            println("Errore nel caricamento del database: \${e.message}")
            ManutenzioniDatabase(emptyList(), emptyList(), emptyList())
        }
    }

    private fun saveToDisk(impianti: List<Impianto>, clienti: List<Cliente>, cantieri: List<Cantiere>) {
        val db = ManutenzioniDatabase(impianti, clienti, cantieri)
        dbFile.writeText(json.encodeToString(ManutenzioniDatabase.serializer(), db))
    }

    private fun getImpiantiCache(): MutableList<Impianto> {
        if (cache == null) {
            val db = loadFromDisk()
            cache = db.impianti.toMutableList()
            if (cacheClienti == null) {
                cacheClienti = db.clienti.toMutableList()
            }
            if (cacheCantieri == null) {
                cacheCantieri = db.cantieri.toMutableList()
            }
        }
        return cache!!
    }

    private fun getClientiCache(): MutableList<Cliente> {
        if (cacheClienti == null) {
            val db = loadFromDisk()
            cacheClienti = db.clienti.toMutableList()
            if (cache == null) {
                cache = db.impianti.toMutableList()
            }
            if (cacheCantieri == null) {
                cacheCantieri = db.cantieri.toMutableList()
            }
        }
        return cacheClienti!!
    }

    private fun getCantieriCache(): MutableList<Cantiere> {
        if (cacheCantieri == null) {
            val db = loadFromDisk()
            cacheCantieri = db.cantieri.toMutableList()
            // Assicura che anche le altre cache siano popolate
            getImpiantiCache()
            getClientiCache()
        }
        return cacheCantieri!!
    }

    // === Impianti CRUD ===

    override suspend fun salvaImpianto(impianto: Impianto) {
        val list = getImpiantiCache()
        val index = list.indexOfFirst { it.codIntervento == impianto.codIntervento }
        if (index >= 0) {
            list[index] = impianto
        } else {
            list.add(impianto)
        }
        saveToDisk(list, getClientiCache(), getCantieriCache())
    }

    override suspend fun caricaImpianti(): List<Impianto> {
        return getImpiantiCache().toList()
    }

    override suspend fun eliminaImpianto(codIntervento: String) {
        val list = getImpiantiCache()
        list.removeAll { it.codIntervento == codIntervento }
        saveToDisk(list, getClientiCache(), getCantieriCache())
    }

    override suspend fun getImpianto(codIntervento: String): Impianto? {
        return getImpiantiCache().find { it.codIntervento == codIntervento }
    }

    // === Clienti CRUD ===

    override suspend fun caricaClienti(): List<Cliente> {
        return getClientiCache().toList()
    }

    override suspend fun salvaCliente(cliente: Cliente) {
        val list = getClientiCache()
        val index = list.indexOfFirst { it.id == cliente.id }
        if (index >= 0) {
            list[index] = cliente
        } else {
            list.add(cliente)
        }
        saveToDisk(getImpiantiCache(), list, getCantieriCache())
    }

    override suspend fun eliminaCliente(id: String) {
        val list = getClientiCache()
        list.removeAll { it.id == id }
        saveToDisk(getImpiantiCache(), list, getCantieriCache())
    }

    // === Getters for new workflow ===

    override suspend fun getCantieriForCliente(clienteId: String): List<Cantiere> {
        return getCantieriCache().filter { it.clienteId == clienteId }
    }

    override suspend fun getImpiantiForCantiere(cantiereId: String): List<Impianto> {
        return getImpiantiCache().filter { it.cantiereId == cantiereId }
    }

    // === Cantieri CRUD ===

    override suspend fun salvaCantiere(cantiere: Cantiere) {
        val list = getCantieriCache()
        val index = list.indexOfFirst { it.id == cantiere.id }
        if (index >= 0) {
            list[index] = cantiere
        } else {
            list.add(cantiere)
        }
        saveToDisk(getImpiantiCache(), getClientiCache(), list)
    }

    override suspend fun eliminaCantiere(id: String) {
        val list = getCantieriCache()
        list.removeAll { it.id == id }
        saveToDisk(getImpiantiCache(), getClientiCache(), list)
    }
}
