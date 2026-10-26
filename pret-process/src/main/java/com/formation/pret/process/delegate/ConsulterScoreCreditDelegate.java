package com.formation.pret.process.delegate;

import com.formation.pret.domain.service.CentraleRisquesService;
import com.formation.pret.domain.service.CentraleRisquesService.ResultatCentrale;
import com.formation.pret.process.PretVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Branche parallele 1 : fournit scoreCredit et incidentBancaire au DMN risquePret.
 */
@Component("consulterScoreCredit")
public class ConsulterScoreCreditDelegate implements JavaDelegate {

    private final CentraleRisquesService centraleRisquesService;

    public ConsulterScoreCreditDelegate(CentraleRisquesService centraleRisquesService) {
        this.centraleRisquesService = centraleRisquesService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        ResultatCentrale resultat = centraleRisquesService.consulter(
                (String) execution.getVariable(PretVariables.EMPRUNTEUR));

        execution.setVariable(PretVariables.SCORE_CREDIT, resultat.score());
        execution.setVariable(PretVariables.INCIDENT_BANCAIRE, resultat.incident());
    }
}
