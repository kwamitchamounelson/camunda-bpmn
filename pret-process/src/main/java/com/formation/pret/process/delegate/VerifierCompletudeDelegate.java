package com.formation.pret.process.delegate;

import com.formation.pret.domain.model.StatutPret;
import com.formation.pret.domain.service.DemandePretService;
import com.formation.pret.process.PretVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.formation.pret.process.PretVariables.longOuZero;

/**
 * Pose ${dossierComplet} pour la passerelle Gw_Complet.
 * Rejouee apres chaque "Completer le dossier" : elle relit donc les variables,
 * qui ont pu etre corrigees par le conseiller, et les reporte sur la demande.
 */
@Component("verifierCompletude")
public class VerifierCompletudeDelegate implements JavaDelegate {

    private final DemandePretService demandePretService;

    public VerifierCompletudeDelegate(DemandePretService demandePretService) {
        this.demandePretService = demandePretService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> variables = execution.getVariables();
        String demandeId = (String) variables.get(PretVariables.DEMANDE_ID);
        long revenus = longOuZero(variables, PretVariables.REVENUS_MENSUELS);
        long valeurBien = longOuZero(variables, PretVariables.VALEUR_BIEN);

        List<String> manquantes = demandePretService.piecesManquantes(
                (String) variables.get(PretVariables.EMPRUNTEUR),
                longOuZero(variables, PretVariables.MONTANT_DEMANDE),
                (int) longOuZero(variables, PretVariables.DUREE_MOIS),
                revenus,
                valeurBien);

        demandePretService.majInformations(demandeId, revenus, valeurBien);
        demandePretService.changerStatut(demandeId,
                manquantes.isEmpty() ? StatutPret.EN_ANALYSE : StatutPret.INCOMPLETE);

        execution.setVariable(PretVariables.DOSSIER_COMPLET, manquantes.isEmpty());
        execution.setVariable(PretVariables.PIECES_MANQUANTES, String.join(", ", manquantes));
    }
}
