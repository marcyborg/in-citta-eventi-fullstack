# In Città | API eventi e autenticazione con Spring Boot

Componente backend in `demo/`: espone le API per eventi, autenticazione e
geocodifica usate dal [frontend Angular collegato](../event-frontend/README.md).
Il progetto completo,
con istruzioni di avvio integrato, è descritto nel
[README principale](../README.md).

Spring Boot 3.5.6 e Java 21, con Flyway per lo schema. Le risorse principali sono `/api/events`,
`/api/auth` e `/api/geocode`; il frontend le raggiunge tramite il proxy `/api`.

## Avvio locale

Imposta prima `JWT_SECRET` nel terminale: deve essere privato, non un
segnaposto, di almeno 64 byte UTF-8. Maven non legge automaticamente `.env`.
I test ordinari non richiedono segreti reali.

```bash
mvn test
mvn spring-boot:run
```

Il database H2 predefinito è persistente nel file `data/events.mv.db`,
escluso da Git. Il profilo `h2` usa un database temporaneo per i test.
Swagger è in `http://localhost:8080/swagger-ui/index.html`.

| Metodo | Endpoint | Descrizione |
| --- | --- | --- |
| GET | `/api/events?page=0&size=10&categoria=teatro&start=2030-01-01T00:00:00&end=2030-12-31T23:59:59` | Ricerca combinata e paginata, ordinata per data |
| GET | `/api/events/all` | Elenco completo per data |
| GET | `/api/events/upcoming` | Prossimi eventi |
| GET | `/api/events/{id}` | Dettaglio |
| GET | `/api/geocode?luogo=Piazza%20del%20Duomo%2C%20Milano` | Localizzazione su richiesta, con cache e limite di traffico |
| POST | `/api/events` | Crea, richiede Bearer JWT |
| PUT | `/api/events/{id}` | Modifica, richiede Bearer JWT e proprietà dell'evento o ruolo ADMIN |
| DELETE | `/api/events/{id}` | Elimina, richiede Bearer JWT e proprietà dell'evento o ruolo ADMIN |
| POST | `/api/auth/register` | Registra username e password, minimo 8 caratteri |
| POST | `/api/auth/login` | Restituisce `{ "token": "...", "user": { "id": 1, "username": "...", "role": "USER" } }` |
| GET | `/api/auth/me` | Profilo dell'account autenticato, senza password |

Per retrocompatibilità restano `/api/events/categoria/{categoria}` e
`/api/events/data?start=...&end=...`. Le date sono nel formato locale
`yyyy-MM-ddTHH:mm:ss` senza offset. Gli errori usano Problem Details con
`status`, `title`, `detail` e, per la validazione, `errors`.

Imposta un `JWT_SECRET` privato di almeno 64 byte in ogni avvio normale;
non esiste un fallback. Per PostgreSQL, attiva
`SPRING_PROFILES_ACTIVE=postgres` e configura le variabili
`SPRING_DATASOURCE_*`. Per cambiare il servizio di geocodifica Nominatim
compatibile, usa `GEOCODING_BASE_URL`. L'endpoint può restituire 404, 429 o
502; il frontend offre comunque la selezione manuale delle coordinate.

## Persistenza, autorizzazioni e Docker

H2 locale e PostgreSQL sono archivi separati: cambiare profilo non importa
eventi o account. Flyway applica V1 e V2, mentre Hibernate usa `ddl-auto:
validate`: non modifica lo schema automaticamente. Uno schema preesistente
senza cronologia Flyway richiede verifica e baseline manuale controllata,
non viene adottato automaticamente.

La creazione assegna `ownerId` all'account autenticato. Il valore è
read-only nel JSON e non può essere scelto o trasferito dal client.
Modifica e cancellazione sono consentite solo al proprietario o a un ADMIN;
gli eventi storici con `ownerId: null` sono gestibili solo da ADMIN.
Registrazione pubblica sempre USER: ruoli e permessi sono risolti dal database,
anche quando viene riutilizzato un token già emesso.

Il Dockerfile copia il JAR da `target/`: esegui prima `mvn clean package`
in questa cartella, poi avvia Compose dalla radice del repository.
Le precauzioni sui segreti e i limiti della demo sono nel
[README principale](../README.md).

Per creare un amministratore offline, adottare un archivio esistente o
eseguire le integrazioni PostgreSQL consulta
[Migrazioni e sicurezza](../docs/MIGRAZIONI-E-SICUREZZA.md).
