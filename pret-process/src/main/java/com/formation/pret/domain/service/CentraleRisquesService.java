package com.formation.pret.domain.service;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Simule l'interrogation du fichier national des incidents de remboursement
 * et d'un bureau de score. Quelques emprunteurs connus permettent de jouer
 * les branches "risque" du processus ; tout autre nom obtient un bon score.
 */
@Service
public class CentraleRisquesService {

    public record ResultatCentrale(int score, boolean incident) {
    }

    private static final ResultatCentrale PROFIL_PAR_DEFAUT = new ResultatCentrale(780, false);

    private static final Map<String, ResultatCentrale> PROFILS_CONNUS = Map.of(
            "paul fiche", new ResultatCentrale(320, true),
            "lea moyenne", new ResultatCentrale(640, false));

    public ResultatCentrale consulter(String emprunteur) {
        String cle = emprunteur == null ? "" : emprunteur.trim().toLowerCase();
        return PROFILS_CONNUS.getOrDefault(cle, PROFIL_PAR_DEFAUT);
    }
}
