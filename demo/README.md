# In Città | API eventi e autenticazione con Spring Boot

Componente backend in `demo/`: espone le API per eventi, autenticazione e
geocodifica usate dal [frontend Angular collegato](../event-frontend/README.md).
Il progetto completo,
con istruzioni di avvio integrato, è descritto nel
[README principale](../README.md).

Spring Boot 3.5.6 e Java 21. Le risorse principali sono `/api/events`,
`/api/auth` e `/api/geocode`; il frontend le raggiunge tramite il proxy `/api`.

## Avvio locale

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
| PUT | `/api/events/{id}` | Modifica, richiede Bearer JWT |
| DELETE | `/api/events/{id}` | Elimina, richiede Bearer JWT |
| POST | `/api/auth/register` | Registra username e password, minimo 8 caratteri |
| POST | `/api/auth/login` | Restituisce `{ "token": "..." }` |

Per retrocompatibilità restano `/api/events/categoria/{categoria}` e
`/api/events/data?start=...&end=...`. Le date sono nel formato locale
`yyyy-MM-ddTHH:mm:ss` senza offset. Gli errori usano Problem Details con
`status`, `title`, `detail` e, per la validazione, `errors`.

Imposta un `JWT_SECRET` privato di almeno 64 byte in produzione; il valore
predefinito è esclusivamente per lo sviluppo locale. Per PostgreSQL, attiva
`SPRING_PROFILES_ACTIVE=postgres` e configura le variabili
`SPRING_DATASOURCE_*`. Per cambiare il servizio di geocodifica Nominatim
compatibile, usa `GEOCODING_BASE_URL`. L'endpoint può restituire 404, 429 o
502; il frontend offre comunque la selezione manuale delle coordinate.

## Persistenza, autorizzazioni e Docker

H2 locale e PostgreSQL sono archivi separati: cambiare profilo non importa
eventi o account. Lo schema è gestito da Hibernate con `ddl-auto: update`,
non da migrazioni versionate.

Gli endpoint di scrittura richiedono autenticazione, ma non controllano
la proprietà dell'evento o ruoli distinti. Non presentare questa protezione
come un sistema completo di autorizzazione.

Il Dockerfile copia il JAR da `target/`: esegui prima `mvn clean package`
in questa cartella, poi avvia Compose dalla radice del repository.
Le precauzioni sui segreti e i limiti della demo sono nel
[README principale](../README.md).
