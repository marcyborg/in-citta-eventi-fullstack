# In Città | Migrazioni, proprietà degli eventi e segreti

Questa versione aggiunge ruoli USER/ADMIN, proprietà degli eventi e Flyway.
Non migra automaticamente database preesistenti e non modifica le scelte
su sessioni in memoria, date locali o servizi cartografici.

## Credenziali e avvio

`JWT_SECRET` è obbligatorio anche con H2. Il backend rifiuta valori vuoti,
inferiori a 64 byte UTF-8 e i segnaposto dimostrativi noti; la chiave viene
validata all'avvio e non viene registrata nei log.

Per PowerShell puoi generare una chiave nella variabile del terminale:

```powershell
$bytes = New-Object byte[] 64
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
```

Conserva la chiave in un sistema appropriato per i segreti e riutilizzala per
i successivi avvii dello stesso ambiente. Cambiarla invalida i token emessi;
non generarla ad ogni riavvio come strategia di gestione delle sessioni.

Maven non carica `.env`. Compose invece lo legge dalla radice e richiede sia
`JWT_SECRET` sia `DB_PASSWORD` non vuoti. Non ci sono password PostgreSQL
predefinite; per Java fuori da Docker con profilo `postgres` configura anche
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e
`SPRING_DATASOURCE_PASSWORD`.

I test usano `application-test.yml`, che vive esclusivamente nelle risorse
di test: la chiave pubblica della fixture non viene inclusa nel JAR.
Il profilo runtime `h2` da solo non introduce segreti di fallback.

## Regole di accesso

- **Consultazione**: elenco, filtri, dettaglio e prossimi eventi restano pubblici.
- **Creazione**: autenticazione obbligatoria; il backend assegna il proprietario
  dall'account nel database. `ownerId` nel body non viene accettato in scrittura.
- **Modifica e cancellazione**: consentite al proprietario o a un ADMIN;
  gli altri account ricevono Problem Details con HTTP 403.
- **Eventi storici**: `ownerId` resta NULL; solo un ADMIN può gestirli.
  Non si presume chi li abbia creati e non si assegnano al primo utente.
- **Registrazione**: crea sempre USER, anche se il body prova a inviare ADMIN.
- **Ruoli**: risolti dal database ad ogni richiesta autenticata; una rimozione
  del ruolo ADMIN ha effetto anche su un JWT già emesso.

Il login restituisce il token e un profilo minimo `{id, username, role}`;
`GET /api/auth/me` restituisce lo stesso profilo se autenticati. Password e
hash non vengono restituiti. Il frontend usa il profilo per l'interfaccia,
ma il backend verifica comunque ogni operazione.

## Creare un amministratore offline

Non esistono amministratori o password amministrative predefiniti. La
procedura `admin-bootstrap` è disabilitata negli avvii normali e rifiuta
username già presenti: non promuove automaticamente un account registrato
da qualcun altro.

Scegli un username nuovo e una password privata di almeno 12 caratteri e
massimo 72 byte UTF-8. Per PostgreSQL imposta nel terminale le stesse variabili
datasource dell'ambiente da amministrare; fermati se non sei certo del database.

```powershell
$env:ADMIN_USERNAME = "username-amministratore-scelto"
$env:ADMIN_PASSWORD = [System.Net.NetworkCredential]::new(
  "", (Read-Host "Password amministratore" -AsSecureString)
).Password
cd demo
mvn clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar `
  --spring.main.web-application-type=none `
  --spring.profiles.active=postgres,admin-bootstrap
Remove-Item Env:ADMIN_PASSWORD
Remove-Item Env:ADMIN_USERNAME
```

`JWT_SECRET` deve già essere impostato. Per H2 su file ometti `postgres` e
usa `--spring.profiles.active=admin-bootstrap`, dalla cartella `demo`, con
il backend spento per evitare il lock del file. Se usi un URL H2 diverso,
imposta esplicitamente `SPRING_DATASOURCE_URL`. Non scegliere il profilo
`h2` temporaneo per un account che vuoi conservare.

Il comando non apre un server HTTP, salva la password con BCrypt e poi
termina. Riavvia normalmente il backend e accedi con il nuovo account.
Non attivare `admin-bootstrap` nel Compose o in un servizio sempre acceso.

## Schema versionato

V1 descrive lo schema precedente (`users` e `event`), per database nuovi
o come riferimento per un'adozione manuale verificata. V2 rende non nulli
username/password, aggiunge USER/ADMIN, `owner_id`, la chiave esterna e indici.
Gli account preesistenti diventano USER, mentre gli eventi restano senza autore.

Flyway mantiene versioni e checksum; `baseline-on-migrate` è false e `clean`
è disabilitato. Hibernate usa `validate`. Dopo il primo rilascio non cambiare
V1 o V2: aggiungi V3 e successive e provale su una copia del database.

## Database già popolati: procedura prima dell'aggiornamento

