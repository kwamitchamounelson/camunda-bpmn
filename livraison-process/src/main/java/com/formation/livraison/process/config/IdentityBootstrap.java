package com.formation.livraison.process.config;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cree les comptes de demonstration attendus par le BPMN :
 *  - Task_Prepa      -> candidateGroups="logistique"
 *  - Task_Validation -> candidateGroups="managers"
 * Sans eux, les taches existeraient sans destinataire capable de se connecter.
 */
@Configuration
public class IdentityBootstrap {

    private static final Logger log = LoggerFactory.getLogger(IdentityBootstrap.class);

    @Bean
    public ApplicationRunner creerUtilisateursDemo(IdentityService identityService) {
        return args -> {
            creerGroupe(identityService, "logistique", "Logistique");
            creerGroupe(identityService, "managers", "Managers");

            creerUtilisateur(identityService, "employeLogistique", "Employe", "Logistique",
                    "logistique@formation.local", "logistique");
            creerUtilisateur(identityService, "manager", "Marie", "Manager",
                    "manager@formation.local", "managers");
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
