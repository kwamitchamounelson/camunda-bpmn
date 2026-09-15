# Processus de livraison — Camunda 7.22 / Spring Boot 3.3 / Java 21

Projet d'apprentissage : exécuter un processus « order-to-delivery » réaliste et l'observer dans Cockpit.

## Architecture

Projet Maven multi-modules, **une seule application déployée** (moteur Camunda embarqué).

| Module | Contenu | Dépend de Camunda ? |
|---|---|---|
| `livraison-domain` | `Commande`, `StockService`, `PricingService`, `EmailService`, `CommandeService` | **Non** |
| `livraison-process` | Application Spring Boot, moteur + webapps, `JavaDelegate`, table DMN, API REST | Oui |

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
| **Décision DMN** (business rule task) | « Qui doit valider ? » selon type client + montant |

### La décision DMN

Le niveau de validation n'est plus codé en dur : il est produit par une **table de décision**
`dmn/niveauValidation.dmn`, appelée par la business rule task *Déterminer le niveau de validation*.
Elle prend en entrée `typeClient` et `montant`, et renvoie la variable `validateur` :

| # | Type client | Montant | → validateur |
|---|---|---|---|
| 1 | VIP | < 10 000 € | `aucun` |
| 2 | VIP | ≥ 10 000 € | `directeurs` |
| 3 | (tout) | < 1 000 € | `aucun` |
| 4 | (tout) | 1 000–5 000 € | `managers` |
| 5 | (tout) | ≥ 5 000 € | `directeurs` |

