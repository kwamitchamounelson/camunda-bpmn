package com.formation.livraison.process.delegate;

import com.formation.livraison.domain.model.Commande;
import com.formation.livraison.domain.model.StatutCommande;
import com.formation.livraison.domain.service.CommandeService;
import com.formation.livraison.domain.service.EmailService;
import com.formation.livraison.process.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Cable sur Task_3 via camunda:delegateExpression="#{envoyerEmailConfirmation}".
 */
@Component("envoyerEmailConfirmation")
public class EnvoyerEmailConfirmationDelegate implements JavaDelegate {

    private final EmailService emailService;
    private final CommandeService commandeService;

    public EnvoyerEmailConfirmationDelegate(EmailService emailService, CommandeService commandeService) {
        this.emailService = emailService;
        this.commandeService = commandeService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String commandeId = (String) execution.getVariable(ProcessVariables.COMMANDE_ID);
        Commande commande = commandeService.getObligatoire(commandeId);

        emailService.envoyerConfirmation(commande);
        commandeService.changerStatut(commandeId, StatutCommande.CONFIRMEE);
    }
}
