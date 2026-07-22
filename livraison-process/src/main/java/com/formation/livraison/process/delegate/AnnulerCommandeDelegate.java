package com.formation.livraison.process.delegate;

import com.formation.livraison.domain.service.CommandeService;
import com.formation.livraison.process.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Branche "rejet" de la validation manager : consigne le motif de refus
 * et cloture la commande avant l'evenement de fin End_Refus.
 */
@Component("annulerCommande")
public class AnnulerCommandeDelegate implements JavaDelegate {

    private final CommandeService commandeService;

    public AnnulerCommandeDelegate(CommandeService commandeService) {
        this.commandeService = commandeService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String commandeId = (String) execution.getVariable(ProcessVariables.COMMANDE_ID);
        String motif = (String) execution.getVariable(ProcessVariables.MOTIF_REFUS);
        commandeService.enregistrerRefus(commandeId, motif != null ? motif : "Non precise");
    }
}
