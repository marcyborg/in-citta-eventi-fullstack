// La preview Perplexity riscrive questo placeholder verso il backend Java nel sandbox.
// In locale e con Nginx le richieste restano relative e passano attraverso il proxy /api.
export const API_BASE =
  typeof window !== 'undefined' &&
  (window.location.hostname.endsWith('.pplx.app') || window.location.hostname === 'www.perplexity.ai')
    ? '__PORT_8081__'
    : '';
