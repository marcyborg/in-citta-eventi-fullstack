# In Città | Frontend Angular (`event-frontend/`)

Questa interfaccia utilizza le API del
[backend Spring Boot collegato](../demo/README.md). La composizione dei due
servizi e le opzioni di avvio sono nel [README principale](../README.md).

Angular 20 e Leaflet: agenda cronologica, filtri combinabili, paginazione,
dettaglio con mappa, accesso e registrazione, creazione e modifica degli eventi.
Nel form è possibile cercare il luogo, posizionare il pin sulla mappa o
digitare coordinate. Per un evento già nel database senza coordinate, il
dettaglio offre una ricerca del luogo salvato senza alterare l'evento.

## Avvio locale

Richiede Node.js 20.19+ o 22 e il backend sulla porta 8080.

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
