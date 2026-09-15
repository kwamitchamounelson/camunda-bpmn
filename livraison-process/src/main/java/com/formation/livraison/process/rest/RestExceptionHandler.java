package com.formation.livraison.process.rest;

import org.camunda.bpm.engine.TaskAlreadyClaimedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les erreurs fonctionnelles en reponses lisibles depuis Swagger.
 * Sans cela, un id inconnu ressort en 500 avec un corps vide de message,
 * ce qui rend l'API penible a explorer.
 */
@RestControllerAdvice(assignableTypes = {CommandeController.class, TacheController.class, StockController.class})
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    /**
     * Couvre aussi bien les ressources inconnues ("Commande introuvable : ...")
     * que les parametres invalides ("Quantite negative interdite : ...").
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErreurResponse> requeteInvalide(IllegalArgumentException exception) {
        log.warn("Requete rejetee : {}", exception.getMessage());
        return ResponseEntity.badRequest().body(new ErreurResponse(exception.getMessage()));
    }

    @ExceptionHandler(TaskAlreadyClaimedException.class)
    public ResponseEntity<ErreurResponse> tacheDejaPrise(TaskAlreadyClaimedException exception) {
        log.warn("Attribution refusee : {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErreurResponse(exception.getMessage()));
    }
}
