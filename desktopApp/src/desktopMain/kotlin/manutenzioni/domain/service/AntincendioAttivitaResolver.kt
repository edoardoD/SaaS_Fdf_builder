package manutenzioni.domain.service

import manutenzioni.domain.model.Attivita
import manutenzioni.domain.model.Impianto
import manutenzioni.domain.model.Periodo

/**
 * Resolver per calcolare le attività effettive per la Rilevazione Incendi (RI)
 * in base alla composizione impiantistica del cantiere.
 */
object AntincendioAttivitaResolver {

    /**
     * Risolve le attività effettive per la Rilevazione Incendi.
     *
     * @param impianto L'impianto RI
     * @param impiantiNelCantiere Tutti gli impianti presenti nel cantiere
     * @param frequenza La frequenza selezionata per la generazione
     * @return Lista delle attività filtrate
     */
    fun resolveAttivita(
        impianto: Impianto,
        impiantiNelCantiere: List<Impianto>,
        frequenza: Periodo
    ): List<Attivita> {
        // 1. Filtro base per frequenza
        val attivitaFrequenzaOk = FrequencyFilter.filterByFrequenza(impianto.listaAttivita, frequenza)

        // Se non è l'impianto Antincendio, ritorniamo semplicemente il filtro per frequenza.
        // Assumiamo che il codice intervento per antincendio sia "RI".
        if (impianto.codIntervento.trim().uppercase() != "RI") {
            return attivitaFrequenzaOk
        }

        // 2. Filtro contestuale impianti presenti nel cantiere
        val codiciPresenti = impiantiNelCantiere.map { it.codIntervento.trim().uppercase() }.toSet()

        return attivitaFrequenzaOk.filter { att ->
            val target = att.targetImpiantoCod?.trim()?.uppercase()
            target == null || target in codiciPresenti
        }
    }
}
