package com.formation.pret.domain.repository;

import com.formation.pret.domain.model.DemandePret;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stockage en memoire, comme CommandeRepository : il disparait a l'arret,
 * en meme temps que la base H2 en memoire du moteur.
 */
@Repository
public class DemandePretRepository {

    private final Map<String, DemandePret> demandes = new ConcurrentHashMap<>();

    public DemandePret save(DemandePret demande) {
        demandes.put(demande.getId(), demande);
        return demande;
    }

    public Optional<DemandePret> findById(String id) {
        return Optional.ofNullable(demandes.get(id));
    }

    public List<DemandePret> findAll() {
        return List.copyOf(demandes.values());
    }
}
