# Processus de livraison — Camunda 7.22 / Spring Boot 3.3 / Java 21

Projet d'apprentissage : exécuter un processus « order-to-delivery » réaliste et l'observer dans Cockpit.

## Architecture

Projet Maven multi-modules, **une seule application déployée** (moteur Camunda embarqué).

| Module | Contenu | Dépend de Camunda ? |
|---|---|---|
| `livraison-domain` | `Commande`, `StockService`, `PricingService`, `EmailService`, `CommandeService` | **Non** |
| `livraison-process` | Application Spring Boot, moteur + webapps, `JavaDelegate`, API REST | Oui |

Le module métier ignore totalement qu'il est orchestré. C'est ce qui te permettra plus tard de
passer en *External Task* (worker séparé) ou vers Camunda 8 sans réécrire la logique métier.

## Le processus

```
Commande passée
      │
      ▼
Vérifier disponibilité (service)
      │
      ◆ Stock disponible ? ── rupture ──▶ Lancer réappro (service)
      │                                        │
   disponible                            Attendre 30 s (timer) ──┐
      │                                                          │
      ▼          ◀───────────── boucle: re-vérifier le stock ────┘
Calculer le montant (service)
      │
      ◆ Montant > 1000 € ? ── oui ──▶ Valider la commande (user, groupe managers)
      │                                   │        ╎ (timer 2 min non interruptif)
     non                                  │        ╰▶ Envoyer un rappel (service) ▶ fin
      │                                   ◆ Approuvée ?
      │                                   │        └─ non ─▶ Annuler (service) ▶ Commande refusée
      ▼          ◀──────── oui ───────────┘
Préparer la commande (user, groupe logistique)
      │
      ╫ split parallèle
      ├──▶ Envoyer email de confirmation (service) ──┐
      └──▶ Planifier l'expédition (service) ─────────┤
      ╫ join parallèle ◀──────────────────────────────┘
      ▼
Compléter la commande (service)
      ▼
Commande livrée
```

Ce que le modèle illustre :

| Concept BPMN | Où |
|---|---|
| Tâche automatique (service task + JavaDelegate) | Vérifier, Réappro, Montant, Email, Expédition, Annuler, Compléter |
| Tâche manuelle (user task) | Valider (managers), Préparer (logistique) |
| Passerelle exclusive (choix conditionnel) | Stock disponible ?, Montant > 1000 ?, Approuvée ? |
| Passerelle parallèle (split/join) | Email + Expédition en simultané |
| Tâche programmée (timer intermédiaire) | Attente de réapprovisionnement (30 s) |
| Événement frontière non interruptif (timer) | Rappel manager après 2 min |
| Boucle | Rupture → réappro → attente → re-vérification |
| Formulaire de tâche | Champs `approuve` / `motifRefus` sur la validation |

## Câblage BPMN → Java

Le nom du bean Spring doit correspondre **exactement** à la `delegateExpression` du BPMN.

| Élément BPMN | Expression / groupe | Classe |
|---|---|---|
| `Task_VerifStock` | `#{verifierDisponibilite}` | `VerifierDisponibiliteDelegate` (écrit `disponible`) |
| `Task_Reappro` | `#{lancerReapprovisionnement}` | `LancerReapprovisionnementDelegate` |
| `Task_Montant` | `#{calculerMontant}` | `CalculerMontantDelegate` (écrit `montant`) |
| `Task_Validation` | user task, groupe `managers` | — (écrit `approuve`, `motifRefus`) |
| `Task_Annuler` | `#{annulerCommande}` | `AnnulerCommandeDelegate` |
| `Task_Rappel` | `#{envoyerRappel}` | `EnvoyerRappelDelegate` |
| `Task_Prepa` | user task, groupe `logistique` | — |
| `Task_Email` | `#{envoyerEmailConfirmation}` | `EnvoyerEmailConfirmationDelegate` |
| `Task_Expedition` | `#{planifierExpedition}` | `PlanifierExpeditionDelegate` |
| `Task_Complete` | `#{completeOrder}` | `CompleteOrderDelegate` |

Les décisions des passerelles reposent sur ces variables de processus :
`${disponible}`, `${montant > 1000}`, `${approuve}`.

## Fichier BPMN

