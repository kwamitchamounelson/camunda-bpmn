package com.formation.livraison.process;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * scanBasePackages remonte d'un cran pour que les @Service du module
 * livraison-domain soient detectes par le scan de composants, et inclut
 * com.formation.pret : le module pret-process n'a pas d'application propre,
 * ses delegates et son API tournent dans ce moteur. Ses BPMN/DMN sont deployes
 * par le meme deployment-resource-pattern (classpath*: couvre tous les jars).
 *
 * Pas de @EnableProcessApplication ici : l'annotation remplace le deploiement
 * pilote par camunda.bpm.deployment-resource-pattern par celui d'une process
 * application (META-INF/processes.xml). Sans ce fichier, plus rien n'est deploye
 * et startProcessInstanceByKey echoue avec "no processes deployed with key ...".
 */
@SpringBootApplication(scanBasePackages = {"com.formation.livraison", "com.formation.pret"})
public class LivraisonProcessApplication {

    public static void main(String[] args) {
        SpringApplication.run(LivraisonProcessApplication.class, args);
    }
}
