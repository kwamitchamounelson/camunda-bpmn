package com.formation.livraison.process;

/**
 * Noms des variables de processus centralises : evite les chaines
 * magiques dispersees entre le BPMN, les delegates et le REST.
 */
public final class ProcessVariables {

    public static final String PROCESS_KEY = "ProcessusLivraison";

    /** Ids des taches utilisateur du BPMN, utilises par l'API REST pour cibler la bonne tache. */
    public static final String TASK_VALIDATION = "Task_Validation";
    public static final String TASK_PREPARATION = "Task_Prepa";

    public static final String COMMANDE_ID = "commandeId";
    public static final String PRODUIT = "produit";
    public static final String QUANTITE = "quantite";
    public static final String CLIENT = "client";
    public static final String DISPONIBLE = "disponible";
    public static final String MONTANT = "montant";
    public static final String TYPE_CLIENT = "typeClient";
    public static final String VALIDATEUR = "validateur";
    public static final String APPROUVE = "approuve";
    public static final String MOTIF_REFUS = "motifRefus";

    private ProcessVariables() {
    }
}
