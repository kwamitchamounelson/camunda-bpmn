package com.formation.pret.process.rest;

import com.formation.pret.domain.model.DemandePret;
import com.formation.pret.domain.service.DemandePretService;
import com.formation.pret.process.service.PretProcessService;
import com.formation.pret.process.service.PretProcessService.TimerEnAttente;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prets")
@Tag(name = "Prets", description = "Depot et suivi des demandes de pret hypothecaire")
public class PretController {

    private final PretProcessService processService;
    private final DemandePretService demandePretService;

    public PretController(PretProcessService processService, DemandePretService demandePretService) {
        this.processService = processService;
        this.demandePretService = demandePretService;
    }

    @Operation(
            summary = "Deposer une demande de pret et demarrer le processus",
            description = """
                    Cree la demande puis demarre une instance de `ProcessusPretHypothecaire`
                    avec l'id de demande comme businessKey.

                    Le processus s'execute jusqu'au premier etat d'attente : tache
                    "Completer le dossier" si des pieces manquent, sinon tache "Expertiser le bien".
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Demande creee, instance demarree"),
            @ApiResponse(responseCode = "500", description = "Erreur au demarrage du processus", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PretResponse> creer(@RequestBody CreerDemandePretRequest request) {
        ProcessInstance instance = processService.demarrer(request.emprunteur(), request.email(),
                request.montantDemande(), request.dureeMois(), request.revenusMensuels(),
                request.apport(), request.valeurBien());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reponse(demandePretService.getObligatoire(instance.getBusinessKey())));
    }

    @Operation(summary = "Lister les demandes de pret",
            description = "Chaque demande embarque les variables et les timers en attente de son instance.")
    @ApiResponse(responseCode = "200", description = "Liste des demandes")
    @GetMapping
    public List<PretResponse> lister() {
        return reponses(demandePretService.lister());
    }

    @Operation(summary = "Consulter une demande de pret")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande trouvee"),
            @ApiResponse(responseCode = "400", description = "Demande introuvable", content = @Content)
    })
    @GetMapping("/{id}")
    public PretResponse detail(
            @Parameter(description = "Id de la demande", example = "PRET-1A2B3C4D") @PathVariable String id) {
        return reponse(demandePretService.getObligatoire(id));
    }

    @Operation(
            summary = "Declencher un timer immediatement",
            description = """
                    Simule l'ecoulement du delai sans attendre : relance des pieces (15 j),
                    delai de reflexion legal (10 j) ou validite de l'offre (30 j).
                    Equivalent du bouton "Executer" sur un job dans Cockpit.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Timer execute, etat de la demande a jour"),
            @ApiResponse(responseCode = "400", description = "Timer inconnu ou deja execute", content = @Content)
    })
    @PostMapping("/timers/{jobId}/declencher")
    public PretResponse declencherTimer(@PathVariable String jobId) {
        String demandeId = processService.declencherTimer(jobId);
        return reponse(demandePretService.getObligatoire(demandeId));
    }

    private PretResponse reponse(DemandePret demande) {
        return reponses(List.of(demande)).get(0);
    }

    private List<PretResponse> reponses(List<DemandePret> demandes) {
        Map<String, Map<String, Object>> variables = processService.variablesParInstance(
                demandes.stream().map(DemandePret::getProcessInstanceId).toList());
        Map<String, List<TimerEnAttente>> timers = processService.timersParInstance();

        return demandes.stream()
                .map(demande -> PretResponse.from(demande,
                        variables.get(demande.getProcessInstanceId()),
                        timers.get(demande.getProcessInstanceId())))
                .toList();
    }
}
