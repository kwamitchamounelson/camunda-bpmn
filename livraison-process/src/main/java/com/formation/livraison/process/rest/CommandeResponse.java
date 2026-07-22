package com.formation.livraison.process.rest;

import com.formation.livraison.domain.model.Commande;

public record CommandeResponse(
        String id,
        String client,
        String email,
        String produit,
        int quantite,
        String typeClient,
        double montant,
        String numeroSuivi,
        String statut,
        String processInstanceId) {

    public static CommandeResponse from(Commande commande) {
        return new CommandeResponse(
                commande.getId(),
                commande.getClient(),
                commande.getEmail(),
                commande.getProduit(),
                commande.getQuantite(),
                commande.getTypeClient(),
                commande.getMontant(),
                commande.getNumeroSuivi(),
                commande.getStatut().name(),
                commande.getProcessInstanceId());
    }
}
