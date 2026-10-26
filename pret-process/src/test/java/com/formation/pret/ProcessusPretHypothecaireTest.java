package com.formation.pret;

import com.formation.pret.domain.model.DemandePret;
import com.formation.pret.domain.model.StatutPret;
import com.formation.pret.domain.service.DemandePretService;
import com.formation.pret.process.PretVariables;
import com.formation.pret.process.service.PretProcessService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProcessusPretHypothecaireTest {

    @Autowired
    private PretProcessService processService;
    @Autowired
    private DemandePretService demandePretService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private ManagementService managementService;

    @Test
    void dossierSolide_analyste_offreAcceptee_pretOctroye() {
        // 200 000 EUR sur 20 ans a 3,5 % => ~1160 EUR/mois pour 6000 EUR de revenus : ~19 %
        ProcessInstance instance = deposer("Alice Martin", 6000, 240_000);

        // Score et endettement sont calcules ; seule l'expertise retient le jeton au join.
        assertThat(tacheUnique(instance).getTaskDefinitionKey()).isEqualTo(PretVariables.TASK_EXPERTISE);
        assertThat(variable(instance, PretVariables.SCORE_CREDIT)).isEqualTo(780);
        assertThat((Double) variable(instance, PretVariables.TAUX_ENDETTEMENT)).isBetween(19.0, 20.0);

        completer(instance, Map.of(PretVariables.VALEUR_EXPERTISEE, 240_000L));
        assertThat(variable(instance, PretVariables.NIVEAU_RISQUE)).isEqualTo("standard");
        assertThat(tacheUnique(instance).getTaskDefinitionKey()).isEqualTo(PretVariables.TASK_ANALYSE);

        completer(instance, Map.of(PretVariables.PRET_ACCORDE, true, PretVariables.TAUX_PROPOSE, "3,2"));
        DemandePret offre = demande(instance);
        assertThat(offre.getStatut()).isEqualTo(StatutPret.OFFRE_EMISE);
        assertThat(offre.getTauxAnnuel()).isEqualTo(3.2);

        // Delai de reflexion : aucune tache tant que le timer n'a pas expire.
        assertThat(taskService.createTaskQuery().processInstanceId(instance.getId()).count()).isZero();
        executerTimer(instance);
        assertThat(tacheUnique(instance).getTaskDefinitionKey()).isEqualTo(PretVariables.TASK_ACCEPTATION);

        completer(instance, Map.of(PretVariables.OFFRE_ACCEPTEE, true));
        assertThat(tacheUnique(instance).getTaskDefinitionKey()).isEqualTo(PretVariables.TASK_SIGNATURE);
        completer(instance, Map.of());

        assertThat(estTerminee(instance)).isTrue();
        assertThat(demande(instance).getStatut()).isEqualTo(StatutPret.OCTROYEE);
        assertThat(demande(instance).getNumeroContrat()).startsWith("CTR-");
    }

    @Test
    void endettementAuDelaDe35_comiteDeCredit_refus() {
        // ~1160 EUR/mois pour 3150 EUR de revenus : ~36,8 % => derogation HCSF
        ProcessInstance instance = deposer("Bruno Durand", 3150, 240_000);
        completer(instance, Map.of(PretVariables.VALEUR_EXPERTISEE, 240_000L));

        assertThat(variable(instance, PretVariables.NIVEAU_RISQUE)).isEqualTo("eleve");
        assertThat(tacheUnique(instance).getTaskDefinitionKey()).isEqualTo(PretVariables.TASK_COMITE);

        completer(instance, Map.of(PretVariables.PRET_ACCORDE, false,
                PretVariables.MOTIF_REFUS, "Reste a vivre insuffisant"));

        assertThat(estTerminee(instance)).isTrue();
        assertThat(demande(instance).getStatut()).isEqualTo(StatutPret.REFUSEE);
        assertThat(demande(instance).getMotifRefus()).isEqualTo("Reste a vivre insuffisant");
    }

    @Test
    void incidentBancaire_refusAutomatiqueSansDecisionHumaine() {
        ProcessInstance instance = deposer("Paul Fiche", 6000, 240_000);
        completer(instance, Map.of(PretVariables.VALEUR_EXPERTISEE, 240_000L));

        assertThat(estTerminee(instance)).isTrue();
        assertThat(demande(instance).getStatut()).isEqualTo(StatutPret.REFUSEE);
        assertThat(demande(instance).getMotifRefus()).startsWith("Profil de risque incompatible");
    }

    @Test
    void valeurExpertiseeTropBasse_comiteDeCredit() {
        // 200 000 EUR empruntes pour un bien expertise 180 000 : LTV ~111 %
        ProcessInstance instance = deposer("Alice Martin", 6000, 240_000);
        completer(instance, Map.of(PretVariables.VALEUR_EXPERTISEE, 180_000L));

        assertThat(variable(instance, PretVariables.NIVEAU_RISQUE)).isEqualTo("eleve");
        assertThat(tacheUnique(instance).getTaskDefinitionKey()).isEqualTo(PretVariables.TASK_COMITE);
    }

    @Test
    void dossierIncomplet_conseillerLeComplete_puisInstruction() {
        ProcessInstance instance = deposer("Chloe Petit", 0, 240_000);

        assertThat(demande(instance).getStatut()).isEqualTo(StatutPret.INCOMPLETE);
        assertThat(variable(instance, PretVariables.PIECES_MANQUANTES)).isEqualTo("justificatifs de revenus");
        assertThat(tacheUnique(instance).getTaskDefinitionKey()).isEqualTo(PretVariables.TASK_COMPLETER);

        completer(instance, Map.of(PretVariables.REVENUS_MENSUELS, 5500L, PretVariables.VALEUR_BIEN, 240_000L));

        assertThat(demande(instance).getStatut()).isEqualTo(StatutPret.EN_ANALYSE);
        assertThat(demande(instance).getRevenusMensuels()).isEqualTo(5500);
        assertThat(tacheUnique(instance).getTaskDefinitionKey()).isEqualTo(PretVariables.TASK_EXPERTISE);
    }

    @Test
    void dossierIncomplet_sansReponseSous15Jours_classeSansSuite() {
        ProcessInstance instance = deposer("Chloe Petit", 0, 240_000);

        executerTimer(instance);

        assertThat(estTerminee(instance)).isTrue();
        assertThat(demande(instance).getStatut()).isEqualTo(StatutPret.CLASSEE_SANS_SUITE);
    }

    @Test
    void offreNonAccepteeSous30Jours_offreExpiree() {
        ProcessInstance instance = deposer("Alice Martin", 6000, 240_000);
        completer(instance, Map.of(PretVariables.VALEUR_EXPERTISEE, 240_000L));
        completer(instance, Map.of(PretVariables.PRET_ACCORDE, true));
        executerTimer(instance); // delai de reflexion

        executerTimer(instance); // validite de l'offre

        assertThat(estTerminee(instance)).isTrue();
        assertThat(demande(instance).getStatut()).isEqualTo(StatutPret.OFFRE_EXPIREE);
    }

    @Test
    void offreDeclineeParLeClient() {
        ProcessInstance instance = deposer("Alice Martin", 6000, 240_000);
        completer(instance, Map.of(PretVariables.VALEUR_EXPERTISEE, 240_000L));
        completer(instance, Map.of(PretVariables.PRET_ACCORDE, true));
        executerTimer(instance);

        completer(instance, Map.of(PretVariables.OFFRE_ACCEPTEE, false));

        assertThat(estTerminee(instance)).isTrue();
        assertThat(demande(instance).getStatut()).isEqualTo(StatutPret.OFFRE_DECLINEE);
    }

    // ---- helpers ----

    private ProcessInstance deposer(String emprunteur, long revenus, long valeurBien) {
        return processService.demarrer(emprunteur, "test@exemple.fr", 200_000, 240, revenus, 40_000, valeurBien);
    }

    private Task tacheUnique(ProcessInstance instance) {
        return taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();
    }

    private void completer(ProcessInstance instance, Map<String, Object> variables) {
        taskService.complete(tacheUnique(instance).getId(), variables);
    }

    private Object variable(ProcessInstance instance, String nom) {
        return runtimeService.getVariable(instance.getId(), nom);
    }

    private DemandePret demande(ProcessInstance instance) {
        return demandePretService.getObligatoire(instance.getBusinessKey());
    }

    private void executerTimer(ProcessInstance instance) {
        Job timer = managementService.createJobQuery()
                .processInstanceId(instance.getId())
                .timers()
                .singleResult();
        assertThat(timer).isNotNull();
        managementService.executeJob(timer.getId());
    }

    private boolean estTerminee(ProcessInstance instance) {
        HistoricProcessInstance historique = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(instance.getId())
                .singleResult();
        return historique != null && historique.getEndTime() != null;
    }
}
