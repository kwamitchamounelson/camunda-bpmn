package com.formation.pret.process.delegate;

import com.formation.pret.domain.service.SimulateurCredit;
import com.formation.pret.process.PretVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.formation.pret.process.PretVariables.longOuZero;

/**
 * Branche parallele 2 : mensualite estimee au taux de reference et taux
 * d'endettement, compare par le DMN au plafond de 35 % (norme HCSF).
 */
@Component("calculerEndettement")
public class CalculerEndettementDelegate implements JavaDelegate {

    private final SimulateurCredit simulateurCredit;

    public CalculerEndettementDelegate(SimulateurCredit simulateurCredit) {
        this.simulateurCredit = simulateurCredit;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> variables = execution.getVariables();
        double mensualite = simulateurCredit.mensualite(
                longOuZero(variables, PretVariables.MONTANT_DEMANDE),
                (int) longOuZero(variables, PretVariables.DUREE_MOIS),
                SimulateurCredit.TAUX_REFERENCE);

        execution.setVariable(PretVariables.MENSUALITE_ESTIMEE, mensualite);
        execution.setVariable(PretVariables.TAUX_ENDETTEMENT, simulateurCredit.tauxEndettement(
                mensualite, longOuZero(variables, PretVariables.REVENUS_MENSUELS)));
    }
}
