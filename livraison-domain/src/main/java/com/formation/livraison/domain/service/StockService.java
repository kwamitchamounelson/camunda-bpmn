package com.formation.livraison.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final Map<String, Integer> stocks = new ConcurrentHashMap<>(Map.of(
            "clavier", 25,
            "souris", 40,
            "ecran", 3,
            "casque", 0
    ));

    /**
     * Un produit inconnu du catalogue est considere comme indisponible.
     */
    public boolean estDisponible(String produit, int quantite) {
        int restant = stocks.getOrDefault(produit.toLowerCase(), 0);
        boolean disponible = restant >= quantite;
        log.info("Verification stock : produit={}, demande={}, restant={}, disponible={}",
                produit, quantite, restant, disponible);
        return disponible;
    }

    public int stockRestant(String produit) {
        return stocks.getOrDefault(produit.toLowerCase(), 0);
    }
}
