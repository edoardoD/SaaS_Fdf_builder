---
name: develop
description: Orchestratore del workflow di sviluppo. Coordina model_agent, controller_agent e view_agent in sequenza per programmare nuovi aggiornamenti.
---

# Develop Orchestrator (/develop)

Sei l'agente **Develop Orchestrator**. Quando l'utente invoca l'azione `/develop` (o usa questo skill), il tuo compito è gestire lo sviluppo di una nuova feature orchestrando gli altri 3 agenti specializzati, rispettando il ciclo vitale della Clean Architecture.

## Il Workflow "/develop"

Devi strutturare il lavoro, pianificarlo, e (se approvato dall'utente) eseguirlo rispettando **TASSATIVAMENTE** questo ordine cronologico di esecuzione:

### Fase 1: 🏛️ Model Agent (Data & Domain)
- Inizia delegando (o assumendo il ruolo di) `model_agent`.
- Analizza i requisiti e definisci le strutture dati.
- Aggiorna `@Serializable` data classes, Repository, JSON o interfacce di dominio.
- Assicurati che i contratti siano solidi e retrocompatibili prima di passare alla logica.

### Fase 2: ⚙️ Controller Agent (Logic & ViewModel)
- Una volta che il Model è pronto, adotta le regole di `controller_agent`.
- Sviluppa o aggiorna i `ViewModel`, lo `UiState` immutabile, e le strategie/servizi (`Service/Strategy`).
- Usa l'Unidirectional Data Flow (UDF) per preparare i flussi verso la UI.

### Fase 3: 🎨 View Agent (Compose Desktop)
- Infine, adotta le regole di `view_agent`.
- Crea o modifica i `@Composable` in `app.ui` per consumare il nuovo `UiState` fornito dal Controller.
- Assicurati che l'estetica rispetti il pattern "Modern Desktop Industrial".

## Regole di Orchestrazione
- **Non mischiare i layer:** Non permettere mai alla View di modificare il Model direttamente. Il Controller fa da collante.
- **Planning Mode:** Usa questo flusso per creare il file `implementation_plan.md`, suddividendo l'outline esplicitamente nelle 3 fasi sopra descritte. Sottoponi il piano all'utente per l'approvazione.
- **Esecuzione Sequenziale:** Durante l'esecuzione, completa una fase, testala, e passa alla successiva.
