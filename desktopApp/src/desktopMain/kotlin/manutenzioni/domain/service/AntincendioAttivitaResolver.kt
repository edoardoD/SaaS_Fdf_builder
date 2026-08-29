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
    /**
     * Filtra le attività contestualmente in base agli impianti presenti nel cantiere,
     * poi applica il filtro per frequenza.
     */
    fun resolveAttivita(
        impianto: Impianto,
        impiantiNelCantiere: List<Impianto>,
        frequenza: Periodo
    ): List<Attivita> {
        // 1. Filtro base per frequenza
        val attivitaFrequenzaOk = FrequencyFilter.filterByFrequenza(impianto.listaAttivita, frequenza)

        // Se non è l'impianto Antincendio, ritorniamo semplicemente il filtro per frequenza.
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

    /**
     * Calcola le frequenze disponibili per un impianto, tenendo conto
     * degli impianti effettivamente presenti nel cantiere.
     * Per RI, esclude le frequenze che derivano solo da attività condizionate
     * i cui impianti target non sono presenti nel cantiere.
     */
    fun resolveFrequenze(
        impianto: Impianto,
        impiantiNelCantiere: List<Impianto>
    ): List<Periodo> {
        if (impianto.codIntervento.trim().uppercase() != "RI") {
            return FrequencyFilter.frequenzeDisponibili(impianto.listaAttivita)
        }

        val codiciPresenti = impiantiNelCantiere.map { it.codIntervento.trim().uppercase() }.toSet()

        // Filtra le attività contestualmente, poi estrae le frequenze distinte
        val attivitaApplicabili = impianto.listaAttivita.filter { att ->
            val target = att.targetImpiantoCod?.trim()?.uppercase()
            target == null || target in codiciPresenti
        }

        return FrequencyFilter.frequenzeDisponibili(attivitaApplicabili)
    }
}
