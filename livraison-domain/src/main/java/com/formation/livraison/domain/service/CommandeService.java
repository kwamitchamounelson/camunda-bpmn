package com.formation.livraison.domain.service;

import com.formation.livraison.domain.model.Commande;
import com.formation.livraison.domain.model.StatutCommande;
import com.formation.livraison.domain.repository.CommandeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CommandeService {

    private static final Logger log = LoggerFactory.getLogger(CommandeService.class);

    private final CommandeRepository repository;

    public CommandeService(CommandeRepository repository) {
        this.repository = repository;
    }

    public Commande creer(String client, String email, String produit, int quantite) {
        String id = "CMD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Commande commande = new Commande(id, client, email, produit, quantite);
        repository.save(commande);
        log.info("Commande creee : {}", commande);
        return commande;
    }

    public Commande getObligatoire(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Commande introuvable : " + id));
    }

    public List<Commande> lister() {
        return repository.findAll();
    }

    public Commande changerStatut(String id, StatutCommande statut) {
        Commande commande = getObligatoire(id);
        commande.setStatut(statut);
        repository.save(commande);
        log.info("Commande {} : statut -> {}", id, statut);
        return commande;
    }

    public void lierAuProcessus(String id, String processInstanceId) {
        Commande commande = getObligatoire(id);
        commande.setProcessInstanceId(processInstanceId);
        repository.save(commande);
    }
}
