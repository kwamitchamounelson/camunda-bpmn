package com.formation.pret.process.service;

import com.formation.pret.domain.model.DemandePret;
import com.formation.pret.domain.service.DemandePretService;
import com.formation.pret.process.PretVariables;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.management.JobDefinition;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Pont entre le metier et le moteur pour le pret : demarrage de l'instance,
 * lecture des variables pour l'affichage et pilotage des timers.
 */
@Service
public class PretProcessService {

    private static final Logger log = LoggerFactory.getLogger(PretProcessService.class);

    /** Timer en attente sur une instance : delai de relance, de reflexion ou de validite d'offre. */
    public record TimerEnAttente(String jobId, String processInstanceId, String activiteId,
                                 String libelle, Date echeance) {
    }

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final ManagementService managementService;
    private final RepositoryService repositoryService;
    private final DemandePretService demandePretService;

    public PretProcessService(RuntimeService runtimeService, HistoryService historyService,
                              ManagementService managementService, RepositoryService repositoryService,
                              DemandePretService demandePretService) {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.managementService = managementService;
        this.repositoryService = repositoryService;
        this.demandePretService = demandePretService;
    }

    public ProcessInstance demarrer(String emprunteur, String email, long montantDemande, int dureeMois,
                                    long revenusMensuels, long apport, long valeurBien) {
        DemandePret demande = demandePretService.creer(emprunteur, email, montantDemande, dureeMois,
                revenusMensuels, apport, valeurBien);

        Map<String, Object> variables = Variables.createVariables()
                .putValue(PretVariables.DEMANDE_ID, demande.getId())
                .putValue(PretVariables.EMPRUNTEUR, emprunteur)
                .putValue(PretVariables.MONTANT_DEMANDE, montantDemande)
                .putValue(PretVariables.DUREE_MOIS, (long) dureeMois)
                .putValue(PretVariables.REVENUS_MENSUELS, revenusMensuels)
                .putValue(PretVariables.APPORT, apport)
                .putValue(PretVariables.VALEUR_BIEN, valeurBien);

        // businessKey = id de demande, comme pour les commandes de livraison.
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                PretVariables.PROCESS_KEY, demande.getId(), variables);

        demandePretService.lierAuProcessus(demande.getId(), instance.getId());
        log.info("Instance {} demarree pour la demande de pret {}", instance.getId(), demande.getId());
        return instance;
    }

    /**
     * Variables de chaque instance lues dans l'historique (history-level full) :
     * elles restent donc consultables une fois l'instance terminee.
     */
    public Map<String, Map<String, Object>> variablesParInstance(List<String> processInstanceIds) {
        List<String> ids = processInstanceIds.stream().filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return historyService.createHistoricVariableInstanceQuery()
                .processInstanceIdIn(ids.toArray(String[]::new))
                .list().stream()
                // Une variable sans valeur (ex. motifRefus d'un pret accorde) n'apporte rien a l'affichage.
                .filter(variable -> variable.getValue() != null)
                .collect(Collectors.groupingBy(
                        HistoricVariableInstance::getProcessInstanceId,
                        Collectors.toMap(HistoricVariableInstance::getName, HistoricVariableInstance::getValue,
                                (premiere, seconde) -> seconde, TreeMap::new)));
    }

    public Map<String, List<TimerEnAttente>> timersParInstance() {
        List<Job> timers = managementService.createJobQuery()
                .processDefinitionKey(PretVariables.PROCESS_KEY)
                .timers()
                .list();
        return timers.stream()
                .map(this::versTimer)
                .collect(Collectors.groupingBy(TimerEnAttente::processInstanceId));
    }

    /**
     * Execute un timer tout de suite au lieu d'attendre 10, 15 ou 30 jours.
     * C'est ce que fait le bouton "Executer" d'un job dans Cockpit.
     */
    public String declencherTimer(String jobId) {
        Job job = managementService.createJobQuery()
                .jobId(jobId)
                .processDefinitionKey(PretVariables.PROCESS_KEY)
                .timers()
                .singleResult();
        if (job == null) {
            throw new IllegalArgumentException("Timer introuvable ou deja execute : " + jobId);
        }
        String demandeId = demandeIdDe(job.getProcessInstanceId());
        managementService.executeJob(jobId);
        log.info("Timer {} declenche manuellement pour la demande {}", jobId, demandeId);
        return demandeId;
    }

    private String demandeIdDe(String processInstanceId) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        return instance == null ? null : instance.getBusinessKey();
    }

    private TimerEnAttente versTimer(Job job) {
        JobDefinition definition = managementService.createJobDefinitionQuery()
                .jobDefinitionId(job.getJobDefinitionId())
                .singleResult();
        String activiteId = definition == null ? null : definition.getActivityId();
        return new TimerEnAttente(job.getId(), job.getProcessInstanceId(), activiteId,
                libelleActivite(job.getProcessDefinitionId(), activiteId), job.getDuedate());
    }

    /** Nom affiche dans le BPMN ("Delai de reflexion legal (10 j)"...), mis en cache par le moteur. */
    private String libelleActivite(String processDefinitionId, String activiteId) {
        if (activiteId == null) {
            return null;
        }
        FlowElement element = repositoryService.getBpmnModelInstance(processDefinitionId)
                .getModelElementById(activiteId);
        return element == null || element.getName() == null ? activiteId : element.getName();
    }
}
