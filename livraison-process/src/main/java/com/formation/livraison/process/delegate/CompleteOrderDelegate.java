package com.formation.livraison.process.delegate;

import com.formation.livraison.domain.model.StatutCommande;
import com.formation.livraison.domain.service.CommandeService;
import com.formation.livraison.process.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Cable sur Task_4 via camunda:delegateExpression="#{completeOrder}".
 */
@Component("completeOrder")
public class CompleteOrderDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CompleteOrderDelegate.class);

    private final CommandeService commandeService;

    public CompleteOrderDelegate(CommandeService commandeService) {
        this.commandeService = commandeService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String commandeId = (String) execution.getVariable(ProcessVariables.COMMANDE_ID);
        commandeService.changerStatut(commandeId, StatutCommande.LIVREE);
        log.info("[{}] Commande {} livree, fin du processus",
                execution.getProcessInstanceId(), commandeId);
    }
}
