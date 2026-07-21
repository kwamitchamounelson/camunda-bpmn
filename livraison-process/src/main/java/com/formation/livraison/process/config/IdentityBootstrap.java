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
 * Le BPMN affecte Task_2 a l'utilisateur "employeLogistique". Sans ce compte,
 * la tache existe mais personne ne peut se connecter pour la traiter.
 */
@Configuration
public class IdentityBootstrap {

    private static final Logger log = LoggerFactory.getLogger(IdentityBootstrap.class);

    private static final String USER_ID = "employeLogistique";
    private static final String GROUP_ID = "logistique";

    @Bean
    public ApplicationRunner creerUtilisateursDemo(IdentityService identityService) {
        return args -> {
            creerGroupeSiAbsent(identityService);
            creerUtilisateurSiAbsent(identityService);
        };
    }

    private void creerGroupeSiAbsent(IdentityService identityService) {
        if (identityService.createGroupQuery().groupId(GROUP_ID).count() > 0) {
            return;
        }
        Group groupe = identityService.newGroup(GROUP_ID);
        groupe.setName("Logistique");
        groupe.setType("WORKFLOW");
        identityService.saveGroup(groupe);
        log.info("Groupe '{}' cree", GROUP_ID);
    }

    private void creerUtilisateurSiAbsent(IdentityService identityService) {
        if (identityService.createUserQuery().userId(USER_ID).count() > 0) {
            return;
        }
        User user = identityService.newUser(USER_ID);
        user.setFirstName("Employe");
        user.setLastName("Logistique");
        user.setPassword("demo");
        user.setEmail("logistique@formation.local");
        identityService.saveUser(user);
        identityService.createMembership(USER_ID, GROUP_ID);
        log.info("Utilisateur '{}' cree (mot de passe : demo)", USER_ID);
    }
}
