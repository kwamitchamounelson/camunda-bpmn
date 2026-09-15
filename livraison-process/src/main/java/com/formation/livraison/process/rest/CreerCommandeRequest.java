package com.formation.livraison.process.rest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreerCommandeRequest", description = "Donnees necessaires au demarrage d'une livraison")
public record CreerCommandeRequest(

        @Schema(description = "Nom du client", example = "Alice", requiredMode = Schema.RequiredMode.REQUIRED)
        String client,

        @Schema(description = "Email de confirmation", example = "alice@exemple.fr")
        String email,

        @Schema(description = "Produit commande ; un produit hors catalogue est facture au tarif par defaut",
                example = "clavier", allowableValues = {"clavier", "souris", "ecran", "casque"})
        String produit,

        @Schema(description = "Quantite commandee ; au-dela du stock disponible le processus part en reapprovisionnement",
                example = "2")
        int quantite,

        @Schema(description = "Pilote la decision DMN niveauValidation. Vide ou absent => STANDARD.",
                example = "STANDARD", allowableValues = {"STANDARD", "VIP"})
        String typeClient) {
}
