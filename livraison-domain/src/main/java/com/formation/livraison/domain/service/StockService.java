package com.formation.livraison.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;
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

    /**
     * Photo triee de l'etat du stock. Copie defensive : l'appelant ne peut pas
     * modifier le magasin en passant par la map retournee.
     */
    public Map<String, Integer> tousLesStocks() {
        return new TreeMap<>(stocks);
    }

    /**
     * Fixe la quantite d'un produit, en creant l'entree si le produit est inconnu.
     * Sert a mettre en place un scenario : passer "casque" a 0 pour forcer la rupture,
     * ou le remonter pour debloquer une instance en attente de reapprovisionnement.
     */
    public int definir(String produit, int quantite) {
        if (quantite < 0) {
            throw new IllegalArgumentException("Quantite negative interdite : " + quantite);
        }
        String cle = cle(produit);
        stocks.put(cle, quantite);
        log.info("Stock fixe : produit={}, stock={}", cle, quantite);
        return quantite;
    }

    /**
     * Applique un mouvement signe (entree positive, sortie negative).
     * Le calcul se fait dans un compute() : si le resultat est negatif,
     * l'exception remonte et le stock reste inchange.
     */
    public int ajuster(String produit, int mouvement) {
        String cle = cle(produit);
        int nouveau = stocks.compute(cle, (produitCle, courant) -> {
            int restant = (courant == null ? 0 : courant) + mouvement;
            if (restant < 0) {
                throw new IllegalArgumentException("Mouvement refuse : stock de %s = %d, mouvement demande = %d"
                        .formatted(produitCle, courant == null ? 0 : courant, mouvement));
            }
            return restant;
        });
        log.info("Mouvement de stock : produit={}, mouvement={}, stock={}", cle, mouvement, nouveau);
        return nouveau;
    }

    private String cle(String produit) {
        if (produit == null || produit.isBlank()) {
            throw new IllegalArgumentException("Produit obligatoire");
        }
        return produit.toLowerCase();
    }

    /**
     * Reapprovisionne le produit pour couvrir la quantite demandee, avec une marge.
     * Rend le prochain controle de disponibilite positif : la boucle du processus
     * finit donc par sortir de l'etat de rupture.
     */
    public int reapprovisionner(String produit, int quantiteDemandee) {
        String cle = produit.toLowerCase();
        int ajout = Math.max(quantiteDemandee, 10) + 5;
        int nouveau = stocks.merge(cle, ajout, Integer::sum);
        log.info("Reapprovisionnement : produit={}, ajout={}, stock={}", produit, ajout, nouveau);
        return nouveau;
    }
}
