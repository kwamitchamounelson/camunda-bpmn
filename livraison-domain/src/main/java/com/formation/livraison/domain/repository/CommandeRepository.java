package com.formation.livraison.domain.repository;

import com.formation.livraison.domain.model.Commande;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stockage en memoire : suffisant pour l'apprentissage, et remplacable
 * par du JPA sans toucher aux delegates Camunda.
 */
@Repository
public class CommandeRepository {

    private final Map<String, Commande> commandes = new ConcurrentHashMap<>();

    public Commande save(Commande commande) {
        commandes.put(commande.getId(), commande);
        return commande;
    }

    public Optional<Commande> findById(String id) {
        return Optional.ofNullable(commandes.get(id));
    }

    public List<Commande> findAll() {
        Collection<Commande> valeurs = commandes.values();
        return List.copyOf(valeurs);
    }
}
