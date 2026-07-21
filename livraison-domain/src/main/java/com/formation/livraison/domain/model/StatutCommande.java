package com.formation.livraison.domain.model;

/**
 * Cycle de vie fonctionnel d'une commande.
 * Il est volontairement distinct de l'etat technique de l'instance de processus Camunda.
 */
public enum StatutCommande {

    NOUVELLE,
    STOCK_VERIFIE,
    STOCK_INSUFFISANT,
    PREPAREE,
    CONFIRMEE,
    LIVREE
}
