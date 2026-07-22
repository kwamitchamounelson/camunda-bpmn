package com.formation.livraison.process;

import com.formation.livraison.domain.model.StatutCommande;
import com.formation.livraison.domain.service.CommandeService;
import com.formation.livraison.process.service.LivraisonProcessService;
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
class ProcessusLivraisonTest {

    @Autowired
    private LivraisonProcessService processService;
    @Autowired
    private CommandeService commandeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private ManagementService managementService;

    @Test
    void montantFaible_dmnRenvoieAucun_pasDeValidation() {
        // clavier x2 = 90 EUR -> DMN: "aucun" -> saute la validation
        ProcessInstance instance = processService.demarrer(
                "Alice", "alice@exemple.fr", "clavier", 2, "STANDARD");

        assertThat(validateur(instance)).isEqualTo("aucun");
        Task tache = tacheUnique(instance);
        assertThat(tache.getTaskDefinitionKey()).isEqualTo("Task_Prepa");

        taskService.complete(tache.getId());
        assertThat(estTerminee(instance)).isTrue();
        assertThat(statut(instance)).isEqualTo(StatutCommande.LIVREE);
    }

    @Test
    void montantMoyen_dmnRenvoieManagers_validationManagerPuisLivraison() {
        // clavier x25 = 1125 EUR, client standard -> DMN: "managers"
        ProcessInstance instance = processService.demarrer(
                "Bob", "bob@exemple.fr", "clavier", 25, "STANDARD");

        assertThat(validateur(instance)).isEqualTo("managers");
        Task validation = tacheUnique(instance);
        assertThat(validation.getTaskDefinitionKey()).isEqualTo("Task_Validation");

        taskService.complete(validation.getId(), Map.of(ProcessVariables.APPROUVE, true));

        Task preparation = tacheUnique(instance);
        assertThat(preparation.getTaskDefinitionKey()).isEqualTo("Task_Prepa");
        taskService.complete(preparation.getId());

        assertThat(estTerminee(instance)).isTrue();
        assertThat(statut(instance)).isEqualTo(StatutCommande.LIVREE);
    }

    @Test
    void grosMontant_dmnRenvoieDirecteurs() {
        // ecran x25 = 6250 EUR -> DMN: "directeurs"
        ProcessInstance instance = processService.demarrer(
                "Carla", "carla@exemple.fr", "ecran", 25, "STANDARD");

        // ecran est en rupture (stock 3) : on debloque d'abord le reappro
        executerTimerReappro(instance);

        assertThat(validateur(instance)).isEqualTo("directeurs");
        assertThat(tacheUnique(instance).getTaskDefinitionKey()).isEqualTo("Task_Validation");
    }

    @Test
    void clientVip_dmnRenvoieAucun_memeAvecUnMontantEleve() {
        // clavier x25 = 1125 EUR MAIS client VIP (< 10000) -> DMN: "aucun"
        ProcessInstance instance = processService.demarrer(
                "Vera", "vera@exemple.fr", "clavier", 25, "VIP");

        assertThat(validateur(instance)).isEqualTo("aucun");
        assertThat(tacheUnique(instance).getTaskDefinitionKey()).isEqualTo("Task_Prepa");
    }

    @Test
    void validationRejetee_annuleLaCommande() {
        ProcessInstance instance = processService.demarrer(
                "David", "david@exemple.fr", "clavier", 25, "STANDARD"); // 1125 EUR -> managers

        Task validation = tacheUnique(instance);
        taskService.complete(validation.getId(), Map.of(
                ProcessVariables.APPROUVE, false,
                ProcessVariables.MOTIF_REFUS, "Budget depasse"));

        assertThat(estTerminee(instance)).isTrue();
        var commande = commandeService.getObligatoire(instance.getBusinessKey());
        assertThat(commande.getStatut()).isEqualTo(StatutCommande.REFUSEE);
        assertThat(commande.getMotifRefus()).isEqualTo("Budget depasse");
    }

    @Test
    void ruptureDeStock_declencheLaBoucleDeReapprovisionnement() {
        // casque : stock initial 0 -> rupture. casque x1 = 80 EUR -> pas de validation ensuite
        ProcessInstance instance = processService.demarrer(
                "Eric", "eric@exemple.fr", "casque", 1, "STANDARD");

        assertThat(taskService.createTaskQuery().processInstanceId(instance.getId()).count())
                .isZero();
        assertThat(statut(instance)).isEqualTo(StatutCommande.EN_REAPPROVISIONNEMENT);

        executerTimerReappro(instance);

        assertThat(tacheUnique(instance).getTaskDefinitionKey()).isEqualTo("Task_Prepa");
    }

    // ---- helpers ----

    private Task tacheUnique(ProcessInstance instance) {
        return taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();
    }

    private String validateur(ProcessInstance instance) {
        return (String) runtimeService.getVariable(instance.getId(), ProcessVariables.VALIDATEUR);
    }

    private StatutCommande statut(ProcessInstance instance) {
        return commandeService.getObligatoire(instance.getBusinessKey()).getStatut();
    }

    private void executerTimerReappro(ProcessInstance instance) {
        // Le job executor est desactive en test : on declenche le timer manuellement
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
