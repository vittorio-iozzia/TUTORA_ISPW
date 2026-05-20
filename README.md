# TUTORA
**Academic Tutoring Platform**

TUTORA is a desktop application that connects students and tutors on a single platform.
Students can search for tutors, book lessons, submit applications to become tutors themselves, and leave reviews.
Tutors manage their availability and respond to booking requests. An admin oversees quality by evaluating tutor applications.

The platform is available in two interfaces: a full **JavaFX GUI** and a **CLI** for terminal-based use.

Built for the Software Engineering and Project course (ISPW).

---

## Overview

The system supports three user roles — **Student**, **Tutor**, and **Admin** — each with dedicated functionalities:

- **Tutor Search & Booking** — Students browse and filter available tutors by category. They view tutor profiles, pick an available lesson slot, and send a booking request. The tutor is notified and can accept or reject.
- **Apply to Become a Tutor** — Students submit a tutor application with supporting documents for a chosen category. The admin reviews the application and the outcome is sent back via notification.
- **Notification System** — Role-aware notification panel. Each notification may carry an executable action (pay a lesson, accept/reject a booking, approve/reject an application).
- **Reviews** — After a completed lesson, students can rate and review the tutor.
- **Payments** — Integrated PayPal payment flow triggered when a booking is accepted by the tutor.
- **Chat** — Direct messaging between students and tutors.
- **Session Management** — Concurrent user sessions handled via token-based mechanism.
- **Social Login** — OAuth integration with Google and Meta.
- **Dual Interface** — Full graphical interface (JavaFX) and a command-line interface (CLI), selectable at startup.

---

## Architecture

Java application following the **MVC** pattern, with a layered separation between boundary, controller, and entity.

| Pattern          | Where it is used                                                                 |
|------------------|----------------------------------------------------------------------------------|
| Singleton        | `DaoFactory` (Bill Pugh Holder), `SessionManager`                                |
| Abstract Factory | `DaoFactory` — three concrete implementations: `DbDaoFactory`, `DemoDaoFactory`, `JsonDaoFactory` |
| Observer         | `TutorApplication` — push model via `java.beans.PropertyChangeSupport`           |
| MVC              | Boundary (GFX/CLI controllers) → Application controllers → Model/DAO             |

---

## Requirements

**Software**
- JDK 17+
- Maven 3.8+
- JavaFX SDK 21
- MySQL 8.0+ (optional — the app also runs with in-memory Demo or JSON persistence)
- Git

**Hardware**
- Multi-core CPU (i5 / Ryzen 5 or equivalent)
- 8 GB RAM minimum
- 500 MB free disk
- 1366x768 display minimum
- Internet connection (required for PayPal and OAuth flows)

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/vittorio-iozzia/TUTORA_ISPW.git
cd TUTORA_ISPW
```

### 2. Set up the database (only for DB mode)

Make sure MySQL is running, then import the schema:

```bash
mysql -u root -p < database/TUTORA_db.sql
```

This creates the database with all tables, indexes, foreign keys, and sample data.

If your MySQL instance uses a different host, port, or credentials, update the connection settings in `src/main/resources/app.properties` before launching.

### 3. Build and run

```bash
mvn clean install
mvn javafx:run
```

At startup you will be prompted to choose the interface and the persistence mode:

```
  Seleziona interfaccia:
    1) Grafica (JavaFX)
    2) Testuale (CLI)

  Seleziona modalita' di persistenza:
    1) Demo  (in-memory, nessun DB)
    2) JSON  (file su disco)
    3) DB    (MySQL)
```

You can also skip the prompt by passing arguments directly:

```bash
# GUI with in-memory demo data (no DB needed)
mvn javafx:run -Djavafx.args="--ui=GFX --dao=DEMO"

# CLI with in-memory demo data
mvn javafx:run -Djavafx.args="--ui=CLI --dao=DEMO"

