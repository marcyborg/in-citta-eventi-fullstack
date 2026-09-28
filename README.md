# In Città | Eventi cittadini fullstack

Un'unica applicazione per consultare e gestire eventi cittadini, composta dal
[backend Java Spring Boot](demo/README.md) in `demo/` e dal
[frontend Angular](event-frontend/README.md) in `event-frontend/`. Le due cartelle
sono parti dello stesso progetto: Angular chiama le API Spring Boot sotto `/api`;
il proxy di sviluppo e Nginx in Docker inoltrano le richieste al backend.

| Componente | Cartella | Responsabilità |
| --- | --- | --- |
| Backend Spring Boot | [`demo/`](demo/README.md) | CRUD e ricerca eventi, autenticazione JWT, geocodifica, Swagger, H2/PostgreSQL |
| Frontend Angular | [`event-frontend/`](event-frontend/README.md) | Agenda, filtri, dettaglio, login, form eventi e mappa Leaflet/OpenStreetMap |
| Avvio integrato | [`compose.yaml`](compose.yaml) | Angular + Spring Boot + PostgreSQL |

## Funzionalità

- Eventi ordinati per data, ricerca per categoria e intervallo di date,
  dettaglio, paginazione e operazioni CRUD.
- Validazioni (fra cui titolo obbligatorio e data futura) e errori API
  strutturati.
- Registrazione e accesso con JWT: la creazione, modifica e cancellazione
  degli eventi richiede autenticazione.
- Posizione facoltativa con coordinate, selezione su mappa e geocodifica su
  richiesta. Il dettaglio mostra anche i luoghi degli eventi già presenti nel
  database: una mappa se hanno coordinate; altrimenti propone una ricerca del
  luogo a partire dal testo salvato, senza modificare il record.

## Avvio in locale

Servono JDK 21, Maven 3.9+ e Node.js 20.19+ o 22. Dalla radice del repository
apri due terminali:

```bash
cd demo
mvn spring-boot:run
```

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

## Avvio integrato con Docker

La build dell'immagine backend usa un JAR già compilato. Dalla radice:

```bash
cd demo
mvn test package
cd ..
```

Crea un file `.env` locale prendendo come guida [`.env.example`](.env.example)
e imposta un `JWT_SECRET` privato, casuale, di almeno 64 byte. Imposta anche
`DB_PASSWORD` per PostgreSQL. Non aggiungere `.env` al repository.

```bash
docker compose up --build
```

Frontend: `http://localhost:4200/`; backend:
`http://localhost:8080/`; Swagger:
`http://localhost:8080/swagger-ui/index.html`. PostgreSQL usa un volume
persistente. `docker compose down -v` elimina quel volume e i dati contenuti.
Il valore JWT predefinito in `application.yml` è solo per lo sviluppo locale:
non usarlo in ambienti pubblici.

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

## Mappa e dati preesistenti

La ricerca del luogo usa Nominatim dopo un'azione esplicita dell'utente,
senza completamento automatico; l'istanza applica un intervallo minimo di
1,1 secondi fra richieste e una cache in memoria. Per carichi di produzione
usa un servizio compatibile gestito con limiti e cache condivisi
(`GEOCODING_BASE_URL`). Rispetta la
[policy Nominatim](https://operations.osmfoundation.org/policies/nominatim/)
e la [policy dei riquadri OpenStreetMap](https://operations.osmfoundation.org/policies/tiles/).
Le coordinate geocodificate vanno sempre verificate prima del salvataggio.

## Repository e installazione sul PC

La radice del repository contiene sia `demo/` sia `event-frontend/`.
Su Windows, apri un terminale in
`C:\Users\Francesco\Documents\GitHub` ed esegui:

```powershell
git clone https://github.com/marcyborg/in-citta-eventi-fullstack.git
cd in-citta-eventi-fullstack
```

`.gitignore` esclude `target/`, `dist/`, `node_modules/`, cache Angular,
database locali, segreti `.env` e output JavaScript generati. Conserva
`package-lock.json` per installazioni riproducibili; non inserire mai
credenziali o dati personali negli esempi e nei commit.
