package com.formation.pret.process.rest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreerDemandePretRequest", description = "Dossier depose par l'emprunteur")
public record CreerDemandePretRequest(

        @Schema(description = """
                Nom de l'emprunteur. Deux noms ont un profil simule particulier :
                "Paul Fiche" (incident bancaire => refus automatique) et
                "Lea Moyenne" (score 640 => comite de credit).""",
                example = "Alice Martin", requiredMode = Schema.RequiredMode.REQUIRED)
        String emprunteur,

        @Schema(example = "alice.martin@exemple.fr")
        String email,

        @Schema(description = "Capital emprunte (EUR)", example = "200000")
        long montantDemande,

        @Schema(description = "Duree du pret en mois", example = "240")
        int dureeMois,

        @Schema(description = "Revenus mensuels nets (EUR). 0 => dossier incomplet", example = "6000")
        long revenusMensuels,

        @Schema(description = "Apport personnel (EUR)", example = "40000")
        long apport,

        @Schema(description = "Prix du bien (EUR). 0 => dossier incomplet", example = "240000")
        long valeurBien) {
}
