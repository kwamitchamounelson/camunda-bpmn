package com.formation.livraison.process.rest;

import com.formation.livraison.domain.model.Commande;
import com.formation.livraison.domain.service.CommandeService;
import com.formation.livraison.process.service.LivraisonProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

@RestController
@RequestMapping("/api/commandes")
@Tag(name = "Commandes", description = "Creation et consultation des commandes de livraison")
public class CommandeController {

    private final LivraisonProcessService processService;
    private final CommandeService commandeService;

    public CommandeController(LivraisonProcessService processService, CommandeService commandeService) {
        this.processService = processService;
        this.commandeService = commandeService;
    }

    @Operation(
            summary = "Creer une commande et demarrer le processus",
            description = """
                    Cree la commande puis demarre une instance de `processus_livraison`
                    avec l'id de commande comme businessKey.

                    Le montant est calcule par le processus (tarifs : clavier 45, souris 20,
                    ecran 250, casque 80, defaut 100), donc la reponse peut encore afficher
                    `montant = 0` si le calcul n'a pas eu lieu au moment du retour.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Commande creee, instance de processus demarree"),
            @ApiResponse(responseCode = "500", description = "Erreur au demarrage du processus",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<CommandeResponse> creer(@RequestBody CreerCommandeRequest request) {
        ProcessInstance instance = processService.demarrer(
                request.client(), request.email(), request.produit(), request.quantite(), request.typeClient());

        Commande commande = commandeService.getObligatoire(instance.getBusinessKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommandeResponse.from(commande));
    }

    @Operation(summary = "Lister toutes les commandes",
            description = "Retourne les commandes en memoire, tous statuts confondus.")
    @ApiResponse(responseCode = "200", description = "Liste des commandes")
    @GetMapping
    public List<CommandeResponse> lister() {
        return commandeService.lister().stream().map(CommandeResponse::from).toList();
    }

    @Operation(summary = "Consulter une commande",
            description = "Un id inconnu leve une IllegalArgumentException et se traduit par une erreur 500.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commande trouvee"),
            @ApiResponse(responseCode = "500", description = "Commande introuvable", content = @Content)
    })
    @GetMapping("/{id}")
    public CommandeResponse detail(
            @Parameter(description = "Id de la commande, tel que retourne a la creation",
                    example = "CMD-1A2B3C4D",
                    schema = @Schema(type = "string"))
            @PathVariable String id) {
        return CommandeResponse.from(commandeService.getObligatoire(id));
    }
}
