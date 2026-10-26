package com.formation.pret.process.delegate;

import com.formation.pret.domain.service.DemandePretService;
import com.formation.pret.process.PretVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Atteint par deux chemins : le refus automatique du DMN (risque redhibitoire,
 * aucune decision humaine, donc pas de motifRefus) ou le refus de l'analyste / du comite.
 */
@Component("notifierRefus")
public class NotifierRefusDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(NotifierRefusDelegate.class);

    private final DemandePretService demandePretService;

    public NotifierRefusDelegate(DemandePretService demandePretService) {
        this.demandePretService = demandePretService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String demandeId = (String) execution.getVariable(PretVariables.DEMANDE_ID);
        String motif = (String) execution.getVariable(PretVariables.MOTIF_REFUS);

        if (motif == null || motif.isBlank()) {
            boolean refusAutomatique = execution.getVariable(PretVariables.PRET_ACCORDE) == null;
            motif = refusAutomatique
                    ? "Profil de risque incompatible (incident, score ou endettement)"
                    : "Non precise";
        }

        demandePretService.refuser(demandeId, motif);
        log.info("Refus notifie pour la demande {} : {}", demandeId, motif);
    }
}
