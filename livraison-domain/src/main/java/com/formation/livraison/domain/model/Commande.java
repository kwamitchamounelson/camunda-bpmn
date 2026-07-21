package com.formation.livraison.domain.model;

import java.time.Instant;

public class Commande {

    private final String id;
    private final String client;
    private final String email;
    private final String produit;
    private final int quantite;
    private final Instant creeeLe;

    private StatutCommande statut;
    private String processInstanceId;

    public Commande(String id, String client, String email, String produit, int quantite) {
        this.id = id;
        this.client = client;
        this.email = email;
        this.produit = produit;
        this.quantite = quantite;
        this.creeeLe = Instant.now();
        this.statut = StatutCommande.NOUVELLE;
    }

    public String getId() {
        return id;
    }

    public String getClient() {
        return client;
    }

    public String getEmail() {
        return email;
    }

    public String getProduit() {
        return produit;
    }

    public int getQuantite() {
        return quantite;
    }

    public Instant getCreeeLe() {
        return creeeLe;
    }

    public StatutCommande getStatut() {
        return statut;
    }

    public void setStatut(StatutCommande statut) {
        this.statut = statut;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public String toString() {
        return "Commande{id=%s, client=%s, produit=%s, quantite=%d, statut=%s}"
                .formatted(id, client, produit, quantite, statut);
    }
}
