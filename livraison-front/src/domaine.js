/**
 * Traductions et regles d'affichage, calquees sur ce que le backend produit
 * reellement : seuls les statuts effectivement poses par les delegates
 * apparaissent ici (NOUVELLE, STOCK_VERIFIE, STOCK_INSUFFISANT,
 * EN_REAPPROVISIONNEMENT, CONFIRMEE, EXPEDIEE, LIVREE, REFUSEE).
 */

export const STATUTS = {
  NOUVELLE: { libelle: 'Commande reçue', ton: 'neutre' },
  STOCK_VERIFIE: { libelle: 'Stock vérifié', ton: 'info' },
  STOCK_INSUFFISANT: { libelle: 'Rupture de stock', ton: 'alerte' },
  EN_REAPPROVISIONNEMENT: { libelle: 'Réapprovisionnement', ton: 'alerte' },
  EN_VALIDATION: { libelle: 'En validation', ton: 'info' },
  VALIDEE: { libelle: 'Validée', ton: 'info' },
  PREPAREE: { libelle: 'Préparée', ton: 'info' },
  CONFIRMEE: { libelle: 'Confirmée', ton: 'info' },
  EXPEDIEE: { libelle: 'Expédiée', ton: 'succes' },
  LIVREE: { libelle: 'Livrée', ton: 'succes' },
  REFUSEE: { libelle: 'Refusée', ton: 'danger' },
};

export const statutLisible = (statut) => STATUTS[statut]?.libelle ?? statut;
export const tonStatut = (statut) => STATUTS[statut]?.ton ?? 'neutre';

const VIGNETTES = {
  clavier: '⌨️',
  souris: '🖱️',
  ecran: '🖥️',
  casque: '🎧',
};

export const vignette = (produit) => VIGNETTES[produit?.toLowerCase()] ?? '📦';

export const euros = (montant) =>
  new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(montant ?? 0);

/** Utilisateur propose par defaut pour prendre une tache, selon son groupe candidat. */
const UTILISATEUR_PAR_GROUPE = {
  logistique: 'employeLogistique',
  managers: 'manager',
  directeurs: 'directeur',
};

export const UTILISATEURS = ['employeLogistique', 'manager', 'directeur', 'demo'];

export const utilisateurSuggere = (groupes = []) =>
  UTILISATEUR_PAR_GROUPE[groupes[0]] ?? 'demo';

export const estValidation = (tache) => tache.definitionKey === 'Task_Validation';

/**
 * Suivi facon e-commerce. L'etat se deduit du statut de la commande ET de la
 * tache active : c'est la tache qui dit si l'instance attend une validation
 * ou une preparation, le statut seul ne le raconte pas.
 */
export function etapesSuivi(commande, tacheActive) {
  const statut = commande.statut;
  const refusee = statut === 'REFUSEE';
  const livree = statut === 'LIVREE';
  const expediee = statut === 'EXPEDIEE' || statut === 'CONFIRMEE' || livree;
  const enRupture = statut === 'STOCK_INSUFFISANT' || statut === 'EN_REAPPROVISIONNEMENT';
  const enValidation = tacheActive?.definitionKey === 'Task_Validation';
  const enPreparation = tacheActive?.definitionKey === 'Task_Prepa';

  const stockOk = !enRupture && statut !== 'NOUVELLE';
  const validationFaite = !enValidation && (enPreparation || expediee || refusee);
  const preparationFaite = expediee;

  return [
    { cle: 'recue', libelle: 'Commande reçue', etat: 'fait' },
    {
      cle: 'stock',
      libelle: enRupture ? 'En attente de réapprovisionnement' : 'Stock vérifié',
      etat: enRupture ? 'encours' : stockOk ? 'fait' : 'attente',
    },
    {
      cle: 'validation',
      libelle: enValidation ? 'Validation en cours' : 'Validation',
      etat: enValidation ? 'encours' : validationFaite ? 'fait' : 'attente',
    },
    {
      cle: 'preparation',
      libelle: enPreparation ? 'Préparation en cours' : 'Préparation',
      etat: enPreparation ? 'encours' : preparationFaite ? 'fait' : 'attente',
    },
    {
      cle: 'expedition',
      libelle: 'Expédition',
      etat: expediee ? 'fait' : 'attente',
    },
    {
      cle: 'livraison',
      libelle: refusee ? 'Commande refusée' : 'Livrée',
      etat: refusee ? 'refuse' : livree ? 'fait' : 'attente',
    },
  ];
}
