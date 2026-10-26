package com.formation.pret.process.delegate;

import com.formation.pret.domain.service.DemandePretService;
import com.formation.pret.process.PretVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Derniere etape : le contrat recoit son numero et le pret est octroye.
 */
@Component("mettreEnPlaceEcheancier")
public class MettreEnPlaceEcheancierDelegate implements JavaDelegate {

    private final DemandePretService demandePretService;

    public MettreEnPlaceEcheancierDelegate(DemandePretService demandePretService) {
        this.demandePretService = demandePretService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String demandeId = (String) execution.getVariable(PretVariables.DEMANDE_ID);
        String numeroContrat = "CTR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        execution.setVariable(PretVariables.NUMERO_CONTRAT, numeroContrat);
        demandePretService.octroyer(demandeId, numeroContrat);
    }
}
