package com.formation.livraison.process.rest;

import com.formation.livraison.domain.model.Commande;
import com.formation.livraison.domain.service.CommandeService;
import com.formation.livraison.process.service.LivraisonProcessService;
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
public class CommandeController {

    private final LivraisonProcessService processService;
    private final CommandeService commandeService;

    public CommandeController(LivraisonProcessService processService, CommandeService commandeService) {
        this.processService = processService;
        this.commandeService = commandeService;
    }

    @PostMapping
    public ResponseEntity<CommandeResponse> creer(@RequestBody CreerCommandeRequest request) {
        ProcessInstance instance = processService.demarrer(
                request.client(), request.email(), request.produit(), request.quantite());

        Commande commande = commandeService.getObligatoire(instance.getBusinessKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommandeResponse.from(commande));
    }

    @GetMapping
    public List<CommandeResponse> lister() {
        return commandeService.lister().stream().map(CommandeResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CommandeResponse detail(@PathVariable String id) {
        return CommandeResponse.from(commandeService.getObligatoire(id));
    }
}
