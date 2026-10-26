package com.formation.pret.process.rest;

import com.formation.pret.process.service.PretTacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.camunda.bpm.engine.task.Task;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prets/taches")
@Tag(name = "Taches pret", description = "Taches utilisateur du processus de pret hypothecaire")
public class TachePretController {

    private final PretTacheService tacheService;

    public TachePretController(PretTacheService tacheService) {
        this.tacheService = tacheService;
    }

    @Operation(summary = "Lister les taches de pret en attente")
    @ApiResponse(responseCode = "200", description = "Taches actives du processus de pret")
    @GetMapping
    public List<TachePretResponse> lister() {
        List<Task> taches = tacheService.lister();
        Map<String, String> demandeIds = tacheService.demandeIdParInstance(taches);
        return taches.stream()
                .map(tache -> TachePretResponse.from(tache,
                        demandeIds.get(tache.getProcessInstanceId()),
                        tacheService.groupesCandidats(tache.getId())))
                .toList();
    }

    @Operation(summary = "S'attribuer une tache de pret")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tache attribuee"),
            @ApiResponse(responseCode = "400", description = "Tache inconnue ou utilisateur absent", content = @Content),
            @ApiResponse(responseCode = "409", description = "Tache deja prise", content = @Content)
    })
    @PostMapping("/{taskId}/attribuer")
    public TachePretResponse attribuer(@PathVariable String taskId,
                                       @RequestBody AttribuerTachePretRequest request) {
        return reponse(tacheService.attribuer(taskId, request.utilisateur()));
    }

    @Operation(summary = "Liberer une tache de pret")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tache liberee"),
            @ApiResponse(responseCode = "400", description = "Tache inconnue", content = @Content)
    })
    @PostMapping("/{taskId}/liberer")
    public TachePretResponse liberer(@PathVariable String taskId) {
        return reponse(tacheService.liberer(taskId));
    }

    @Operation(
            summary = "Completer une tache de pret",
            description = "Pose les variables fournies puis termine la tache. Voir le schema "
                    + "CompleterTachePretRequest pour les variables attendues par chaque tache.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tache completee ; renvoie l'id de la demande"),
            @ApiResponse(responseCode = "400", description = "Tache inconnue ou variable manquante", content = @Content)
    })
    @PostMapping("/{taskId}/completer")
    public Map<String, String> completer(@PathVariable String taskId,
                                         @RequestBody(required = false) CompleterTachePretRequest request) {
        String demandeId = tacheService.completer(taskId, request == null ? Map.of() : request.variables());
        return Map.of("demandeId", demandeId);
    }

    private TachePretResponse reponse(Task tache) {
        return TachePretResponse.from(tache,
                tacheService.demandeIdParInstance(List.of(tache)).get(tache.getProcessInstanceId()),
                tacheService.groupesCandidats(tache.getId()));
    }
}
