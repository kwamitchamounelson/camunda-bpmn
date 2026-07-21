package com.formation.livraison.process;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * scanBasePackages remonte d'un cran pour que les @Service du module
 * livraison-domain soient detectes par le scan de composants.
 */
@SpringBootApplication(scanBasePackages = "com.formation.livraison")
@EnableProcessApplication("livraison-process-app")
public class LivraisonProcessApplication {

    public static void main(String[] args) {
        SpringApplication.run(LivraisonProcessApplication.class, args);
    }
}
