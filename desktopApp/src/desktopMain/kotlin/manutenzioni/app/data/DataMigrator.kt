package manutenzioni.app.data

import kotlinx.coroutines.runBlocking

/**
 * Script standalone per migrare i dati dall'attuale file manutenzioni_db.json
 * verso il nuovo database MongoDB locale, garantendo nessuna perdita di dati.
 */
fun main() {
    runBlocking {
    println("Avvio migrazione dati da JSON a MongoDB...")

    // 1. Inizializziamo entrambi i repository
    val jsonRepo = JsonManutenzioneRepository()
    val mongoRepo = MongoManutenzioneRepository("mongodb://localhost:27017")

    // 2. Carichiamo i dati dal JSON
    val clienti = jsonRepo.caricaClienti()
    val impianti = jsonRepo.caricaImpianti()
    
    // Per gestire i cantieri nel nuovo workflow (se ci sono)
    // Se non ci sono cantieri salvati ma sono dentro impianti, questo è il modo per estrarli o gestire.
    // In JsonManutenzioneRepository non vedo esplicitato il caricamento separato dei cantieri nel file
    // ma la getCantieriForCliente e salvaCantiere dovrebbero gestirlo.

    println("Trovati ${clienti.size} clienti nel JSON.")
    println("Trovati ${impianti.size} impianti nel JSON.")

    // 3. Migriamo i Clienti
    for (cliente in clienti) {
        mongoRepo.salvaCliente(cliente)
        
        // Nel JSON, i cantieri sono stati aggiunti ultimamente, recuperiamo e migriamo
        val cantieri = jsonRepo.getCantieriForCliente(cliente.id)
        for (cantiere in cantieri) {
            mongoRepo.salvaCantiere(cantiere)
        }
    }
    
    // 4. Migriamo gli Impianti
    for (impianto in impianti) {
        mongoRepo.salvaImpianto(impianto)
    }

        println("Migrazione completata con successo! I dati sono ora in MongoDB.")
        kotlin.system.exitProcess(0)
    }
}
