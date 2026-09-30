# In Città | Eventi cittadini con Spring Boot, Angular e mappa OpenStreetMap

Applicazione fullstack dimostrativa per consultare e gestire eventi cittadini,
filtrare l'agenda e visualizzare il luogo su una mappa interattiva.
Il [backend Java Spring Boot](demo/README.md) in `demo/` e il
[frontend Angular](event-frontend/README.md) in `event-frontend/` sono parti
dello stesso progetto: Angular chiama le API Spring Boot sotto `/api`;
il proxy di sviluppo e Nginx in Docker inoltrano le richieste al backend.

| Componente | Cartella | Tecnologie e responsabilità |
| --- | --- | --- |
| API e logica eventi | [`demo/`](demo/README.md) | Java 21, Spring Boot 3.5.6, JPA, JWT, Flyway, Swagger, H2/PostgreSQL |
| Interfaccia e mappa | [`event-frontend/`](event-frontend/README.md) | Angular 20, TypeScript, Leaflet, OpenStreetMap, agenda e form eventi |
| Avvio integrato | [`compose.yaml`](compose.yaml) | Docker Compose, PostgreSQL 16 e Nginx con proxy API |

## Anteprima

Screenshot reali dell'applicazione, acquisiti su un'istanza isolata con dati
dimostrativi. Titoli, descrizioni e date non rappresentano un programma
effettivo di eventi cittadini.

### Agenda e filtri

La pagina iniziale mostra gli eventi in ordine cronologico e consente di
filtrare per categoria e intervallo di date.

![Agenda degli eventi con filtri e sei appuntamenti dimostrativi](docs/screenshots/agenda.png)

### Dettaglio con mappa

Un evento con coordinate salvate mostra il luogo sulla mappa Leaflet.
La cartografia visualizzata è di OpenStreetMap, con attribuzione mantenuta
nello screenshot.

![Dettaglio di un evento dimostrativo con mappa di Piazza del Duomo a Milano](docs/screenshots/dettaglio-mappa.png)

## Funzionalità

- **Agenda**: eventi ordinati per data, ricerca per categoria e intervallo di date,
  dettaglio, paginazione e operazioni CRUD.
- **Integrità dei dati**: validazioni (fra cui titolo obbligatorio e data futura) e errori API
  strutturati.
- **Autenticazione e permessi**: registrazione e accesso con JWT; ogni nuovo
  evento appartiene al suo autore. Solo il proprietario o un amministratore
  possono modificarlo o eliminarlo; gli eventi storici senza autore sono
  gestibili esclusivamente dagli amministratori.
- **Localizzazione**: posizione facoltativa con coordinate, selezione su mappa e geocodifica su
  richiesta. Il dettaglio mostra anche i luoghi degli eventi già presenti nel
  database: una mappa se hanno coordinate; altrimenti propone una ricerca del
  luogo a partire dal testo salvato, senza modificare il record.

## Scegliere il database e il percorso di avvio

Il progetto supporta **H2 e PostgreSQL**: scegli uno dei due percorsi qui
sotto, senza modificare il codice. H2 è il percorso rapido per iniziare
in locale; PostgreSQL con Docker Compose è consigliato per provare lo
stack completo e un database in un servizio separato.

| Percorso | Quando sceglierlo | Database e persistenza | Avvio |
| --- | --- | --- | --- |
| H2 locale | Sviluppo ed esercitazioni senza installare un server database | File `demo/data/events.mv.db`, conservato ai riavvii | Backend Maven e frontend Angular in due terminali |
| PostgreSQL con Docker | Demo dello stack completo, anche per chi clona il progetto | PostgreSQL 16 nel servizio `db`, con volume `postgres_data` | Docker Compose avvia database, backend e frontend |

Flyway gestisce lo schema in entrambe le modalità. H2 e PostgreSQL sono
**archivi distinti**: passare da un percorso all'altro non trasferisce
automaticamente eventi o account. Il volume rende persistente PostgreSQL,
ma non sostituisce un backup; nessuna delle due modalità rende la demo
pronta per l'esposizione pubblica.

**Se hai già dati, prima dell'avvio fai un backup e verifica lo schema**:
un database popolato senza cronologia Flyway non viene adottato automaticamente.
La [guida migrazioni e sicurezza](docs/MIGRAZIONI-E-SICUREZZA.md) spiega
variabili, amministratori e adozione dei database esistenti.

## H2: avvio locale rapido

Servono JDK 21, Maven 3.9+ e Node.js 20.19+ della serie 20 oppure 22.12+
della serie 22, con npm. Non servono Docker o un server PostgreSQL:
la configurazione predefinita usa H2 su file.

