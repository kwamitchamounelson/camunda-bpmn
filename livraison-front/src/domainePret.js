/**
 * Pendant de domaine.js pour le pret hypothecaire : traductions et regles
 * d'affichage uniquement. Les decisions (completude, risque, offre) restent
 * dans le backend : delegates Java et DMN risquePret.
 */

export const STATUTS_PRET = {
  DEPOSEE: { libelle: 'Déposée', ton: 'neutre' },
  INCOMPLETE: { libelle: 'Dossier incomplet', ton: 'alerte' },
  EN_ANALYSE: { libelle: 'En instruction', ton: 'info' },
  OFFRE_EMISE: { libelle: 'Offre émise', ton: 'info' },
  FONDS_DEBLOQUES: { libelle: 'Fonds débloqués', ton: 'succes' },
  OCTROYEE: { libelle: 'Prêt octroyé', ton: 'succes' },
  REFUSEE: { libelle: 'Refusé', ton: 'danger' },
  CLASSEE_SANS_SUITE: { libelle: 'Classé sans suite', ton: 'danger' },
  OFFRE_EXPIREE: { libelle: 'Offre expirée', ton: 'danger' },
  OFFRE_DECLINEE: { libelle: 'Offre déclinée', ton: 'alerte' },
};

export const statutPretLisible = (statut) => STATUTS_PRET[statut]?.libelle ?? statut;
export const tonStatutPret = (statut) => STATUTS_PRET[statut]?.ton ?? 'neutre';

export const RISQUES = {
  standard: { libelle: 'Risque standard', ton: 'succes' },
  eleve: { libelle: 'Risque élevé', ton: 'alerte' },
  refus: { libelle: 'Risque rédhibitoire', ton: 'danger' },
};

/** Un utilisateur de demo par groupe candidat (cf. PretIdentityBootstrap). */
const UTILISATEUR_PAR_GROUPE = {
  conseillers: 'conseiller',
  experts: 'expert',
  analystes: 'analyste',
  comiteCredit: 'comite',
  notaires: 'notaire',
};

export const UTILISATEURS_PRET = ['conseiller', 'expert', 'analyste', 'comite', 'notaire', 'demo'];

export const utilisateurPretSuggere = (groupes = []) =>
  UTILISATEUR_PAR_GROUPE[groupes[0]] ?? 'demo';

/**
 * Jeux de donnees qui menent chacun a une branche differente du BPMN.
 * Les profils "Paul Fiche" et "Lea Moyenne" sont reconnus par la centrale
 * des risques simulee (CentraleRisquesService).
 */
const BASE = { email: '', montantDemande: 200000, dureeMois: 240, apport: 40000, valeurBien: 240000 };

export const SCENARIOS_PRET = [
  {
    cle: 'solide',
    icone: '✅',
    titre: 'Dossier solide',
    effet: 'Endettement ~19 %, bon score : décision de l’analyste.',
    demande: { ...BASE, emprunteur: 'Alice Martin', revenusMensuels: 6000 },
  },
  {
    cle: 'endettement',
    icone: '⚖️',
    titre: 'Endettement 37 %',
    effet: 'Au-delà de 35 % (HCSF) : passage en comité de crédit.',
    demande: { ...BASE, emprunteur: 'Bruno Durand', revenusMensuels: 3150 },
  },
  {
    cle: 'score',
    icone: '📉',
    titre: 'Score moyen',
    effet: 'Score 640 (< 700) : passage en comité de crédit.',
    demande: { ...BASE, emprunteur: 'Lea Moyenne', revenusMensuels: 6000 },
  },
  {
    cle: 'incident',
    icone: '⛔',
    titre: 'Fichier des incidents',
    effet: 'Refus automatique par le DMN, après l’expertise.',
    demande: { ...BASE, emprunteur: 'Paul Fiche', revenusMensuels: 6000 },
  },
  {
    cle: 'incomplet',
    icone: '📎',
    titre: 'Dossier incomplet',
    effet: 'Revenus manquants : tâche « Compléter le dossier ».',
    demande: { ...BASE, emprunteur: 'Chloe Petit', revenusMensuels: 0 },
  },
];

