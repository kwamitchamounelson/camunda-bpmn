package com.formation.livraison.process.service;

import com.formation.livraison.process.ProcessVariables;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.IdentityLink;
import org.camunda.bpm.engine.task.IdentityLinkType;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pendant de {@link LivraisonProcessService} pour les taches utilisateur :
 * expose ce que fait la Tasklist Camunda (lister, s'attribuer, completer)
 * sous une forme utilisable depuis Swagger.
 */
@Service
public class TacheService {

    private static final Logger log = LoggerFactory.getLogger(TacheService.class);

    private final TaskService taskService;
    private final RuntimeService runtimeService;

    public TacheService(TaskService taskService, RuntimeService runtimeService) {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
    }

    /**
     * Taches actives, filtrables. Les filtres sont cumulables sauf groupe + assignee,
     * combinaison que le moteur refuse.
     */
    public List<Task> lister(String groupe, String assignee, String commandeId) {
        String groupeFiltre = normaliser(groupe);
        String assigneeFiltre = normaliser(assignee);
        String commandeFiltre = normaliser(commandeId);

        if (groupeFiltre != null && assigneeFiltre != null) {
            throw new IllegalArgumentException(
                    "Les filtres 'groupe' et 'assignee' ne peuvent pas etre combines : "
                            + "le moteur rejette cette requete. Utilisez l'un ou l'autre.");
        }

        // Le moteur heberge aussi le processus de pret : sans ce filtre, ses taches
        // apparaitraient ici avec un id de demande a la place d'un id de commande.
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(ProcessVariables.PROCESS_KEY)
                .orderByTaskCreateTime().asc();
        if (groupeFiltre != null) {
            // Une requete par groupe candidat ignore par defaut les taches deja attribuees.
            // includeAssignedTasks() rend la file complete, attribuee ou non.
            query.taskCandidateGroup(groupeFiltre).includeAssignedTasks();
        }
        if (assigneeFiltre != null) {
            query.taskAssignee(assigneeFiltre);
        }
        if (commandeFiltre != null) {
            // La businessKey de l'instance est l'id de commande (cf. LivraisonProcessService).
            query.processInstanceBusinessKey(commandeFiltre);
        }
        return query.list();
    }

    public Task getObligatoire(String taskId) {
        Task tache = taskService.createTaskQuery()
                .taskId(taskId)
                .processDefinitionKey(ProcessVariables.PROCESS_KEY)
                .singleResult();
        if (tache == null) {
            throw new IllegalArgumentException("Tache introuvable ou deja terminee : " + taskId);
        }
        return tache;
    }

    /**
     * Resout en une seule requete la businessKey (= id de commande) des instances
     * portant les taches passees, plutot qu'une requete par tache.
     */
    public Map<String, String> commandeIdParInstance(List<Task> taches) {
        Set<String> instanceIds = taches.stream()
                .map(Task::getProcessInstanceId)
                .collect(Collectors.toSet());
        if (instanceIds.isEmpty()) {
            return Map.of();
        }
        return runtimeService.createProcessInstanceQuery()
                .processInstanceIds(instanceIds)
                .list().stream()
                .filter(instance -> instance.getBusinessKey() != null)
                .collect(Collectors.toMap(ProcessInstance::getProcessInstanceId, ProcessInstance::getBusinessKey));
    }

    /**
     * Groupes candidats de la tache : "logistique" pour la preparation,
     * "managers" ou "directeurs" pour la validation selon la decision DMN.
     */
    public List<String> groupesCandidats(String taskId) {
        return taskService.getIdentityLinksForTask(taskId).stream()
                .filter(lien -> IdentityLinkType.CANDIDATE.equals(lien.getType()))
                .map(IdentityLink::getGroupId)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    public Task attribuer(String taskId, String utilisateur) {
        getObligatoire(taskId);
        if (normaliser(utilisateur) == null) {
            throw new IllegalArgumentException("Utilisateur obligatoire pour attribuer une tache");
        }
        // claim() refuse une tache deja prise par quelqu'un d'autre, contrairement
        // a setAssignee() : c'est le comportement de la Tasklist.
        taskService.claim(taskId, utilisateur);
        log.info("Tache {} attribuee a {}", taskId, utilisateur);
        return getObligatoire(taskId);
    }

    public Task liberer(String taskId) {
        getObligatoire(taskId);
        taskService.claim(taskId, null);
        log.info("Tache {} liberee", taskId);
        return getObligatoire(taskId);
    }

    /**
     * Complete la tache et renvoie l'id de commande concerne, lu avant la
     * completion : une fois la tache terminee, ses variables ne sont plus accessibles.
     */
    public String completer(String taskId, Map<String, Object> variables) {
        getObligatoire(taskId);
        String commandeId = (String) taskService.getVariable(taskId, ProcessVariables.COMMANDE_ID);

        taskService.complete(taskId, variables == null ? Map.of() : variables);
        log.info("Tache {} completee pour la commande {} (variables={})", taskId, commandeId, variables);
        return commandeId;
    }

    /**
     * Raccourci metier pour Task_Validation : pose les variables attendues par la
     * passerelle Gw_ValidDecision (${approuve}) et par le delegate d'annulation.
     */
    public String decider(String taskId, boolean approuve, String motifRefus) {
        Task tache = getObligatoire(taskId);
        if (!ProcessVariables.TASK_VALIDATION.equals(tache.getTaskDefinitionKey())) {
            throw new IllegalArgumentException(
                    "La tache %s est un '%s', pas une tache de validation : utilisez /completer."
                            .formatted(taskId, tache.getTaskDefinitionKey()));
        }

        // HashMap et non Map.of : motifRefus est null quand la commande est approuvee.
        Map<String, Object> variables = new HashMap<>();
        variables.put(ProcessVariables.APPROUVE, approuve);
        variables.put(ProcessVariables.MOTIF_REFUS,
                approuve ? null : (normaliser(motifRefus) == null ? "Non precise" : motifRefus.trim()));

        return completer(taskId, variables);
    }

    /** Pratique pour le mapping REST : les identity links tache par tache. */
    public Map<String, List<String>> groupesCandidatsParTache(List<Task> taches) {
        return taches.stream().collect(Collectors.toMap(
                Task::getId,
                tache -> groupesCandidats(tache.getId()),
                (premier, second) -> premier,
                LinkedHashMap::new));
    }

    private static String normaliser(String valeur) {
        return (valeur == null || valeur.isBlank()) ? null : valeur.trim();
    }
}
