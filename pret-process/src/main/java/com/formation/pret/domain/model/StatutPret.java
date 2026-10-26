package com.formation.pret.domain.model;

/**
 * Statut fonctionnel du dossier, pose par les delegates et les listeners de fin.
 * Il ne dit pas quelle tache attend : c'est la tache active de l'instance qui
 * le precise (expertise, analyse, comite, acceptation, signature).
 */
public enum StatutPret {
    DEPOSEE,
    INCOMPLETE,
    EN_ANALYSE,
    OFFRE_EMISE,
    FONDS_DEBLOQUES,
    OCTROYEE,
    REFUSEE,
    CLASSEE_SANS_SUITE,
    OFFRE_EXPIREE,
    OFFRE_DECLINEE
}
