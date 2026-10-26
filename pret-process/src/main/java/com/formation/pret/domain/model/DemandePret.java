package com.formation.pret.domain.model;

import java.time.Instant;

public class DemandePret {

    private final String id;
    private final String emprunteur;
    private final String email;
    private final long montantDemande;
    private final int dureeMois;
    private final long apport;
    private final Instant creeeLe;

    // Modifiables : le conseiller peut les fournir apres coup ("Completer le dossier").
    private long revenusMensuels;
    private long valeurBien;

    private StatutPret statut;
    private String processInstanceId;
    private String motifRefus;
    private Double tauxAnnuel;
    private Double mensualite;
    private String numeroContrat;

    public DemandePret(String id, String emprunteur, String email, long montantDemande, int dureeMois,
                       long revenusMensuels, long apport, long valeurBien) {
        this.id = id;
        this.emprunteur = emprunteur;
        this.email = email;
        this.montantDemande = montantDemande;
        this.dureeMois = dureeMois;
        this.revenusMensuels = revenusMensuels;
        this.apport = apport;
        this.valeurBien = valeurBien;
        this.creeeLe = Instant.now();
        this.statut = StatutPret.DEPOSEE;
    }

    public String getId() {
        return id;
    }

    public String getEmprunteur() {
        return emprunteur;
    }

    public String getEmail() {
        return email;
    }

    public long getMontantDemande() {
        return montantDemande;
    }

    public int getDureeMois() {
        return dureeMois;
    }

    public long getApport() {
        return apport;
    }

    public Instant getCreeeLe() {
        return creeeLe;
    }

    public long getRevenusMensuels() {
        return revenusMensuels;
    }

    public void setRevenusMensuels(long revenusMensuels) {
        this.revenusMensuels = revenusMensuels;
    }

    public long getValeurBien() {
        return valeurBien;
    }

    public void setValeurBien(long valeurBien) {
        this.valeurBien = valeurBien;
    }

    public StatutPret getStatut() {
        return statut;
    }

    public void setStatut(StatutPret statut) {
        this.statut = statut;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getMotifRefus() {
        return motifRefus;
    }

    public void setMotifRefus(String motifRefus) {
        this.motifRefus = motifRefus;
    }

    public Double getTauxAnnuel() {
        return tauxAnnuel;
    }

    public void setTauxAnnuel(Double tauxAnnuel) {
        this.tauxAnnuel = tauxAnnuel;
    }

    public Double getMensualite() {
        return mensualite;
    }

    public void setMensualite(Double mensualite) {
        this.mensualite = mensualite;
    }

    public String getNumeroContrat() {
        return numeroContrat;
    }

    public void setNumeroContrat(String numeroContrat) {
        this.numeroContrat = numeroContrat;
    }
}
