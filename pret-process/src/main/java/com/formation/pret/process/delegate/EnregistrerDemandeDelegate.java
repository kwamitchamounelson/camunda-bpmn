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
 * Premiere etape du processus : le dossier est officiellement depose.
 * La demande existe deja en memoire (creee par PretProcessService avant le demarrage).
 */
@Component("enregistrerDemande")
public class EnregistrerDemandeDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(EnregistrerDemandeDelegate.class);

    private final DemandePretService demandePretService;

    public EnregistrerDemandeDelegate(DemandePretService demandePretService) {
        this.demandePretService = demandePretService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String demandeId = (String) execution.getVariable(PretVariables.DEMANDE_ID);
        demandePretService.changerStatut(demandeId, StatutPret.DEPOSEE);
        log.info("Demande de pret {} enregistree pour {}", demandeId,
                execution.getVariable(PretVariables.EMPRUNTEUR));
    }
}
