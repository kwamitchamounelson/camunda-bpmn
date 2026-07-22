package com.formation.livraison.domain.model;

/**
 * Cycle de vie fonctionnel d'une commande.
 * Volontairement distinct de l'etat technique de l'instance de processus Camunda.
 */
public enum StatutCommande {

    NOUVELLE,
    STOCK_VERIFIE,
    STOCK_INSUFFISANT,
    EN_REAPPROVISIONNEMENT,
    EN_VALIDATION,
    VALIDEE,
    REFUSEE,
    PREPAREE,
    CONFIRMEE,
    EXPEDIEE,
    LIVREE
}
