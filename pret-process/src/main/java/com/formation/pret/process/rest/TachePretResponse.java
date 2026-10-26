package com.formation.pret.process.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import org.camunda.bpm.engine.task.Task;

import java.util.Date;
import java.util.List;

@Schema(name = "TachePretResponse", description = "Tache utilisateur active du processus de pret")
public record TachePretResponse(

        @Schema(example = "8f2c1d34-0b9c-11ef-9f3a-0242ac110002")
        String id,

        @Schema(description = "Id de l'activite dans le BPMN", example = "Task_Analyse",
                allowableValues = {"Task_Completer", "Task_Expertise", "Task_Analyse", "Task_Comite",
                        "Task_Acceptation", "Task_Signature"})
        String definitionKey,

        @Schema(example = "Analyser le dossier")
        String nom,

        @Schema(description = "Demande concernee (businessKey de l'instance)", example = "PRET-1A2B3C4D")
        String demandeId,

        @Schema(example = "d4e3f2a1-0b9c-11ef-9f3a-0242ac110002")
        String processInstanceId,

        @Schema(nullable = true, example = "analyste")
        String assignee,

        @Schema(example = "[\"analystes\"]")
        List<String> groupesCandidats,

        Date creeeLe) {

    public static TachePretResponse from(Task tache, String demandeId, List<String> groupesCandidats) {
        return new TachePretResponse(
                tache.getId(),
                tache.getTaskDefinitionKey(),
                tache.getName(),
                demandeId,
                tache.getProcessInstanceId(),
                tache.getAssignee(),
                groupesCandidats,
                tache.getCreateTime());
    }
}
