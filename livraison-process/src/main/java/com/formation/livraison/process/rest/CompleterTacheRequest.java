package com.formation.livraison.process.rest;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(name = "CompleterTacheRequest",
        description = "Variables posees au moment de completer la tache. Corps optionnel : "
                + "Task_Prepa n'attend aucune variable.")
public record CompleterTacheRequest(

        @Schema(description = "Variables de processus a ecrire avant de terminer la tache",
                example = "{\"approuve\": true}", nullable = true)
        Map<String, Object> variables) {
}
