package com.formation.livraison.process;

import com.formation.livraison.domain.model.StatutCommande;
import com.formation.livraison.domain.service.CommandeService;
import com.formation.livraison.process.service.LivraisonProcessService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
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
    private HistoryService historyService;
    @Autowired
    private ManagementService managementService;

    @Test
    void commandeSimple_sansValidation_seTermineParLivraison() {
        // clavier x2 = 90 EUR, sous le seuil : pas de validation manager
        ProcessInstance instance = processService.demarrer(
                "Alice", "alice@exemple.fr", "clavier", 2);

        Task tache = tacheUnique(instance);
        assertThat(tache.getTaskDefinitionKey()).isEqualTo("Task_Prepa");
        assertThat(commandeService.getObligatoire(instance.getBusinessKey()).getMontant())
                .isEqualTo(90.0);

        taskService.complete(tache.getId());

        assertThat(estTerminee(instance)).isTrue();
        assertThat(commandeService.getObligatoire(instance.getBusinessKey()).getStatut())
                .isEqualTo(StatutCommande.LIVREE);
    }

    @Test
    void montantEleve_passeParLaValidationManager_puisLivraison() {
        // clavier x25 = 1125 EUR, au-dessus du seuil : validation requise
        ProcessInstance instance = processService.demarrer(
                "Bob", "bob@exemple.fr", "clavier", 25);

        Task validation = tacheUnique(instance);
        assertThat(validation.getTaskDefinitionKey()).isEqualTo("Task_Validation");

        taskService.complete(validation.getId(), Map.of(ProcessVariables.APPROUVE, true));

        Task preparation = tacheUnique(instance);
        assertThat(preparation.getTaskDefinitionKey()).isEqualTo("Task_Prepa");
        taskService.complete(preparation.getId());

        assertThat(estTerminee(instance)).isTrue();
        assertThat(commandeService.getObligatoire(instance.getBusinessKey()).getStatut())
                .isEqualTo(StatutCommande.LIVREE);
    }

    @Test
    void validationRejetee_annuleLaCommande() {
        ProcessInstance instance = processService.demarrer(
                "Carla", "carla@exemple.fr", "ecran", 5); // 1250 EUR

        Task validation = tacheUnique(instance);
        assertThat(validation.getTaskDefinitionKey()).isEqualTo("Task_Validation");

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
        // casque : stock initial 0 -> rupture
        ProcessInstance instance = processService.demarrer(
                "David", "david@exemple.fr", "casque", 1);

        // Pas encore de tache humaine : le processus attend sur le timer de reappro
        assertThat(taskService.createTaskQuery().processInstanceId(instance.getId()).count())
                .isZero();
        assertThat(commandeService.getObligatoire(instance.getBusinessKey()).getStatut())
                .isEqualTo(StatutCommande.EN_REAPPROVISIONNEMENT);

        // Le job executor est desactive en test : on declenche le timer manuellement
        Job timer = managementService.createJobQuery()
                .processInstanceId(instance.getId())
                .timers()
                .singleResult();
        assertThat(timer).isNotNull();
        managementService.executeJob(timer.getId());

        // Apres reappro et re-verification, le stock couvre la commande :
        // le processus avance jusqu'a la preparation
        Task tache = tacheUnique(instance);
        assertThat(tache.getTaskDefinitionKey()).isEqualTo("Task_Prepa");
    }

    private Task tacheUnique(ProcessInstance instance) {
        return taskService.createTaskQuery()
                .processInstanceId(instance.getId())
                .singleResult();
    }

    private boolean estTerminee(ProcessInstance instance) {
        HistoricProcessInstance historique = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(instance.getId())
                .singleResult();
        return historique != null && historique.getEndTime() != null;
    }
}
