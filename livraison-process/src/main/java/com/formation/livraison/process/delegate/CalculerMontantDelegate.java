package com.formation.livraison.process.delegate;

import com.formation.livraison.domain.model.Commande;
import com.formation.livraison.domain.service.CommandeService;
import com.formation.livraison.domain.service.PricingService;
import com.formation.livraison.process.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Calcule le montant total et l'expose en variable de processus.
 * La passerelle Gw_Montant s'appuie dessus (condition ${montant > 1000}).
 */
@Component("calculerMontant")
public class CalculerMontantDelegate implements JavaDelegate {

    private final PricingService pricingService;
    private final CommandeService commandeService;

    public CalculerMontantDelegate(PricingService pricingService, CommandeService commandeService) {
        this.pricingService = pricingService;
        this.commandeService = commandeService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String commandeId = (String) execution.getVariable(ProcessVariables.COMMANDE_ID);
        Commande commande = commandeService.getObligatoire(commandeId);

        double montant = pricingService.calculerMontant(commande.getProduit(), commande.getQuantite());

        execution.setVariable(ProcessVariables.MONTANT, montant);
        commandeService.enregistrerMontant(commandeId, montant);
    }
}
