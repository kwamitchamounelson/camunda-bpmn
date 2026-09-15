package com.formation.livraison.process.rest;

import com.formation.livraison.domain.service.CommandeService;
import com.formation.livraison.process.service.TacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.camunda.bpm.engine.task.Task;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/taches")
@Tag(name = "Taches", description = "Traitement des taches utilisateur (equivalent de la Tasklist Camunda)")
public class TacheController {

    private final TacheService tacheService;
    private final CommandeService commandeService;

    public TacheController(TacheService tacheService, CommandeService commandeService) {
        this.tacheService = tacheService;
        this.commandeService = commandeService;
    }

    @Operation(
            summary = "Lister les taches en attente",
            description = """
                    Sans filtre, retourne toutes les taches actives, preparation et validation confondues.

                    Les filtres `groupe` et `assignee` s'excluent : le moteur rejette la combinaison
                    des deux. `groupe` inclut volontairement les taches deja attribuees, pour voir
                    l'etat complet de la file.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des taches actives"),
            @ApiResponse(responseCode = "400", description = "Filtres groupe et assignee combines",
                    content = @Content)
    })
    @GetMapping
    public List<TacheResponse> lister(
            @Parameter(description = "Groupe candidat", example = "logistique",
                    schema = @Schema(allowableValues = {"logistique", "managers", "directeurs"}))
            @RequestParam(required = false) String groupe,

            @Parameter(description = "Utilisateur ayant pris la tache", example = "employeLogistique")
            @RequestParam(required = false) String assignee,

            @Parameter(description = "Restreint a une commande (businessKey)", example = "CMD-1A2B3C4D")
            @RequestParam(required = false) String commandeId) {

        List<Task> taches = tacheService.lister(groupe, assignee, commandeId);
        Map<String, String> commandeIds = tacheService.commandeIdParInstance(taches);
        Map<String, List<String>> groupes = tacheService.groupesCandidatsParTache(taches);

        return taches.stream()
                .map(tache -> TacheResponse.from(tache,
                        commandeIds.get(tache.getProcessInstanceId()),
                        groupes.getOrDefault(tache.getId(), List.of())))
                .toList();
    }

    @Operation(summary = "Consulter une tache")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tache trouvee"),
            @ApiResponse(responseCode = "400", description = "Tache inconnue ou deja terminee",
                    content = @Content)
    })
    @GetMapping("/{taskId}")
    public TacheResponse detail(@PathVariable String taskId) {
        Task tache = tacheService.getObligatoire(taskId);
        return TacheResponse.from(tache,
                tacheService.commandeIdParInstance(List.of(tache)).get(tache.getProcessInstanceId()),
                tacheService.groupesCandidats(taskId));
    }

    @Operation(summary = "S'attribuer une tache",
            description = "Equivalent du bouton Claim de la Tasklist. Une tache deja prise par "
                    + "un autre utilisateur est refusee : liberez-la d'abord.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tache attribuee"),
            @ApiResponse(responseCode = "400", description = "Tache inconnue ou utilisateur absent",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Tache deja prise par un autre utilisateur",
                    content = @Content)
    })
    @PostMapping("/{taskId}/attribuer")
    public TacheResponse attribuer(@PathVariable String taskId,
                                   @RequestBody AttribuerTacheRequest request) {
        Task tache = tacheService.attribuer(taskId, request.utilisateur());
        return TacheResponse.from(tache,
                tacheService.commandeIdParInstance(List.of(tache)).get(tache.getProcessInstanceId()),
                tacheService.groupesCandidats(taskId));
    }

    @Operation(summary = "Liberer une tache",
            description = "Remet la tache dans la file du groupe candidat, sans assignee.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tache liberee"),
            @ApiResponse(responseCode = "400", description = "Tache inconnue", content = @Content)
    })
    @PostMapping("/{taskId}/liberer")
    public TacheResponse liberer(@PathVariable String taskId) {
        Task tache = tacheService.liberer(taskId);
        return TacheResponse.from(tache,
                tacheService.commandeIdParInstance(List.of(tache)).get(tache.getProcessInstanceId()),
                tacheService.groupesCandidats(taskId));
    }

    @Operation(
            summary = "Completer une tache",
            description = """
                    Termine la tache et relance le jeton. Utilisable pour Task_Prepa,
                    qui n'attend aucune variable : envoyez un corps vide ou `{}`.

                    Pour Task_Validation, preferez /decider qui pose les bonnes variables.

                    La reponse est l'etat de la commande APRES avancement du processus :
                    completer Task_Prepa declenche email + expedition et va jusqu'au statut LIVREE.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tache completee, etat de la commande a jour"),
            @ApiResponse(responseCode = "400", description = "Tache inconnue ou variable manquante",
                    content = @Content)
    })
    @PostMapping("/{taskId}/completer")
    public CommandeResponse completer(@PathVariable String taskId,
                                      @RequestBody(required = false) CompleterTacheRequest request) {
        String commandeId = tacheService.completer(taskId,
                request == null ? Map.of() : request.variables());
        return CommandeResponse.from(commandeService.getObligatoire(commandeId));
    }

    @Operation(
            summary = "Decider d'une validation (Task_Validation)",
            description = """
                    Pose `approuve` et `motifRefus` puis complete la tache.

                    `approuve = true` envoie la commande en preparation (Task_Prepa apparait ensuite
                    dans la file du groupe logistique). `approuve = false` declenche l'annulation :
                    la commande passe au statut REFUSEE avec le motif fourni.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Decision enregistree"),
            @ApiResponse(responseCode = "400",
                    description = "Tache inconnue, ou tache qui n'est pas une validation",
                    content = @Content)
    })
    @PostMapping("/{taskId}/decider")
    public CommandeResponse decider(@PathVariable String taskId,
                                    @RequestBody DeciderValidationRequest request) {
        String commandeId = tacheService.decider(taskId, request.approuve(), request.motifRefus());
        return CommandeResponse.from(commandeService.getObligatoire(commandeId));
    }
}
