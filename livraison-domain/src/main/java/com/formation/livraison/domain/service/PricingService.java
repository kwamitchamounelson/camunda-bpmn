package com.formation.livraison.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Tarification simple : prix unitaire par produit. Un produit inconnu
 * est facture au tarif par defaut.
 */
@Service
public class PricingService {

    private static final Logger log = LoggerFactory.getLogger(PricingService.class);

    private static final double PRIX_DEFAUT = 100.0;

    private final Map<String, Double> tarifs = Map.of(
            "clavier", 45.0,
            "souris", 20.0,
            "ecran", 250.0,
            "casque", 80.0
    );

    public double prixUnitaire(String produit) {
        return tarifs.getOrDefault(produit.toLowerCase(), PRIX_DEFAUT);
    }

    public double calculerMontant(String produit, int quantite) {
        double montant = prixUnitaire(produit) * quantite;
        log.info("Calcul montant : {} x {} = {} EUR", quantite, produit, montant);
        return montant;
    }
}
