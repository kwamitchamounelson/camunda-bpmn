package com.formation.pret.process.service;

import com.formation.pret.process.PretVariables;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.IdentityLink;
import org.camunda.bpm.engine.task.IdentityLinkType;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Taches utilisateur du processus de pret uniquement : chaque requete est
 * restreinte a la cle ProcessusPretHypothecaire, pour ne jamais completer
 * par erreur une tache de livraison avec des variables de pret.
 */
@Service
public class PretTacheService {

    private static final Logger log = LoggerFactory.getLogger(PretTacheService.class);

    private final TaskService taskService;
    private final RuntimeService runtimeService;

    public PretTacheService(TaskService taskService, RuntimeService runtimeService) {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
    }

    public List<Task> lister() {
        return taskService.createTaskQuery()
                .processDefinitionKey(PretVariables.PROCESS_KEY)
                .orderByTaskCreateTime().asc()
                .list();
    }

    /** businessKey (= id de demande) des instances portant ces taches, en une seule requete. */
    public Map<String, String> demandeIdParInstance(List<Task> taches) {
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

    public Task getObligatoire(String taskId) {
        Task tache = taskService.createTaskQuery()
                .taskId(taskId)
                .processDefinitionKey(PretVariables.PROCESS_KEY)
                .singleResult();
        if (tache == null) {
            throw new IllegalArgumentException("Tache de pret introuvable ou deja terminee : " + taskId);
        }
        return tache;
    }

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
        if (utilisateur == null || utilisateur.isBlank()) {
            throw new IllegalArgumentException("Utilisateur obligatoire pour attribuer une tache");
        }
        taskService.claim(taskId, utilisateur.trim());
        log.info("Tache de pret {} attribuee a {}", taskId, utilisateur);
        return getObligatoire(taskId);
    }

    public Task liberer(String taskId) {
        getObligatoire(taskId);
        taskService.claim(taskId, null);
        return getObligatoire(taskId);
    }

    /**
     * Complete la tache et renvoie l'id de demande, lu avant la completion.
     * Les entiers JSON arrivent en Integer : ils sont convertis en Long,
     * le type qu'auraient poses les champs "long" des formulaires de la Tasklist.
     */
    public String completer(String taskId, Map<String, Object> variables) {
        getObligatoire(taskId);
        String demandeId = (String) taskService.getVariable(taskId, PretVariables.DEMANDE_ID);

        Map<String, Object> normalisees = new HashMap<>();
        if (variables != null) {
            variables.forEach((nom, valeur) ->
                    normalisees.put(nom, valeur instanceof Integer entier ? entier.longValue() : valeur));
        }

        taskService.complete(taskId, normalisees);
        log.info("Tache de pret {} completee pour {} (variables={})", taskId, demandeId, normalisees);
        return demandeId;
    }
}
