package com.formation.livraison.process.rest;

import com.formation.livraison.domain.service.PricingService;
import com.formation.livraison.domain.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@Tag(name = "Stocks", description = "Consultation et pilotage du stock utilise par Task_VerifStock")
public class StockController {

    private final StockService stockService;
    private final PricingService pricingService;

    public StockController(StockService stockService, PricingService pricingService) {
        this.stockService = stockService;
        this.pricingService = pricingService;
    }

    @Operation(
            summary = "Consulter tout le stock",
            description = """
                    Etat courant du magasin, trie par produit. Le stock vit en memoire :
                    il repart aux valeurs initiales a chaque redemarrage
                    (clavier 25, souris 40, ecran 3, casque 0).
                    """)
    @ApiResponse(responseCode = "200", description = "Etat du stock")
    @GetMapping
    public List<StockResponse> lister() {
        return stockService.tousLesStocks().entrySet().stream()
                .map(entree -> versReponse(entree.getKey(), entree.getValue()))
                .toList();
    }

    @Operation(summary = "Consulter le stock d'un produit",
            description = "Un produit hors catalogue renvoie une quantite de zero : "
                    + "c'est ce qui le rend indisponible pour le processus.")
    @ApiResponse(responseCode = "200", description = "Stock du produit")
    @GetMapping("/{produit}")
    public StockResponse detail(
            @Parameter(description = "Nom du produit, insensible a la casse", example = "casque")
            @PathVariable String produit) {
        return versReponse(produit.toLowerCase(), stockService.stockRestant(produit));
    }

    @Operation(
            summary = "Fixer le stock d'un produit",
            description = """
                    Ecrase la quantite courante, et cree l'entree si le produit est inconnu.

                    Scenario de formation : mettre `casque` a 0 force la rupture et envoie
                    l'instance dans la boucle de reapprovisionnement (timer de 30 s) ;
                    remonter la quantite pendant l'attente fait sortir l'instance de la boucle
                    au controle suivant.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock mis a jour"),
            @ApiResponse(responseCode = "400", description = "Quantite negative", content = @Content)
    })
    @PutMapping("/{produit}")
    public StockResponse definir(@PathVariable String produit,
                                 @RequestBody DefinirStockRequest request) {
        int quantite = stockService.definir(produit, request.quantite());
        return versReponse(produit.toLowerCase(), quantite);
    }

    @Operation(summary = "Enregistrer un mouvement de stock",
            description = "Ajoute (valeur positive) ou retire (valeur negative) au stock courant. "
                    + "Une sortie superieure au stock disponible est refusee sans rien modifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mouvement applique"),
            @ApiResponse(responseCode = "400", description = "Mouvement rendant le stock negatif",
                    content = @Content)
    })
    @PostMapping("/{produit}/mouvements")
    public StockResponse mouvement(@PathVariable String produit,
                                   @RequestBody MouvementStockRequest request) {
        int quantite = stockService.ajuster(produit, request.mouvement());
        return versReponse(produit.toLowerCase(), quantite);
    }

    private StockResponse versReponse(String produit, int quantite) {
        return new StockResponse(produit, quantite, pricingService.prixUnitaire(produit));
    }
}