# GUI with MySQL
mvn javafx:run -Djavafx.args="--ui=GFX --dao=DB"
```

---

## Testing

Tests are split into two suites:

**BookingApplicationDomain** — booking and review logic:
- Duplicate booking detection (pass and throw cases)
- Duplicate review prevention
- Rating value validation
- Student budget deduction after payment

**StudentReviewDomain** — tutor application and budget logic:
- Tutor application DAO persistence
- Application readiness check before submission
- FSM status transition validation (DRAFT → SUBMITTED → ACCEPTED/REJECTED)
- Insufficient budget detection
- Budget boundary validation

Run all tests with:

```bash
mvn test
```

---

## Code Quality

Monitored via SonarCloud — Project: [`vittorio-iozzia_TUTORA_ISPW`](https://sonarcloud.io/project/overview?id=vittorio-iozzia_TUTORA_ISPW).

---

## Authors

- Alessio Dainelli
- Vittorio Iozzia

---
---

# TUTORA
**Piattaforma di Tutoraggio Accademico**

TUTORA e' un'applicazione desktop che mette in contatto studenti e tutor su un'unica piattaforma.
Gli studenti possono cercare tutor, prenotare lezioni, candidarsi per diventare tutor e lasciare recensioni.
I tutor gestiscono la propria disponibilita' e rispondono alle richieste di prenotazione. Un admin garantisce la qualita' valutando le candidature dei tutor.

La piattaforma e' disponibile in due interfacce: una **GUI JavaFX** completa e una **CLI** per l'uso da terminale.

Realizzata per il corso di Ingegneria del Software e Progettazione Web (ISPW).

---

## Panoramica

Il sistema supporta tre ruoli utente — **Studente**, **Tutor** e **Admin** — ciascuno con funzionalita' dedicate:

- **Ricerca Tutor e Prenotazione** — Gli studenti sfogliano e filtrano i tutor disponibili per categoria. Visualizzano il profilo del tutor, scelgono uno slot disponibile e inviano una richiesta di prenotazione. Il tutor riceve una notifica e puo' accettare o rifiutare.
- **Candidatura Tutor** — Gli studenti inviano una candidatura con i documenti richiesti per una categoria scelta. L'admin valuta la candidatura e l'esito viene comunicato tramite notifica.
- **Sistema di Notifiche** — Pannello notifiche consapevole del ruolo. Ogni notifica puo' includere un'azione eseguibile (pagare una lezione, accettare/rifiutare una prenotazione, approvare/rifiutare una candidatura).
- **Recensioni** — Al termine di una lezione, gli studenti possono valutare e recensire il tutor.
- **Pagamenti** — Flusso di pagamento PayPal integrato, attivato quando il tutor accetta una prenotazione.
- **Chat** — Messaggistica diretta tra studenti e tutor.
- **Gestione Sessioni** — Sessioni utente concorrenti gestite tramite meccanismo basato su token.
- **Social Login** — Integrazione OAuth con Google e Meta.
- **Doppia Interfaccia** — Interfaccia grafica completa (JavaFX) e interfaccia a riga di comando (CLI), selezionabile all'avvio.

---

## Architettura

Applicazione Java che segue il pattern **MVC**, con separazione a strati tra boundary, controller ed entita'.

| Pattern          | Dove viene utilizzato                                                                          |
|------------------|-----------------------------------------------------------------------------------------------|
| Singleton        | `DaoFactory` (Bill Pugh Holder), `SessionManager`                                             |
| Abstract Factory | `DaoFactory` — tre implementazioni concrete: `DbDaoFactory`, `DemoDaoFactory`, `JsonDaoFactory` |
| Observer         | `TutorApplication` — push model tramite `java.beans.PropertyChangeSupport`                    |
| MVC              | Boundary (controller GFX/CLI) -> Controller applicativi -> Model/DAO                          |

---

## Requisiti

**Software**
- JDK 17+
- Maven 3.8+
- JavaFX SDK 21
- MySQL 8.0+ (opzionale — l'app funziona anche con persistenza Demo in-memory o JSON)
- Git

**Hardware**
- CPU multi-core (i5 / Ryzen 5 o equivalente)
- 8 GB RAM minimo
- 500 MB di spazio libero su disco
- Display minimo 1366x768
- Connessione internet (necessaria per i flussi PayPal e OAuth)

---

## Per Iniziare

### 1. Clona il repository

```bash
git clone https://github.com/vittorio-iozzia/TUTORA_ISPW.git
cd TUTORA_ISPW
```

### 2. Configura il database (solo per la modalita' DB)

Assicurati che MySQL sia in esecuzione, poi importa lo schema:

```bash
mysql -u root -p < database/TUTORA_db.sql
```

Questo crea il database con tutte le tabelle, gli indici, le chiavi esterne e i dati di esempio.

Se la tua istanza MySQL usa un host, porta o credenziali diversi, aggiorna le impostazioni di connessione in `src/main/resources/app.properties` prima di avviare.

### 3. Build e avvio

```bash
mvn clean install
mvn javafx:run
```

All'avvio verra' chiesto di scegliere l'interfaccia e la modalita' di persistenza:

```
  Seleziona interfaccia:
    1) Grafica (JavaFX)
    2) Testuale (CLI)

  Seleziona modalita' di persistenza:
    1) Demo  (in-memory, nessun DB)
    2) JSON  (file su disco)
    3) DB    (MySQL)
```

E' possibile saltare il prompt passando gli argomenti direttamente:

```bash
# GUI con dati demo in-memory (nessun DB necessario)
mvn javafx:run -Djavafx.args="--ui=GFX --dao=DEMO"

# CLI con dati demo in-memory
mvn javafx:run -Djavafx.args="--ui=CLI --dao=DEMO"

# GUI con MySQL
mvn javafx:run -Djavafx.args="--ui=GFX --dao=DB"
```

---

## Testing

I test sono suddivisi in due suite:

**BookingApplicationDomain** — logica di prenotazione e recensione:
- Rilevamento prenotazione duplicata (casi pass e throw)
- Prevenzione recensione duplicata
- Validazione del valore del rating
- Deduzione del budget studente dopo il pagamento

**StudentReviewDomain** — logica di candidatura tutor e budget:
- Persistenza DAO della candidatura tutor
- Verifica di completezza prima della sottomissione
- Validazione delle transizioni di stato FSM (DRAFT -> SUBMITTED -> ACCEPTED/REJECTED)
- Rilevamento budget insufficiente
- Validazione dei limiti del budget

Esegui tutti i test con:

```bash
mvn test
```

---

## Qualita' del Codice

Monitorata tramite SonarCloud — Progetto: [`vittorio-iozzia_TUTORA_ISPW`](https://sonarcloud.io/project/overview?id=vittorio-iozzia_TUTORA_ISPW).

---

## Autori

- Alessio Dainelli
- Vittorio Iozzia
