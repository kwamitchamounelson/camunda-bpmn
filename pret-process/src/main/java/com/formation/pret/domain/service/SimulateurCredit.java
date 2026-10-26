package com.formation.pret.domain.service;

import org.springframework.stereotype.Service;

/**
 * Calculs de credit amortissable a mensualites constantes.
 */
@Service
public class SimulateurCredit {

    /** Taux annuel (%) applique tant que l'analyste n'a pas propose le sien. */
    public static final double TAUX_REFERENCE = 3.5;

    public double mensualite(long capital, int dureeMois, double tauxAnnuelPourcent) {
        if (tauxAnnuelPourcent <= 0) {
            return arrondir((double) capital / dureeMois);
        }
        double tauxMensuel = tauxAnnuelPourcent / 100 / 12;
        double mensualite = capital * tauxMensuel / (1 - Math.pow(1 + tauxMensuel, -dureeMois));
        return arrondir(mensualite);
    }

    /** Part des revenus absorbee par la mensualite, en %. */
    public double tauxEndettement(double mensualite, long revenusMensuels) {
        return Math.round(mensualite / revenusMensuels * 1000) / 10.0;
    }

    private static double arrondir(double montant) {
        return Math.round(montant * 100) / 100.0;
    }
}
