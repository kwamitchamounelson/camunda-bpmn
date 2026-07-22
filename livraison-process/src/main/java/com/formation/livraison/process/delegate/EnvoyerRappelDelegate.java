package com.formation.livraison.process.delegate;

import com.formation.livraison.process.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Declenche par l'evenement frontiere non interruptif Boundary_Rappel :
 * si la validation traine, un rappel part sans interrompre la tache en cours.
 */
@Component("envoyerRappel")
public class EnvoyerRappelDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(EnvoyerRappelDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String commandeId = (String) execution.getVariable(ProcessVariables.COMMANDE_ID);
        log.warn("RAPPEL : la commande {} attend toujours une validation manager", commandeId);
    }
}
