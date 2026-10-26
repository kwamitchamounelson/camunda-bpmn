package com.formation.pret.process.config;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Comptes de demonstration attendus par les candidateGroups du BPMN de pret :
 * conseillers, experts, analystes, comiteCredit, notaires
 * (ids alphanumeriques : Camunda refuse les tirets dans les identifiants).
 * Mot de passe "demo" pour tous, comme pour la livraison.
 */
@Configuration
public class PretIdentityBootstrap {

    private static final Logger log = LoggerFactory.getLogger(PretIdentityBootstrap.class);

    @Bean
    public ApplicationRunner creerUtilisateursPret(IdentityService identityService) {
        return args -> {
            creerGroupe(identityService, "conseillers", "Conseillers clientele");
            creerGroupe(identityService, "experts", "Experts immobiliers");
            creerGroupe(identityService, "analystes", "Analystes credit");
            creerGroupe(identityService, "comiteCredit", "Comite de credit");
            creerGroupe(identityService, "notaires", "Notaires");

            creerUtilisateur(identityService, "conseiller", "Claire", "Conseil",
                    "conseiller@formation.local", "conseillers");
            creerUtilisateur(identityService, "expert", "Eric", "Expert",
                    "expert@formation.local", "experts");
            creerUtilisateur(identityService, "analyste", "Anna", "Analyste",
                    "analyste@formation.local", "analystes");
            creerUtilisateur(identityService, "comite", "Cyril", "Comite",
                    "comite@formation.local", "comiteCredit");
            creerUtilisateur(identityService, "notaire", "Nadia", "Notaire",
                    "notaire@formation.local", "notaires");
        };
    }

    private void creerGroupe(IdentityService identityService, String id, String nom) {
        if (identityService.createGroupQuery().groupId(id).count() > 0) {
            return;
        }
        Group groupe = identityService.newGroup(id);
        groupe.setName(nom);
        groupe.setType("WORKFLOW");
        identityService.saveGroup(groupe);
        log.info("Groupe '{}' cree", id);
    }

    private void creerUtilisateur(IdentityService identityService, String userId,
                                  String prenom, String nom, String email, String groupe) {
        if (identityService.createUserQuery().userId(userId).count() > 0) {
            return;
        }
        User user = identityService.newUser(userId);
        user.setFirstName(prenom);
        user.setLastName(nom);
        user.setPassword("demo");
        user.setEmail(email);
        identityService.saveUser(user);
        identityService.createMembership(userId, groupe);
        log.info("Utilisateur '{}' cree (groupe '{}', mot de passe : demo)", userId, groupe);
    }
}
