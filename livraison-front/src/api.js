/**
 * Unique point de contact avec le backend.
 *
 * Deux principes :
 *  - tout passe par l'API REST /api/**, aucune logique metier cote front ;
 *  - chaque appel est publie dans un journal, affiche pendant la demo pour
 *    montrer l'appel HTTP derriere chaque clic. Les rafraichissements
 *    automatiques sont marques "silencieux" pour ne pas noyer ce journal.
 */

const BACKEND = import.meta.env.VITE_BACKEND ?? 'http://localhost:8080';

const abonnes = new Set();
let compteur = 0;

export function ecouterAppels(callback) {
  abonnes.add(callback);
  return () => abonnes.delete(callback);
}

export class ErreurApi extends Error {
  constructor(message, statut) {
    super(message);
    this.statut = statut;
  }
}

async function appeler(methode, chemin, corps, { silencieux = false } = {}) {
  const debut = performance.now();
  const options = { method: methode, headers: {} };
  if (corps !== undefined) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(corps);
  }

  let statut = 0;
  let donnees = null;
  let erreur = null;

  try {
    const reponse = await fetch(chemin, options);
    statut = reponse.status;
    const texte = await reponse.text();
    donnees = texte ? JSON.parse(texte) : null;
    if (!reponse.ok) {
      // Le RestExceptionHandler du backend renvoie { "message": "..." }.
      erreur = donnees?.message ?? `HTTP ${statut}`;
    }
  } catch (e) {
    erreur = e instanceof SyntaxError ? 'Reponse illisible du backend' : 'Backend injoignable';
  }

  if (!silencieux) {
    const entree = {
      id: ++compteur,
      methode,
      chemin,
      statut,
      erreur,
      duree: Math.round(performance.now() - debut),
      horodatage: new Date(),
    };
    abonnes.forEach((callback) => callback(entree));
  }

  if (erreur) throw new ErreurApi(erreur, statut);
  return donnees;
}

export const api = {
  stocks: (options) => appeler('GET', '/api/stocks', undefined, options),
  definirStock: (produit, quantite) =>
    appeler('PUT', `/api/stocks/${encodeURIComponent(produit)}`, { quantite }),
  mouvementStock: (produit, mouvement) =>
    appeler('POST', `/api/stocks/${encodeURIComponent(produit)}/mouvements`, { mouvement }),

  commandes: (options) => appeler('GET', '/api/commandes', undefined, options),
  creerCommande: (commande) => appeler('POST', '/api/commandes', commande),

  taches: (options) => appeler('GET', '/api/taches', undefined, options),
  attribuerTache: (taskId, utilisateur) =>
    appeler('POST', `/api/taches/${taskId}/attribuer`, { utilisateur }),
  libererTache: (taskId) => appeler('POST', `/api/taches/${taskId}/liberer`),
  deciderTache: (taskId, approuve, motifRefus) =>
    appeler('POST', `/api/taches/${taskId}/decider`, { approuve, motifRefus }),
  completerTache: (taskId, variables = {}) =>
    appeler('POST', `/api/taches/${taskId}/completer`, { variables }),

  // Processus de pret hypothecaire : meme moteur, API separee sous /api/prets.
  prets: (options) => appeler('GET', '/api/prets', undefined, options),
  deposerPret: (demande) => appeler('POST', '/api/prets', demande),
  declencherTimer: (jobId) => appeler('POST', `/api/prets/timers/${jobId}/declencher`),
  tachesPret: (options) => appeler('GET', '/api/prets/taches', undefined, options),
  attribuerTachePret: (taskId, utilisateur) =>
    appeler('POST', `/api/prets/taches/${taskId}/attribuer`, { utilisateur }),
  libererTachePret: (taskId) => appeler('POST', `/api/prets/taches/${taskId}/liberer`),
  completerTachePret: (taskId, variables = {}) =>
    appeler('POST', `/api/prets/taches/${taskId}/completer`, { variables }),
};

/** Lien direct vers l'instance dans Cockpit : le coeur de la demo cote moteur. */
export const urlInstanceCockpit = (processInstanceId) =>
  `${BACKEND}/camunda/app/cockpit/default/#/process-instance/${processInstanceId}`;

export const urlCockpit = `${BACKEND}/camunda/app/cockpit/default/#/dashboard`;
export const urlTasklist = `${BACKEND}/camunda/app/tasklist/default/`;
