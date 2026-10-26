package com.formation.pret.domain.service;

import com.formation.pret.domain.model.DemandePret;
import com.formation.pret.domain.model.StatutPret;
import com.formation.pret.domain.repository.DemandePretRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class DemandePretService {

    private static final Logger log = LoggerFactory.getLogger(DemandePretService.class);

    private final DemandePretRepository repository;

    public DemandePretService(DemandePretRepository repository) {
        this.repository = repository;
    }

    public DemandePret creer(String emprunteur, String email, long montantDemande, int dureeMois,
                             long revenusMensuels, long apport, long valeurBien) {
        String id = "PRET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        DemandePret demande = new DemandePret(id, emprunteur, email, montantDemande, dureeMois,
                revenusMensuels, apport, valeurBien);
        return repository.save(demande);
    }

    public DemandePret getObligatoire(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande de pret introuvable : " + id));
    }

    public List<DemandePret> lister() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(DemandePret::getCreeeLe))
                .toList();
    }

    /**
     * Regle de completude : ce qui manque pour pouvoir instruire le dossier.
     * Liste vide = dossier complet.
     */
    public List<String> piecesManquantes(String emprunteur, long montantDemande, int dureeMois,
                                         long revenusMensuels, long valeurBien) {
        List<String> manquantes = new ArrayList<>();
        if (emprunteur == null || emprunteur.isBlank()) {
            manquantes.add("identite de l'emprunteur");
        }
        if (montantDemande <= 0) {
            manquantes.add("montant demande");
        }
        if (dureeMois <= 0) {
            manquantes.add("duree du pret");
        }
        if (revenusMensuels <= 0) {
            manquantes.add("justificatifs de revenus");
        }
        if (valeurBien <= 0) {
            manquantes.add("compromis de vente (prix du bien)");
        }
        return manquantes;
    }

    public void lierAuProcessus(String id, String processInstanceId) {
        getObligatoire(id).setProcessInstanceId(processInstanceId);
    }

    /** Reporte sur le dossier les informations fournies en completant le dossier. */
    public void majInformations(String id, long revenusMensuels, long valeurBien) {
        DemandePret demande = getObligatoire(id);
        demande.setRevenusMensuels(revenusMensuels);
        demande.setValeurBien(valeurBien);
    }

    public void changerStatut(String id, StatutPret statut) {
        DemandePret demande = getObligatoire(id);
        log.info("Demande {} : {} -> {}", id, demande.getStatut(), statut);
        demande.setStatut(statut);
    }

    /**
     * Appele depuis le BPMN par les listeners des evenements de fin sans tache
     * (classe sans suite, offre expiree, offre declinee) :
     * ${demandePretService.cloturer(execution.processBusinessKey, 'OFFRE_EXPIREE')}.
     * Le statut arrive en texte : c'est ce que JUEL sait passer simplement.
     */
    public void cloturer(String id, String statut) {
        changerStatut(id, StatutPret.valueOf(statut));
    }

    public void refuser(String id, String motif) {
        getObligatoire(id).setMotifRefus(motif);
        changerStatut(id, StatutPret.REFUSEE);
    }

    public void enregistrerOffre(String id, double tauxAnnuel, double mensualite) {
        DemandePret demande = getObligatoire(id);
        demande.setTauxAnnuel(tauxAnnuel);
        demande.setMensualite(mensualite);
        changerStatut(id, StatutPret.OFFRE_EMISE);
    }

    public void octroyer(String id, String numeroContrat) {
        getObligatoire(id).setNumeroContrat(numeroContrat);
        changerStatut(id, StatutPret.OCTROYEE);
    }
}
