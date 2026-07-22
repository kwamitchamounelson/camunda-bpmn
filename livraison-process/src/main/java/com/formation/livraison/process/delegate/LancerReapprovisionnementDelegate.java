package com.formation.livraison.process.delegate;

import com.formation.livraison.domain.model.Commande;
import com.formation.livraison.domain.model.StatutCommande;
import com.formation.livraison.domain.service.CommandeService;
import com.formation.livraison.domain.service.StockService;
import com.formation.livraison.process.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Branche "rupture" de Gw_Stock. Reapprovisionne puis le processus attend
 * (Timer_Reappro) avant de revenir controler le stock.
 */
@Component("lancerReapprovisionnement")
public class LancerReapprovisionnementDelegate implements JavaDelegate {

    private final StockService stockService;
    private final CommandeService commandeService;

    public LancerReapprovisionnementDelegate(StockService stockService, CommandeService commandeService) {
        this.stockService = stockService;
        this.commandeService = commandeService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String commandeId = (String) execution.getVariable(ProcessVariables.COMMANDE_ID);
        Commande commande = commandeService.getObligatoire(commandeId);

        stockService.reapprovisionner(commande.getProduit(), commande.getQuantite());
        commandeService.changerStatut(commandeId, StatutCommande.EN_REAPPROVISIONNEMENT);
    }
}
