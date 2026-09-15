package com.formation.livraison.process.rest;

import com.formation.livraison.domain.model.Commande;
import com.formation.livraison.domain.model.StatutCommande;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CommandeResponse", description = "Etat courant d'une commande et de son instance de processus")
public record CommandeResponse(

        @Schema(description = "Id de la commande, egalement businessKey de l'instance Camunda",
                example = "CMD-1A2B3C4D")
        String id,

        @Schema(example = "Alice")
        String client,

        @Schema(example = "alice@exemple.fr")
        String email,

        @Schema(example = "clavier")
        String produit,

        @Schema(example = "2")
        int quantite,

        @Schema(example = "STANDARD")
        String typeClient,

        @Schema(description = "Montant en EUR ; reste a 0 tant que le processus n'a pas calcule le prix",
                example = "90.0")
        double montant,

        @Schema(description = "Renseigne a l'expedition", example = "TRK-5F6E7D8C", nullable = true)
        String numeroSuivi,

        @Schema(description = "Statut fonctionnel, distinct de l'etat technique de l'instance",
                implementation = StatutCommande.class, example = "NOUVELLE")
        String statut,

        @Schema(description = "Id technique de l'instance de processus, utilisable dans Cockpit",
                example = "d4e3f2a1-0b9c-11ef-9f3a-0242ac110002", nullable = true)
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
