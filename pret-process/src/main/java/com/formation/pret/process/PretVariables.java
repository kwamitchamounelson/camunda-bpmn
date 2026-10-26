package com.formation.pret.process;

import java.util.Map;

/**
 * Pendant de ProcessVariables (livraison) pour le processus de pret :
 * noms partages entre le BPMN, le DMN, les delegates et le REST.
 */
public final class PretVariables {

    public static final String PROCESS_KEY = "ProcessusPretHypothecaire";

    /** Ids des taches utilisateur du BPMN. */
    public static final String TASK_COMPLETER = "Task_Completer";
    public static final String TASK_EXPERTISE = "Task_Expertise";
    public static final String TASK_ANALYSE = "Task_Analyse";
    public static final String TASK_COMITE = "Task_Comite";
    public static final String TASK_ACCEPTATION = "Task_Acceptation";
    public static final String TASK_SIGNATURE = "Task_Signature";

    // Saisies a la demande
    public static final String DEMANDE_ID = "demandeId";
    public static final String EMPRUNTEUR = "emprunteur";
    public static final String MONTANT_DEMANDE = "montantDemande";
    public static final String DUREE_MOIS = "dureeMois";
    public static final String REVENUS_MENSUELS = "revenusMensuels";
    public static final String APPORT = "apport";
    public static final String VALEUR_BIEN = "valeurBien";

    // Calculees par les delegates
    public static final String DOSSIER_COMPLET = "dossierComplet";
    public static final String PIECES_MANQUANTES = "piecesManquantes";
    public static final String SCORE_CREDIT = "scoreCredit";
    public static final String INCIDENT_BANCAIRE = "incidentBancaire";
    public static final String MENSUALITE_ESTIMEE = "mensualiteEstimee";
    public static final String TAUX_ENDETTEMENT = "tauxEndettement";
    public static final String NIVEAU_RISQUE = "niveauRisque";
    public static final String TAUX_ANNUEL = "tauxAnnuel";
    public static final String MENSUALITE = "mensualite";
    public static final String NUMERO_CONTRAT = "numeroContrat";

    // Saisies dans les taches utilisateur
    public static final String VALEUR_EXPERTISEE = "valeurExpertisee";
    public static final String PRET_ACCORDE = "pretAccorde";
    public static final String TAUX_PROPOSE = "tauxPropose";
    public static final String MOTIF_REFUS = "motifRefus";
    public static final String OFFRE_ACCEPTEE = "offreAcceptee";

    /** Valeur de sortie du DMN risquePret qui court-circuite toute decision humaine. */
    public static final String RISQUE_REDHIBITOIRE = "refus";

    private PretVariables() {
    }

    /** Lit une variable numerique quel que soit son type (Integer depuis JSON, Long depuis un formulaire). */
    public static long longOuZero(Map<String, Object> variables, String nom) {
        return variables.get(nom) instanceof Number nombre ? nombre.longValue() : 0L;
    }
}
