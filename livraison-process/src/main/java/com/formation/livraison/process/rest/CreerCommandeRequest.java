package com.formation.livraison.process.rest;

public record CreerCommandeRequest(String client, String email, String produit, int quantite) {
}
