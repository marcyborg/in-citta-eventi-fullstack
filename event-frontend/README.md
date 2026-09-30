# In Città | Agenda e mappa eventi con Angular

Componente frontend in `event-frontend/`: questa interfaccia utilizza le API del
[backend Spring Boot collegato](../demo/README.md). La composizione dei due
servizi e le opzioni di avvio sono nel [README principale](../README.md).

Angular 20 e Leaflet: agenda cronologica, filtri combinabili, paginazione,
dettaglio con mappa, accesso e registrazione, creazione e modifica degli eventi.
Nel form è possibile cercare il luogo, posizionare il pin sulla mappa o
digitare coordinate. Per un evento già nel database senza coordinate, il
dettaglio offre una ricerca del luogo salvato senza alterare l'evento.

## Avvio locale

Richiede Node.js 20.19+ della serie 20 oppure 22.12+ della serie 22,
con npm e il backend sulla porta 8080.

```bash
npm ci
npm start
```

Apri `http://localhost:4200/`. Il proxy di sviluppo inoltra `/api` al
backend. Per compilare o testare:

```bash
npm run build
npm test -- --watch=false --browsers=ChromeHeadless
```

I test richiedono Chrome/Chromium, impostabile con `CHROME_BIN`. Il token JWT
è conservato solo in memoria: aggiornando la pagina occorre accedere di nuovo.
In Docker, Nginx inoltra `/api` al servizio `backend`. `node_modules/`,
`dist/` e gli output JavaScript generati sono esclusi da Git.

## Mappa e limiti

Le mappe caricano riquadri OpenStreetMap; la ricerca del luogo passa dal
backend a Nominatim solo dopo un'azione esplicita. La selezione manuale
delle coordinate resta disponibile anche senza geocodifica, ma il caricamento
della mappa dipende dalla connettività.

Una posizione trovata nel dettaglio di un evento senza coordinate non viene
salvata automaticamente. Il login riceve anche il profilo USER/ADMIN;
modifica e cancellazione sono mostrate solo al proprietario o a un ADMIN.
Un accesso diretto al form di modifica non autorizzato mostra un messaggio
senza permettere l'invio. Il backend resta la fonte autorevole dei permessi:
nascondere un pulsante non sostituisce l'autorizzazione delle API.
Le precauzioni e i limiti
complessivi sono nel [README principale](../README.md).