Nel terminale del backend imposta `JWT_SECRET` con una chiave privata casuale
di almeno 64 byte UTF-8. È obbligatoria anche con H2; `.env` non viene
caricato automaticamente da Maven. Ad esempio, in PowerShell, per la prima
configurazione:

```powershell
$bytes = New-Object byte[] 64
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
```

Conserva la chiave in un sistema appropriato per i segreti e reimpostala
negli avvii successivi dello stesso ambiente. Non generarne una nuova a
ogni riavvio: cambiarla invalida i token già emessi.

Dalla radice del repository, nello stesso terminale:

```bash
cd demo
mvn spring-boot:run
```

Apri un secondo terminale nella radice del repository per il frontend:

```bash
cd event-frontend
npm ci
npm start
```

Apri `http://localhost:4200/`; le API rispondono su
`http://localhost:8080/api`. La configurazione predefinita usa H2 persistente
in `demo/data/`: gli eventi e gli account restano disponibili ai riavvii,
ma il file del database non va pubblicato su Git. Dal frontend puoi registrare
un account e accedere per creare eventi. Il token vive solo nella memoria
della scheda e va richiesto di nuovo dopo un refresh.

Per questo percorso lascia non impostati `SPRING_PROFILES_ACTIVE` e gli
override `SPRING_DATASOURCE_*` eventualmente usati per PostgreSQL.
**Non attivare il profilo `h2` per conservare i dati**: quel profilo usa un
database temporaneo in memoria, diverso dall'H2 su file predefinito.
Per fermare l'applicazione usa `Ctrl+C` nei due terminali; non eliminare
`demo/data/` se vuoi conservare eventi e account.

## PostgreSQL: stack completo con Docker

Questo percorso avvia PostgreSQL 16, il backend Spring Boot e il frontend
Angular servito da Nginx. Compose seleziona già il profilo `postgres` e
collega il backend al servizio `db`: **non occorre installare PostgreSQL
sul PC né modificare `application.yml`**.

Servono Docker con il plugin Compose, JDK 21 e Maven 3.9+.
Node.js e npm per il frontend vengono usati all'interno della build Docker,
quindi non sono richiesti sul PC per questo percorso.

**Prima di avviare Compose, compila il backend**: il suo Dockerfile copia
un JAR già esistente e non esegue Maven durante la build dell'immagine.
Servono quindi JDK 21 e Maven anche per questo percorso di avvio.
Dalla radice:

```bash
cd demo
mvn clean package
cd ..
```

Il comando esegue i test e produce `demo/target/demo-0.0.1-SNAPSHOT.jar`.
Copia [`.env.example`](.env.example) in `.env` nella radice del repository,
se non hai già un file configurato. In PowerShell puoi usare
`Copy-Item .env.example .env` soltanto per la prima configurazione, senza
sovrascrivere un `.env` esistente. Compila entrambi i valori vuoti:
`JWT_SECRET` con un valore
privato casuale di almeno 64 byte e `DB_PASSWORD` con una password privata.
Il backend rifiuta segreti mancanti, troppo corti o segnaposto noti;
Compose rifiuta credenziali vuote. Non aggiungere `.env` al repository.

Per generare localmente un segreto in PowerShell, senza inserirne uno fisso
nella documentazione:

```powershell
$bytes = New-Object byte[] 64
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
[Convert]::ToBase64String($bytes)
```

Usa il risultato come valore di `JWT_SECRET` nel tuo `.env` e non condividerlo.
Prima dell'avvio verifica la configurazione con `docker compose config --quiet`,
che non stampa i valori dei segreti.

```bash
docker compose up --build
```

Frontend: `http://localhost:4200/`; backend:
`http://localhost:8080/`; Swagger:
`http://localhost:8080/swagger-ui/index.html`. PostgreSQL usa un volume
persistente; `docker compose down` lo conserva. **Non usare `docker compose
down -v` se vuoi conservare i dati**: elimina quel volume e i dati contenuti.
Non ci sono segreti JWT o password PostgreSQL di fallback. Le credenziali
configurano solo un nuovo volume: cambiare `.env` non ruota automaticamente
la password di un database già inizializzato.

Le porte host 4200 e 8080 devono essere libere. H2 locale e PostgreSQL Docker
sono database distinti: gli eventi e gli account non vengono trasferiti
automaticamente fra le due modalità.

Per verificare i servizi usa `docker compose ps`; per leggere i messaggi del
backend usa `docker compose logs backend`. Per fermare lo stack conservando
il volume usa `docker compose down`. Al successivo `docker compose up`,
eventi e account restano nel database PostgreSQL.