Non cancellare il file H2, non rimuovere il volume PostgreSQL e non usare
`ddl-auto: create` per far partire la nuova versione. L'avvio su uno schema
non vuoto privo di cronologia Flyway è rifiutato deliberatamente.

Prima dell'adozione occorre:

- **Backup e ripristino di prova**: conserva una copia indipendente del database
  e verifica che sia ripristinabile, con il backend vecchio ancora disponibile.
- **Confronto DDL**: confronta tabelle, tipi, lunghezze, identità, chiavi primarie
  e unicità username con V1. Una baseline non effettua questo confronto da sola.
- **Controllo dati**: cerca username/password NULL, username vuoti, con spazi
  iniziali/finali o duplicati; verifica hash BCrypt e compatibilità delle date.
  Risolvi eventuali anomalie su una copia, non modificando dati alla cieca.
- **Prova V2 su copia**: verifica conteggi, campi eventi, coordinate, hash e
  possibilità di login degli account; nessun account deve diventare ADMIN
  implicitamente e gli eventi storici devono mantenere `owner_id` NULL.
- **Finestra di manutenzione**: ferma le scritture durante la migrazione finale
  e conserva il backup per il ripristino. Il vecchio codice non deve usare
  `ddl-auto: update` sul database aggiornato come procedura di rollback.

Solo se lo schema e i dati risultano compatibili, puoi effettuare una baseline
manuale alla versione 1, poi applicare V2. Usa una CLI Flyway compatibile con
la versione gestita dal progetto, con driver del database disponibile:

```powershell
# Esegui dalla cartella demo su una COPIA verificata prima del database reale.
$env:FLYWAY_URL = $env:SPRING_DATASOURCE_URL
$env:FLYWAY_USER = $env:SPRING_DATASOURCE_USERNAME
$env:FLYWAY_PASSWORD = $env:SPRING_DATASOURCE_PASSWORD
$env:FLYWAY_LOCATIONS = "filesystem:src/main/resources/db/migration"
$env:FLYWAY_CLEAN_DISABLED = "true"
$env:FLYWAY_BASELINE_ON_MIGRATE = "false"
flyway -baselineVersion=1 baseline
flyway migrate
flyway validate
Remove-Item Env:FLYWAY_PASSWORD
```

Non eseguire questi comandi se il database non corrisponde a V1 o se contiene
già una cronologia Flyway: serve una procedura specifica, non un'altra baseline.
Per H2 configura esplicitamente URL/utente/password appropriati e lavora con
il database offline. Il pacchetto Maven non include una CLI Flyway separata.

### Backup PostgreSQL con Compose

Con il database avviato, dalla radice del repository:

```powershell
New-Item -ItemType Directory -Force backups | Out-Null
docker compose exec db pg_dump -U eventuser -d eventdb -Fc -f /tmp/events.dump
docker compose cp db:/tmp/events.dump ./backups/events.dump
docker compose exec db rm /tmp/events.dump
```

Il dump contiene dati e cronologia Flyway; conservalo in un luogo protetto
fuori da Git. La copia del file evita la redirezione binaria del dump tramite
PowerShell. Per H2 copia il file `demo/data/events.mv.db` con tutti i processi
che lo usano spenti e conserva la versione H2 necessaria al ripristino.

Non importare un dump sopra il database reale senza un piano di manutenzione:
prova prima il ripristino in un database separato e vuoto. I backup non sono
un'alternativa alla verifica della migrazione.

## Verifica e test PostgreSQL

I test ordinari non toccano il database locale:

```bash
cd demo
mvn --batch-mode verify
```

Le integrazioni richiedono un database dedicato chiamato esattamente
`in_citta_test`. Il validatore dell'URL rifiuta altri nomi; i test cancellano
gli eventi e gli account della fixture, quindi non usarlo per dati reali.

```powershell
$env:POSTGRES_IT_URL = "jdbc:postgresql://localhost:5432/in_citta_test"
$env:POSTGRES_IT_USER = "utente-test-dedicato"
$env:POSTGRES_IT_PASSWORD = [System.Net.NetworkCredential]::new(
  "", (Read-Host "Password database test" -AsSecureString)
).Password
mvn --batch-mode -Ppostgres-it verify
Remove-Item Env:POSTGRES_IT_PASSWORD
```

La CI usa PostgreSQL 16. Le integrazioni coprono permessi, filtri con date
assenti, migrazione V1→V2 senza perdita dei campi storici, rifiuto di schemi
non gestiti e persistenza di eventi/proprietari/ruoli al riavvio. Verificano
anche Compose senza credenziali e conservazione dei dati al riavvio del
container PostgreSQL; non avviano l'intero stack dei tre servizi Compose.

Sessione persistente, fusi orari, limiti di traffico sull'autenticazione,
monitoraggio, HTTPS e disponibilità dei servizi esterni restano separati da
questa modifica. Le correzioni non costituiscono un audit di sicurezza completo.
