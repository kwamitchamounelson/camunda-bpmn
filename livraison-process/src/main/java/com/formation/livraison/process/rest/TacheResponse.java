package com.formation.livraison.process.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import org.camunda.bpm.engine.task.Task;

import java.util.Date;
import java.util.List;

@Schema(name = "TacheResponse", description = "Tache utilisateur active dans le moteur")
public record TacheResponse(

        @Schema(description = "Id technique de la tache, a reutiliser dans les autres appels",
                example = "8f2c1d34-0b9c-11ef-9f3a-0242ac110002")
        String id,

        @Schema(description = "Id de l'activite dans le BPMN", example = "Task_Validation",
                allowableValues = {"Task_Validation", "Task_Prepa"})
        String definitionKey,

        @Schema(description = "Libelle affiche dans la Tasklist", example = "Valider la commande")
        String nom,

        @Schema(description = "Commande concernee (businessKey de l'instance)", example = "CMD-1A2B3C4D")
        String commandeId,

        @Schema(example = "d4e3f2a1-0b9c-11ef-9f3a-0242ac110002")
        String processInstanceId,

        @Schema(description = "Utilisateur ayant pris la tache, null si personne",
                example = "demo", nullable = true)
        String assignee,

        @Schema(description = "Groupes autorises a traiter la tache", example = "[\"managers\"]")
        List<String> groupesCandidats,

        @Schema(description = "Date de creation de la tache")
        Date creeeLe) {

    public static TacheResponse from(Task tache, String commandeId, List<String> groupesCandidats) {
        return new TacheResponse(
                tache.getId(),
                tache.getTaskDefinitionKey(),
                tache.getName(),
                commandeId,
                tache.getProcessInstanceId(),
                tache.getAssignee(),
                groupesCandidats,
                tache.getCreateTime());
    }
}