Hit policy **FIRST** : la première ligne qui matche gagne (d'où la priorité VIP sur les règles standard).
Le résultat pilote ensuite deux choses : la passerelle *Validation requise ?* (`${validateur == "aucun"}`)
et le groupe destinataire de la tâche, assigné dynamiquement via `camunda:candidateGroups="${validateur}"`.
Ouvre le `.dmn` dans Camunda Modeler pour éditer la table visuellement.

## Câblage BPMN → Java

Le nom du bean Spring doit correspondre **exactement** à la `delegateExpression` du BPMN.

| Élément BPMN | Expression / groupe | Classe |
|---|---|---|
| `Task_VerifStock` | `#{verifierDisponibilite}` | `VerifierDisponibiliteDelegate` (écrit `disponible`) |
| `Task_Reappro` | `#{lancerReapprovisionnement}` | `LancerReapprovisionnementDelegate` |
| `Task_Montant` | `#{calculerMontant}` | `CalculerMontantDelegate` (écrit `montant`) |
| `Task_Niveau` | business rule task `decisionRef=niveauValidation` | table DMN (écrit `validateur`) |
| `Task_Validation` | user task, groupe `${validateur}` | — (écrit `approuve`, `motifRefus`) |
| `Task_Annuler` | `#{annulerCommande}` | `AnnulerCommandeDelegate` |
| `Task_Rappel` | `#{envoyerRappel}` | `EnvoyerRappelDelegate` |
| `Task_Prepa` | user task, groupe `logistique` | — |
| `Task_Email` | `#{envoyerEmailConfirmation}` | `EnvoyerEmailConfirmationDelegate` |
| `Task_Expedition` | `#{planifierExpedition}` | `PlanifierExpeditionDelegate` |
| `Task_Complete` | `#{completeOrder}` | `CompleteOrderDelegate` |

Les décisions des passerelles reposent sur ces variables de processus :
`${disponible}`, `${validateur == "aucun"}`, `${approuve}`.

## Fichiers de modèle

- BPMN : `livraison-process/src/main/resources/bpmn/processus_livraison.bpmn`
- DMN : `livraison-process/src/main/resources/dmn/niveauValidation.dmn`

Édite-les dans Camunda Modeler. Tout `.bpmn` du dossier `bpmn/` et tout `.dmn` du dossier `dmn/`
sont déployés au démarrage.

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
| Manager (valide 1 000–5 000 €) | idem | `manager` / `demo` |
| Directeur (valide ≥ 5 000 €) | idem | `directeur` / `demo` |
| Employé logistique (prépare) | idem | `employeLogistique` / `demo` |
| API REST Camunda | http://localhost:8080/engine-rest/engine | — |
| API métier | http://localhost:8080/api/commandes | — |

Les comptes et groupes (`managers`, `directeurs`, `logistique`) sont créés au démarrage par `IdentityBootstrap`.

## Catalogue de démonstration

| Produit | Stock initial | Prix unitaire |
|---|---|---|
| clavier | 25 | 45 € |
| souris | 40 | 20 € |
| ecran | 3 | 250 € |
| casque | 0 | 80 € |

## Scénarios à dérouler

Chaque appel démarre une instance ; observe-la ensuite dans **Cockpit → Processes → Processus de livraison**.
Le champ `typeClient` (`STANDARD` par défaut, ou `VIP`) pilote la décision DMN.

**1. Cas nominal (DMN → `aucun`)** — clavier x2 = 90 € :

```powershell
curl.exe -X POST http://localhost:8080/api/commandes -H "Content-Type: application/json" -d "{\"client\":\"Alice\",\"email\":\"alice@exemple.fr\",\"produit\":\"clavier\",\"quantite\":2}"
```

Le jeton s'arrête sur **Préparer la commande**. Connecte-toi en `employeLogistique`, complète la tâche
depuis la Tasklist : les branches Email + Expédition s'exécutent en parallèle, puis l'instance se termine.

**2. Validation manager (DMN → `managers`)** — clavier x25 = 1125 € :

```powershell
curl.exe -X POST http://localhost:8080/api/commandes -H "Content-Type: application/json" -d "{\"client\":\"Bob\",\"email\":\"bob@exemple.fr\",\"produit\":\"clavier\",\"quantite\":25}"
```

Le jeton s'arrête sur **Valider la commande**, offerte au groupe `managers`. Connecte-toi en `manager` :
le formulaire propose `approuve` et `motifRefus`. Sans réponse en 2 minutes, un rappel part (événement
frontière non interruptif, la tâche reste ouverte). Approuvée → préparation ; refusée → **Commande refusée**.

**3. Validation directeur (DMN → `directeurs`)** — ecran x25 = 6250 € :

```powershell
curl.exe -X POST http://localhost:8080/api/commandes -H "Content-Type: application/json" -d "{\"client\":\"Carla\",\"email\":\"carla@exemple.fr\",\"produit\":\"ecran\",\"quantite\":25}"
```

Ecran étant en rupture (stock 3), le process passe d'abord par le réappro, puis la même commande à
6250 € route la validation vers le groupe `directeurs` (connecte-toi en `directeur`). Un seul parcours
cumule ainsi rupture + réappro + décision DMN.

**4. Client VIP (DMN → `aucun` malgré le montant)** — clavier x25 = 1125 €, mais VIP :

```powershell
curl.exe -X POST http://localhost:8080/api/commandes -H "Content-Type: application/json" -d "{\"client\":\"Vera\",\"email\":\"vera@exemple.fr\",\"produit\":\"clavier\",\"quantite\":25,\"typeClient\":\"VIP\"}"
```

Même montant que le scénario 2, mais la règle VIP de la table DMN saute la validation : le jeton va
directement à la préparation. C'est l'illustration directe de l'intérêt du DMN.

**5. Rupture de stock + réapprovisionnement** — casque (stock 0) :

```powershell
curl.exe -X POST http://localhost:8080/api/commandes -H "Content-Type: application/json" -d "{\"client\":\"Eric\",\"email\":\"eric@exemple.fr\",\"produit\":\"casque\",\"quantite\":1}"
```

Le jeton part sur **Lancer réapprovisionnement** puis attend 30 s sur le timer. Dans Cockpit tu vois
le jeton stationné sur l'événement d'attente. Après 30 s, il reboucle sur la vérification : le stock
est désormais suffisant, le processus continue.

Suivre une commande côté métier : `GET http://localhost:8080/api/commandes/{id}`.

## Base de données

H2 **en mémoire** (`jdbc:h2:mem:camunda-livraison`) : rien n'est écrit sur disque, ni schéma, ni
historique. Chaque démarrage repart d'un moteur vierge, le BPMN et le DMN sont redéployés, et les
comptes de démonstration sont recréés.

C'est un choix assumé, pas une commodité : `CommandeRepository` stocke les commandes dans une map en
mémoire. Une base moteur persistante survivrait donc aux commandes et laisserait après redémarrage
des instances en cours dont la commande a disparu — tâches impossibles à compléter, avec l'erreur
`Commande introuvable`. Les deux stockages sont maintenant alignés sur le même cycle de vie.

Conséquence à connaître : l'historique Cockpit ne couvre que la session en cours. Pour une démo cela
suffit largement ; il faut simplement éviter de redémarrer le backend avec des instances en vol.

## Tests

```powershell
mvn -pl livraison-process test
```

`ProcessusLivraisonTest` couvre les chemins clés : DMN → `aucun` / `managers` / `directeurs`, client VIP,
validation rejetée, et boucle de réapprovisionnement (le timer est déclenché manuellement via
`ManagementService`, car le job executor est désactivé en test).

## Pistes d'exercices

1. Ajouter une colonne de sortie à la table DMN (ex. `delaiLivraison`) et l'exploiter dans le process.
2. Rendre l'événement frontière **interruptif** : annuler la commande si le manager ne répond pas.
3. Passer `Task_Email` en `camunda:asyncBefore="true"` et observer le job dans Cockpit.
4. Lever une exception dans un delegate pour voir apparaître un **incident**, puis le rejouer.
5. Ajouter une passerelle après la réappro : au bout de N tentatives, annuler au lieu de reboucler.
