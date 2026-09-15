# livraison-front

Vitrine React qui pilote le processus Camunda **uniquement via l'API REST `/api/**`** du module
`livraison-process`. Aucune authentification, aucun appel direct au moteur.

## Démarrer

```bash
# 1. le backend, depuis la racine du projet (ou via IntelliJ)
mvn -pl livraison-process -am spring-boot:run

# 2. le front
cd livraison-front
npm install
npm run dev
```

Le front s'ouvre sur <http://localhost:5173>. Les appels `/api/**` sont relayés vers
`http://localhost:8080` par le proxy de Vite (`vite.config.js`) : le navigateur ne voit qu'une
seule origine, il n'y a donc pas de CORS à configurer côté Spring.

## Les quatre écrans

| Écran | Ce qu'il montre | Endpoints |
|---|---|---|
| **Boutique** | Catalogue avec stock temps réel, création de commande | `GET /api/stocks`, `POST /api/commandes` |
| **Mes commandes** | Suivi façon e-commerce, lien direct vers l'instance dans Cockpit | `GET /api/commandes`, `GET /api/taches` |
| **Back-office** | Files de validation et de préparation, claim/libération, décisions | `GET /api/taches`, `POST /api/taches/{id}/...` |
| **Stocks** | Consultation et pilotage du magasin | `GET/PUT /api/stocks`, `POST /api/stocks/{p}/mouvements` |

Le panneau **Appels REST** à droite journalise chaque action déclenchée. Les rafraîchissements
automatiques (toutes les 2 s) en sont volontairement exclus pour garder le journal lisible pendant
une démonstration.

## Déroulé de démonstration suggéré

1. **Cas nominal sans validation** — Alice, client Standard, 2 claviers (90 €). Le DMN renvoie
   `aucun`, la commande file directement en préparation. Dans le back-office, « Marquer comme
   préparée » la fait passer à `LIVREE` avec un numéro de suivi.
2. **Validation manager** — 25 claviers (1125 €). La règle DMN `[1000..5000)` route vers le groupe
   `managers` : la tâche apparaît dans la colonne Validations. Montrer le jeton arrêté sur
   `Task_Validation` dans Cockpit, puis approuver.
3. **Client VIP** — même panier en VIP : la validation est sautée, ce que la timeline de la commande
   montre directement.
4. **Refus** — refuser une validation avec un motif : la commande passe `REFUSEE` et l'instance se
   termine sur `End_Refus`.
5. **Rupture de stock** — onglet Stocks, mettre `casque` à 0, commander un casque. L'instance boucle
   sur le timer de 30 s (visible dans Cockpit). Remonter le stock pendant l'attente : au contrôle
   suivant, la commande repart.

## Variables

`VITE_BACKEND` change l'URL utilisée pour les liens Cockpit et Tasklist
(défaut `http://localhost:8080`). Les appels API, eux, passent toujours par le proxy.