Source unique : `livraison-process/src/main/resources/bpmn/processus_livraison.bpmn`.
C'est ce fichier qu'il faut éditer (ouvre-le dans Camunda Modeler) ; tout `.bpmn` de ce dossier
est déployé au démarrage.

`camunda:historyTimeToLive="P180D"` est présent sur le process : **obligatoire depuis Camunda 7.20**,
sans lui le déploiement échoue au démarrage.

## Démarrer

```powershell
mvn clean install
mvn -pl livraison-process spring-boot:run
```

## Accès

| Quoi | URL | Identifiants |
|---|---|---|
| Cockpit / Tasklist / Admin | http://localhost:8080/camunda/app/welcome/ | `demo` / `demo` |
| Manager (valide les commandes) | idem | `manager` / `demo` |
| Employé logistique (prépare) | idem | `employeLogistique` / `demo` |
| API REST Camunda | http://localhost:8080/engine-rest/engine | — |
| API métier | http://localhost:8080/api/commandes | — |

Les comptes et groupes (`managers`, `logistique`) sont créés au démarrage par `IdentityBootstrap`.

## Catalogue de démonstration

| Produit | Stock initial | Prix unitaire |
|---|---|---|
| clavier | 25 | 45 € |
| souris | 40 | 20 € |
| ecran | 3 | 250 € |
| casque | 0 | 80 € |

## Trois scénarios à dérouler

Chaque appel démarre une instance ; observe-la ensuite dans **Cockpit → Processes → Processus de livraison**.

**1. Cas nominal (ni validation, ni rupture)** — clavier x2 = 90 € :

```powershell
curl.exe -X POST http://localhost:8080/api/commandes -H "Content-Type: application/json" -d "{\"client\":\"Alice\",\"email\":\"alice@exemple.fr\",\"produit\":\"clavier\",\"quantite\":2}"
```

Le jeton s'arrête sur **Préparer la commande**. Connecte-toi en `employeLogistique`, complète la tâche
depuis la Tasklist : les branches Email + Expédition s'exécutent en parallèle, puis l'instance se termine.

**2. Validation manager** — clavier x25 = 1125 € (> 1000 €) :

```powershell
curl.exe -X POST http://localhost:8080/api/commandes -H "Content-Type: application/json" -d "{\"client\":\"Bob\",\"email\":\"bob@exemple.fr\",\"produit\":\"clavier\",\"quantite\":25}"
```

Le jeton s'arrête sur **Valider la commande**. Connecte-toi en `manager` : le formulaire propose
`approuve` (booléen) et `motifRefus`. Si tu ne complètes pas dans les 2 minutes, un rappel part
(événement frontière non interruptif — visible dans les logs, la tâche reste ouverte).
Approuvée → préparation ; refusée → **Commande refusée**.

**3. Rupture de stock + réapprovisionnement** — casque (stock 0) :

```powershell
curl.exe -X POST http://localhost:8080/api/commandes -H "Content-Type: application/json" -d "{\"client\":\"David\",\"email\":\"david@exemple.fr\",\"produit\":\"casque\",\"quantite\":1}"
```

Le jeton part sur **Lancer réapprovisionnement** puis attend 30 s sur le timer. Dans Cockpit tu vois
le jeton stationné sur l'événement d'attente. Après 30 s, il reboucle sur la vérification : le stock
est désormais suffisant, le processus continue.

Suivre une commande côté métier : `GET http://localhost:8080/api/commandes/{id}`.

## Base de données

H2 en fichier (`livraison-process/data/camunda-db.mv.db`) : l'historique survit aux redémarrages.
Pour repartir de zéro, supprime le dossier `data/`.

## Tests

```powershell
mvn -pl livraison-process test
```

`ProcessusLivraisonTest` couvre les quatre chemins : livraison simple, validation approuvée,
validation rejetée, et boucle de réapprovisionnement (le timer est déclenché manuellement via
`ManagementService`, car le job executor est désactivé en test).

## Pistes d'exercices

1. Remplacer le calcul de montant par une **table de décision DMN** (business rule task).
2. Rendre l'événement frontière **interruptif** : annuler la commande si le manager ne répond pas.
3. Passer `Task_Email` en `camunda:asyncBefore="true"` et observer le job dans Cockpit.
4. Lever une exception dans un delegate pour voir apparaître un **incident**, puis le rejouer.
5. Ajouter une passerelle après la réappro : au bout de N tentatives, annuler au lieu de reboucler.
