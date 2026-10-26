package com.formation.pret.process.rest;

import com.formation.pret.domain.model.DemandePret;
import com.formation.pret.domain.model.StatutPret;
import com.formation.pret.process.service.PretProcessService.TimerEnAttente;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Schema(name = "PretResponse", description = "Dossier de pret, avec les variables et timers de son instance")
public record PretResponse(

        @Schema(description = "Id de la demande, egalement businessKey de l'instance", example = "PRET-1A2B3C4D")
        String id,

        @Schema(example = "Alice Martin")
        String emprunteur,

        @Schema(example = "alice.martin@exemple.fr")
        String email,

        @Schema(example = "200000")
        long montantDemande,

        @Schema(example = "240")
        int dureeMois,

        @Schema(example = "6000")
        long revenusMensuels,

        @Schema(example = "40000")
        long apport,

        @Schema(example = "240000")
        long valeurBien,

        @Schema(implementation = StatutPret.class, example = "EN_ANALYSE")
        String statut,

        @Schema(nullable = true, example = "Endettement trop eleve")
        String motifRefus,

        @Schema(description = "Taux annuel de l'offre (%)", nullable = true, example = "3.2")
        Double tauxAnnuel,

        @Schema(description = "Mensualite de l'offre (EUR)", nullable = true, example = "1129.88")
        Double mensualite,

        @Schema(description = "Renseigne quand le pret est octroye", nullable = true, example = "CTR-9F8E7D6C")
        String numeroContrat,

        @Schema(example = "d4e3f2a1-0b9c-11ef-9f3a-0242ac110002", nullable = true)
        String processInstanceId,

        Instant creeeLe,

        @Schema(description = "Variables de l'instance (historique Camunda) : score, endettement, niveau de risque...")
        Map<String, Object> variables,

        @Schema(description = "Timers en attente, declenchables via /api/prets/timers/{jobId}/declencher")
        List<TimerResponse> timers) {

    @Schema(name = "TimerResponse", description = "Timer BPMN en attente sur l'instance")
    public record TimerResponse(
            @Schema(example = "5c1d2e3f-0b9c-11ef-9f3a-0242ac110002") String jobId,
            @Schema(example = "Timer_Reflexion") String activiteId,
            @Schema(example = "Délai de réflexion légal (10 j)") String libelle,
            Date echeance) {

        static TimerResponse from(TimerEnAttente timer) {
            return new TimerResponse(timer.jobId(), timer.activiteId(), timer.libelle(), timer.echeance());
        }
    }

    public static PretResponse from(DemandePret demande, Map<String, Object> variables,
                                    List<TimerEnAttente> timers) {
        return new PretResponse(
                demande.getId(),
                demande.getEmprunteur(),
                demande.getEmail(),
                demande.getMontantDemande(),
                demande.getDureeMois(),
                demande.getRevenusMensuels(),
                demande.getApport(),
                demande.getValeurBien(),
                demande.getStatut().name(),
                demande.getMotifRefus(),
                demande.getTauxAnnuel(),
                demande.getMensualite(),
                demande.getNumeroContrat(),
                demande.getProcessInstanceId(),
                demande.getCreeeLe(),
                variables == null ? Map.of() : variables,
                timers == null ? List.of() : timers.stream().map(TimerResponse::from).toList());
    }
}
