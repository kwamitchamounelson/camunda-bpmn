package com.formation.livraison.process.delegate;

import com.formation.livraison.domain.model.Commande;
import com.formation.livraison.domain.model.StatutCommande;
import com.formation.livraison.domain.service.CommandeService;
import com.formation.livraison.domain.service.StockService;
import com.formation.livraison.process.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Cable sur Task_1 via camunda:delegateExpression="#{verifierDisponibilite}".
 * Le nom du bean doit correspondre exactement a l'expression du BPMN.
 */
@Component("verifierDisponibilite")
public class VerifierDisponibiliteDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(VerifierDisponibiliteDelegate.class);

    private final StockService stockService;
    private final CommandeService commandeService;

    public VerifierDisponibiliteDelegate(StockService stockService, CommandeService commandeService) {
        this.stockService = stockService;
        this.commandeService = commandeService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String commandeId = (String) execution.getVariable(ProcessVariables.COMMANDE_ID);
        Commande commande = commandeService.getObligatoire(commandeId);

        boolean disponible = stockService.estDisponible(commande.getProduit(), commande.getQuantite());

        // Ecrite en variable de processus : visible dans Cockpit et exploitable
        // par une passerelle exclusive si tu enrichis le modele plus tard.
        execution.setVariable(ProcessVariables.DISPONIBLE, disponible);

        commandeService.changerStatut(commandeId,
                disponible ? StatutCommande.STOCK_VERIFIE : StatutCommande.STOCK_INSUFFISANT);

        log.info("[{}] Task_1 terminee pour la commande {} (disponible={})",
                execution.getProcessInstanceId(), commandeId, disponible);
    }
}
