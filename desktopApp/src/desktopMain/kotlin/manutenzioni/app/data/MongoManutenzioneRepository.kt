package manutenzioni.app.data

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import manutenzioni.domain.ManutenzioneRepository
import manutenzioni.domain.model.Cantiere
import manutenzioni.domain.model.Cliente
import manutenzioni.domain.model.Impianto

class MongoManutenzioneRepository(connectionString: String = "mongodb://localhost:27017") : ManutenzioneRepository {
    private val client = MongoClient.create(connectionString)
    private val database = client.getDatabase("manutenzioni_db")

    private val impiantiCollection = database.getCollection<Impianto>("impianti")
    private val clientiCollection = database.getCollection<Cliente>("clienti")
    private val cantieriCollection = database.getCollection<Cantiere>("cantieri")

    override suspend fun salvaImpianto(impianto: Impianto) {
        // Upsert by id
        impiantiCollection.replaceOne(
            filter = eq("id", impianto.id),
            replacement = impianto,
            options = ReplaceOptions().upsert(true)
        )
    }

    override suspend fun caricaImpianti(): List<Impianto> {
        return impiantiCollection.find().toList()
    }

    override suspend fun eliminaImpianto(id: String) {
        impiantiCollection.deleteOne(eq("id", id))
    }

    override suspend fun getImpianto(codIntervento: String): Impianto? {
        return impiantiCollection.find(eq("codIntervento", codIntervento)).firstOrNull()
    }

    override suspend fun aggiornaImpiantiGlobalmente(impiantoTemplate: Impianto) {
        // Update all impianti with the same codIntervento
        val impianti = impiantiCollection.find(eq("codIntervento", impiantoTemplate.codIntervento)).toList()
        for (imp in impianti) {
            val updated = impiantoTemplate.copy(
                id = imp.id, 
                cantiereId = imp.cantiereId, 
                quantita = imp.quantita, 
                noteSpecifiche = imp.noteSpecifiche
            )
            salvaImpianto(updated)
        }
    }

    override suspend fun caricaClienti(): List<Cliente> {
        return clientiCollection.find().toList()
    }

    override suspend fun salvaCliente(cliente: Cliente) {
        clientiCollection.replaceOne(
            filter = eq("id", cliente.id),
            replacement = cliente,
            options = ReplaceOptions().upsert(true)
        )
    }

    override suspend fun updateCliente(id: String, newName: String) {
        val cliente = clientiCollection.find(eq("id", id)).firstOrNull()
        if (cliente != null) {
            salvaCliente(cliente.copy(nome = newName))
        }
    }

    override suspend fun eliminaCliente(id: String) {
        clientiCollection.deleteOne(eq("id", id))
    }

    override suspend fun getCantieriForCliente(clienteId: String): List<Cantiere> {
        return cantieriCollection.find(eq("clienteId", clienteId)).toList()
    }

    override suspend fun getImpiantiForCantiere(cantiereId: String): List<Impianto> {
        return impiantiCollection.find(eq("cantiereId", cantiereId)).toList()
    }

    override suspend fun salvaCantiere(cantiere: Cantiere) {
        cantieriCollection.replaceOne(
            filter = eq("id", cantiere.id),
            replacement = cantiere,
            options = ReplaceOptions().upsert(true)
        )
    }

    override suspend fun updateCantiere(id: String, newName: String) {
        val cantiere = cantieriCollection.find(eq("id", id)).firstOrNull()
        if (cantiere != null) {
            salvaCantiere(cantiere.copy(nome = newName))
        }
    }

    override suspend fun eliminaCantiere(id: String) {
        cantieriCollection.deleteOne(eq("id", id))
    }
}