/** Libelles des variables Camunda affichees dans la fiche d'un dossier. */
export const LIBELLES_VARIABLES = {
  dossierComplet: 'Dossier complet',
  piecesManquantes: 'Pièces manquantes',
  scoreCredit: 'Score crédit',
  incidentBancaire: 'Incident bancaire',
  mensualiteEstimee: 'Mensualité estimée (3,5 %)',
  tauxEndettement: 'Taux d’endettement (%)',
  valeurExpertisee: 'Valeur expertisée',
  niveauRisque: 'Niveau de risque (DMN)',
  pretAccorde: 'Prêt accordé',
  tauxPropose: 'Taux proposé',
  motifRefus: 'Motif du refus',
  tauxAnnuel: 'Taux de l’offre (%)',
  mensualite: 'Mensualité de l’offre',
  offreAcceptee: 'Offre acceptée',
  dateSignature: 'Date de signature',
  numeroContrat: 'Numéro de contrat',
};

const STATUTS_FINAUX_ECHEC = ['REFUSEE', 'CLASSEE_SANS_SUITE', 'OFFRE_EXPIREE', 'OFFRE_DECLINEE'];

export const estTermine = (demande) =>
  demande.statut === 'OCTROYEE' || STATUTS_FINAUX_ECHEC.includes(demande.statut);

/**
 * Frise du dossier. Comme pour la livraison, l'etape courante se deduit du
 * statut ET de la tache active (ou du timer en attente) : le statut seul ne
 * dit pas si l'instance attend l'expert, l'analyste ou le comite.
 */
export function etapesPret(demande, tacheActive) {
  const { statut, variables = {}, timers = [] } = demande;
  const cle = tacheActive?.definitionKey;
  const ordre = ['depot', 'completude', 'analyses', 'decision', 'offre', 'signature', 'octroi'];

  let courante;
  if (statut === 'OCTROYEE') courante = 'fin';
  else if (statut === 'CLASSEE_SANS_SUITE' || cle === 'Task_Completer') courante = 'completude';
  else if (cle === 'Task_Expertise') courante = 'analyses';
  else if (cle === 'Task_Analyse' || cle === 'Task_Comite') courante = 'decision';
  else if (statut === 'REFUSEE') courante = 'decision';
  else if (cle === 'Task_Signature' || statut === 'FONDS_DEBLOQUES') courante = 'signature';
  else if (statut === 'OFFRE_EMISE' || statut === 'OFFRE_EXPIREE' || statut === 'OFFRE_DECLINEE')
    courante = 'offre';
  else courante = 'depot';

  const echec = STATUTS_FINAUX_ECHEC.includes(statut);
  const indexCourant = courante === 'fin' ? ordre.length : ordre.indexOf(courante);
  const enReflexion = timers.some((t) => t.activiteId === 'Timer_Reflexion');

  const libelles = {
    depot: 'Demande déposée',
    completude: statut === 'CLASSEE_SANS_SUITE' ? 'Classé sans suite' : 'Complétude',
    analyses: cle === 'Task_Expertise' ? 'Expertise du bien' : 'Analyses',
    decision:
      statut === 'REFUSEE'
        ? 'Prêt refusé'
        : cle === 'Task_Comite'
          ? 'Comité de crédit'
          : cle === 'Task_Analyse'
            ? 'Analyse du dossier'
            : `Décision${variables.niveauRisque ? ` (${RISQUES[variables.niveauRisque]?.libelle ?? ''})` : ''}`,
    offre:
      statut === 'OFFRE_EXPIREE'
        ? 'Offre expirée'
        : statut === 'OFFRE_DECLINEE'
          ? 'Offre déclinée'
          : enReflexion
            ? 'Délai de réflexion'
            : cle === 'Task_Acceptation'
              ? 'Attente d’acceptation'
              : 'Offre',
    signature: cle === 'Task_Signature' ? 'Signature chez le notaire' : 'Signature',
    octroi: 'Prêt octroyé',
  };

  return ordre.map((etape, index) => {
    let etat = 'attente';
    if (index < indexCourant) etat = 'fait';
    else if (index === indexCourant) etat = echec ? 'refuse' : 'encours';
    // Le depot est fait des que le processus a avance d'un cran.
    if (etape === 'depot' && statut !== 'DEPOSEE') etat = 'fait';
    return { cle: etape, libelle: libelles[etape], etat };
  });
}