### PostgreSQL già installato, senza Docker

Se preferisci un server PostgreSQL esistente, crea un database e un utente
dedicati, poi configura nel terminale del backend `JWT_SECRET`,
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e
`SPRING_DATASOURCE_PASSWORD`. Dalla cartella `demo` avvia:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

In questo caso il server PostgreSQL e la persistenza sono gestiti da te;
avvia il frontend nel secondo terminale come nel percorso H2.
Per i dettagli e per database già popolati consulta la
[guida migrazioni e sicurezza](docs/MIGRAZIONI-E-SICUREZZA.md).

## Verifica

```bash
cd demo && mvn test
cd ../event-frontend && npm ci && npm run build
npm test -- --watch=false --browsers=ChromeHeadless
```

I test Angular richiedono Chrome/Chromium; configura `CHROME_BIN` se non viene
trovato. I dettagli delle API e delle variabili sono nel
[README backend](demo/README.md), quelli dell'interfaccia nel
[README frontend](event-frontend/README.md).

La [CI GitHub Actions](.github/workflows/ci.yml) esegue i test backend,
la build Angular e i test frontend per push e pull request.
Un job PostgreSQL 16 verifica permessi, migrazioni, conservazione dei dati,
riavvii applicativi e riavvio del container database, oltre ai vincoli Compose.
Non esegue la build e l'avvio completo dei tre servizi Compose.
I test ordinari usano H2 e una chiave pubblica esclusivamente sul classpath
di test; per PostgreSQL usa il database dedicato `in_citta_test`,
come descritto nella [guida](docs/MIGRAZIONI-E-SICUREZZA.md).

## Mappa e dati preesistenti

La ricerca del luogo usa Nominatim dopo un'azione esplicita dell'utente,
senza completamento automatico; l'istanza applica un intervallo minimo di
1,1 secondi fra richieste e una cache in memoria. Per carichi di produzione
usa un servizio compatibile gestito con limiti e cache condivisi
(`GEOCODING_BASE_URL`). Rispetta la
[policy Nominatim](https://operations.osmfoundation.org/policies/nominatim/)
e la [policy dei riquadri OpenStreetMap](https://operations.osmfoundation.org/policies/tiles/).
Le coordinate geocodificate vanno sempre verificate prima del salvataggio.

## Limiti della demo

- **Gestione operativa**: l'autorizzazione proprietario/amministratore è
  implementata, ma non esiste un pannello di amministrazione degli account.
  L'amministratore iniziale viene creato solo con una procedura offline esplicita.
- **Dati e credenziali**: Flyway gestisce le migrazioni e Hibernate valida
  lo schema, ma restano necessari backup verificati, rotazione dei segreti e
  una procedura controllata per gli archivi preesistenti.
- **Servizi esterni**: mappe e geocodifica richiedono connettività e dipendono
  dalla disponibilità e dai limiti dei servizi OpenStreetMap/Nominatim.
- **Sessione e date**: il JWT resta in memoria e viene perso al refresh;
  le date sono locali, senza offset o gestione esplicita dei fusi orari.

Il progetto è adatto a esercitazioni e dimostrazioni tecniche. Non è un sistema
pronto per l'esposizione pubblica: prima servono gestione operativa,
HTTPS, revisione della configurazione di deployment e una verifica di sicurezza dedicata.

## Clonare e aggiornare il progetto

La radice del repository contiene sia `demo/` sia `event-frontend/`.
Clona il progetto in una cartella locale:

```bash
git clone https://github.com/marcyborg/in-citta-eventi-fullstack.git
cd in-citta-eventi-fullstack
```

Per aggiornare un clone esistente, conserva prima eventuali modifiche locali,
poi esegui `git switch main` e `git pull --ff-only origin main`.

`.gitignore` esclude `target/`, `dist/`, `node_modules/`, cache Angular,
database locali, segreti `.env` e output JavaScript generati. Conserva
`package-lock.json` per installazioni riproducibili; non inserire mai
credenziali o dati personali negli esempi e nei commit.

## Licenza e contenuti di terze parti

Il codice del progetto è distribuito con [licenza MIT](LICENSE),
copyright 2026 Francesco Marchitelli. Il testo della licenza definisce
permessi, condizioni e limitazioni di responsabilità.

Le dipendenze mantengono le rispettive licenze; la licenza del progetto
non sostituisce quelle dei contenuti di terze parti.
La cartografia e i dati OpenStreetMap, inclusi quelli visibili negli screenshot,
restano soggetti alle relative condizioni e attribuzioni:
[OpenStreetMap copyright e licenza](https://www.openstreetmap.org/copyright).
