package com.formation.pret.process.rest;

import org.camunda.bpm.engine.TaskAlreadyClaimedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Meme role que RestExceptionHandler cote livraison, limite aux controleurs de pret.
 */
@RestControllerAdvice(assignableTypes = {PretController.class, TachePretController.class})
public class PretExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PretExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PretErreurResponse> requeteInvalide(IllegalArgumentException exception) {
        log.warn("Requete pret rejetee : {}", exception.getMessage());
        return ResponseEntity.badRequest().body(new PretErreurResponse(exception.getMessage()));
    }

    @ExceptionHandler(TaskAlreadyClaimedException.class)
    public ResponseEntity<PretErreurResponse> tacheDejaPrise(TaskAlreadyClaimedException exception) {
        log.warn("Attribution refusee : {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new PretErreurResponse(exception.getMessage()));
    }
}
