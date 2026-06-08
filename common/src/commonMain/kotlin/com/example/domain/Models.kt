package com.example.domain

data class Apparato(
    val id: String,
    val tipo: String, // e.g. "Quadro Elettrico", "Cabina MT", "UPS"
    val nome: String,
    val frequenza: String // e.g. "Semestrale", "Annuale"
)

data class Cantiere(
    val id: String,
    val nome: String,
    val apparati: List<Apparato>
)

data class Cliente(
    val id: String,
    val nome: String,
    val cantieri: List<Cantiere>
)

object MockDatabase {
    val clienti = listOf(
        Cliente(
            id = "c1",
            nome = "Ospedali Privati",
            cantieri = listOf(
                Cantiere(
                    id = "cant1",
                    nome = "Villa Igea",
                    apparati = buildList {
                        // 5 Quadri elettrici semestrali
                        for (i in 1..5) {
                            add(Apparato("q$i", "Quadro Elettrico", "Quadro Elettrico $i", "Semestrale"))
                        }
                        // 3 Cabine MT annuali
                        for (i in 1..3) {
                            add(Apparato("cab$i", "Cabina MT", "Cabina Media Tensione $i", "Annuale"))
                        }
                        // 5 UPS semestrali
                        for (i in 1..5) {
                            add(Apparato("ups$i", "UPS", "UPS $i", "Semestrale"))
                        }
                    }
                ),
                Cantiere(
                    id = "cant2",
                    nome = "Clinica San Francesco",
                    apparati = listOf(
                        Apparato("q1_sf", "Quadro Elettrico", "Quadro Generale", "Annuale"),
                        Apparato("ups1_sf", "UPS", "UPS Sala Operatoria", "Semestrale")
                    )
                )
            )
        ),
        Cliente(
            id = "c2",
            nome = "Industrie Rossi",
            cantieri = listOf(
                Cantiere(
                    id = "cant3",
                    nome = "Stabilimento Nord",
                    apparati = listOf(
                        Apparato("rif1", "Rifasamento", "Rifasamento Principale", "Annuale"),
                        Apparato("q1_nord", "Quadro Elettrico", "Quadro Produzione", "Semestrale")
                    )
                )
            )
        )
    )

    fun getCantieriByCliente(clienteId: String): List<Cantiere> {
        return clienti.find { it.id == clienteId }?.cantieri ?: emptyList()
    }
}
