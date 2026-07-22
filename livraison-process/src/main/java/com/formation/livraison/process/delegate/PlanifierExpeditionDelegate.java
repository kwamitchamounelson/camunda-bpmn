package com.formation.livraison.process.delegate;

import com.formation.livraison.domain.model.StatutCommande;
import com.formation.livraison.domain.service.CommandeService;
import com.formation.livraison.process.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Branche parallele de Gw_Par1 : genere un numero de suivi et passe la
 * commande en EXPEDIEE. S'execute en meme temps que l'envoi de l'email.
 */
@Component("planifierExpedition")
public class PlanifierExpeditionDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PlanifierExpeditionDelegate.class);

    private final CommandeService commandeService;

    public PlanifierExpeditionDelegate(CommandeService commandeService) {
        this.commandeService = commandeService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String commandeId = (String) execution.getVariable(ProcessVariables.COMMANDE_ID);

        String numeroSuivi = "TRK-" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
        commandeService.enregistrerSuivi(commandeId, numeroSuivi);
        commandeService.changerStatut(commandeId, StatutCommande.EXPEDIEE);

        log.info("Expedition planifiee pour {} : numero de suivi {}", commandeId, numeroSuivi);
    }
}
