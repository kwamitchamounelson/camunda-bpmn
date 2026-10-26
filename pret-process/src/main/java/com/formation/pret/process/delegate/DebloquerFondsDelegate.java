package com.formation.pret.process.delegate;

import com.formation.pret.domain.model.StatutPret;
import com.formation.pret.domain.service.DemandePretService;
import com.formation.pret.process.PretVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Apres la signature de l'acte : les fonds sont verses au notaire.
 */
@Component("debloquerFonds")
public class DebloquerFondsDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(DebloquerFondsDelegate.class);

    private final DemandePretService demandePretService;

    public DebloquerFondsDelegate(DemandePretService demandePretService) {
        this.demandePretService = demandePretService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String demandeId = (String) execution.getVariable(PretVariables.DEMANDE_ID);
        demandePretService.changerStatut(demandeId, StatutPret.FONDS_DEBLOQUES);
        log.info("Fonds debloques pour la demande {} ({} EUR)", demandeId,
                execution.getVariable(PretVariables.MONTANT_DEMANDE));
    }
}
