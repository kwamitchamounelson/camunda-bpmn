package com.formation.pret.process.delegate;

import com.formation.pret.domain.service.DemandePretService;
import com.formation.pret.domain.service.SimulateurCredit;
import com.formation.pret.process.PretVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.formation.pret.process.PretVariables.longOuZero;

/**
 * Chiffre l'offre definitive au taux propose par l'analyste ou le comite.
 * Le formulaire Camunda saisit ce taux en texte ("3,2" ou "3.2") : on le
 * convertit ici, avec repli sur le taux de reference s'il est vide ou illisible.
 */
@Component("emettreOffre")
public class EmettreOffreDelegate implements JavaDelegate {

    private final DemandePretService demandePretService;
    private final SimulateurCredit simulateurCredit;

    public EmettreOffreDelegate(DemandePretService demandePretService, SimulateurCredit simulateurCredit) {
        this.demandePretService = demandePretService;
        this.simulateurCredit = simulateurCredit;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> variables = execution.getVariables();
        String demandeId = (String) variables.get(PretVariables.DEMANDE_ID);

        double taux = lireTaux(variables.get(PretVariables.TAUX_PROPOSE));
        double mensualite = simulateurCredit.mensualite(
                longOuZero(variables, PretVariables.MONTANT_DEMANDE),
                (int) longOuZero(variables, PretVariables.DUREE_MOIS),
                taux);

        execution.setVariable(PretVariables.TAUX_ANNUEL, taux);
        execution.setVariable(PretVariables.MENSUALITE, mensualite);
        demandePretService.enregistrerOffre(demandeId, taux, mensualite);
    }

    private static double lireTaux(Object saisie) {
        if (saisie instanceof Number nombre) {
            return nombre.doubleValue();
        }
        if (saisie instanceof String texte && !texte.isBlank()) {
            try {
                return Double.parseDouble(texte.trim().replace(',', '.').replace("%", ""));
            } catch (NumberFormatException ignoree) {
                // Saisie libre du formulaire : on retombe sur le taux de reference.
            }
        }
        return SimulateurCredit.TAUX_REFERENCE;
    }
}
