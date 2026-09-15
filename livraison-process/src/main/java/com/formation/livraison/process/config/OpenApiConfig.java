package com.formation.livraison.process.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadonnees affichees en tete de Swagger UI (/swagger-ui.html).
 * Seule l'API metier /api/** est documentee : l'API REST de Camunda (/engine-rest)
 * est servie par JAX-RS et reste donc invisible pour springdoc, qui ne scanne
 * que les controleurs Spring MVC.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI livraisonOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Livraison")
                        .version("1.0.0")
                        .description("""
                                API metier du processus de livraison Camunda 7.

                                Creer une commande demarre une instance du processus `processus_livraison`
                                dont la businessKey est l'id de commande : l'instance est donc retrouvable
                                telle quelle dans Cockpit.

                                Les taches utilisateur (preparation, validation) ne passent pas par cette API :
                                elles se traitent dans la Tasklist Camunda (http://localhost:8080/camunda).
                                """)
                        .license(new License().name("Support de formation")))
                .externalDocs(new ExternalDocumentation()
                        .description("Cockpit / Tasklist / Admin Camunda")
                        .url("http://localhost:8080/camunda"));
    }
}
