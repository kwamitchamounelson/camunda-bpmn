package com.formation.livraison.process.service;

import com.formation.livraison.domain.model.Commande;
import com.formation.livraison.domain.service.CommandeService;
import com.formation.livraison.process.ProcessVariables;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.variable.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Seul point du code qui fait le pont entre le metier et le moteur :
 * il cree la commande puis demarre l'instance de processus correspondante.
 */
@Service
public class LivraisonProcessService {

    private static final Logger log = LoggerFactory.getLogger(LivraisonProcessService.class);

    private final RuntimeService runtimeService;
    private final CommandeService commandeService;

    public LivraisonProcessService(RuntimeService runtimeService, CommandeService commandeService) {
        this.runtimeService = runtimeService;
        this.commandeService = commandeService;
    }

    public ProcessInstance demarrer(String client, String email, String produit, int quantite) {
        Commande commande = commandeService.creer(client, email, produit, quantite);

        Map<String, Object> variables = Variables.createVariables()
                .putValue(ProcessVariables.COMMANDE_ID, commande.getId())
                .putValue(ProcessVariables.CLIENT, client)
                .putValue(ProcessVariables.PRODUIT, produit)
                .putValue(ProcessVariables.QUANTITE, quantite);

        // La businessKey rend l'instance identifiable dans Cockpit par l'id de commande.
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                ProcessVariables.PROCESS_KEY, commande.getId(), variables);

        commandeService.lierAuProcessus(commande.getId(), instance.getId());
        log.info("Instance de processus {} demarree pour la commande {}",
                instance.getId(), commande.getId());

        return instance;
    }
}
