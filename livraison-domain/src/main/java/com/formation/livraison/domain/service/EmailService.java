package com.formation.livraison.domain.service;

import com.formation.livraison.domain.model.Commande;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Simulation d'envoi d'email : on se contente de tracer, l'objectif
 * etant d'observer l'execution du processus dans Cockpit.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void envoyerConfirmation(Commande commande) {
        log.info("""
                        ---- EMAIL DE CONFIRMATION ----
                        A        : {}
                        Objet    : Votre commande {} est en route
                        Contenu  : Bonjour {}, votre commande de {} x {} a bien ete preparee.
                        -------------------------------""",
                commande.getEmail(), commande.getId(), commande.getClient(),
                commande.getQuantite(), commande.getProduit());
    }
}
