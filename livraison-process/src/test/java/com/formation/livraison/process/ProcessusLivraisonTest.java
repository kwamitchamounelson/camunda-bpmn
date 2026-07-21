package com.formation.livraison.process;

import com.formation.livraison.domain.model.StatutCommande;
import com.formation.livraison.domain.service.CommandeService;
import com.formation.livraison.process.service.LivraisonProcessService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

    @Test
    void leProcessusSArreteSurLaTacheUtilisateurPuisSeTermine() {
        ProcessInstance instance = processService.demarrer(
                "Alice", "alice@exemple.fr", "clavier", 2);

        // Task_1 (service) s'execute immediatement, le moteur attend sur Task_2 (user task)
        Task tache = taskService.createTaskQuery()
                .processInstanceId(instance.getId())
                .singleResult();

        assertThat(tache).isNotNull();
        assertThat(tache.getTaskDefinitionKey()).isEqualTo("Task_2");
        assertThat(tache.getAssignee()).isEqualTo("employeLogistique");

        String commandeId = instance.getBusinessKey();
        assertThat(commandeService.getObligatoire(commandeId).getStatut())
                .isEqualTo(StatutCommande.STOCK_VERIFIE);

        // Completer la tache deroule Task_3 puis Task_4 jusqu'a l'evenement de fin
        taskService.complete(tache.getId());

        HistoricProcessInstance historique = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(instance.getId())
                .singleResult();

        assertThat(historique.getEndTime()).isNotNull();
        assertThat(commandeService.getObligatoire(commandeId).getStatut())
                .isEqualTo(StatutCommande.LIVREE);
    }

    @Test
    void unProduitEnRuptureEstMarqueIndisponible() {
        ProcessInstance instance = processService.demarrer(
                "Bob", "bob@exemple.fr", "casque", 1);

        assertThat(commandeService.getObligatoire(instance.getBusinessKey()).getStatut())
                .isEqualTo(StatutCommande.STOCK_INSUFFISANT);
    }
}
