# Processus de livraison — Camunda 7.22 / Spring Boot 3.3 / Java 21

Projet d'apprentissage : exécuter le processus `ProcessusLivraison` et l'observer dans Cockpit.

## Architecture

Projet Maven multi-modules, **une seule application déployée** (moteur Camunda embarqué).

| Module | Contenu | Dépend de Camunda ? |
|---|---|---|
| `livraison-domain` | `Commande`, `StockService`, `EmailService`, `CommandeService` | **Non** |
| `livraison-process` | Application Spring Boot, moteur + webapps, `JavaDelegate`, API REST | Oui |

Le module métier ignore totalement qu'il est orchestré. C'est ce qui te permettra plus tard de
passer en *External Task* (worker séparé) ou vers Camunda 8 sans réécrire la logique métier.

> Pourquoi pas deux applications séparées ? Cela imposerait le pattern External Task dès le départ,
> qui masque justement ce que tu veux voir : delegates, variables, incidents, historique.

## Câblage BPMN → Java

Le nom du bean Spring doit correspondre **exactement** à la `delegateExpression` du BPMN.

| Élément BPMN | Expression | Classe |
|---|---|---|
| `Task_1` Vérifier disponibilité | `#{verifierDisponibilite}` | `VerifierDisponibiliteDelegate` |
| `Task_2` Préparer la commande | *user task*, assignee `employeLogistique` | — |
| `Task_3` Envoyer email | `#{envoyerEmailConfirmation}` | `EnvoyerEmailConfirmationDelegate` |
| `Task_4` Compléter la commande | `#{completeOrder}` | `CompleteOrderDelegate` |

## Fichier BPMN

Source unique : `livraison-process/src/main/resources/bpmn/processus_livraison.bpmn`.
C'est ce fichier qu'il faut éditer (ouvre-le dans Camunda Modeler) ; tout `.bpmn` de ce dossier
est déployé au démarrage.

Deux ajouts par rapport au modèle d'origine :

- `camunda:historyTimeToLive="P180D"` — **obligatoire depuis Camunda 7.20**. Sans lui, le
  déploiement échoue au démarrage (`historyTimeToLive` non défini).
- `name="Processus de livraison"` — confort d'affichage dans Cockpit.

## Démarrer

```powershell
mvn clean install
mvn -pl livraison-process spring-boot:run
```

## Accès

| Quoi | URL | Identifiants |
|---|---|---|
| Cockpit / Tasklist / Admin | http://localhost:8080/camunda/app/welcome/ | `demo` / `demo` |
| Tasklist en tant qu'employé | idem | `employeLogistique` / `demo` |
| API REST Camunda | http://localhost:8080/engine-rest/engine | — |
| API métier | http://localhost:8080/api/commandes | — |

Le compte `employeLogistique` est créé au démarrage par `IdentityBootstrap` : sans lui, la tâche
`Task_2` existerait sans que personne ne puisse se connecter pour la traiter.

## Dérouler un cas complet

1. Démarrer une instance :

```powershell
curl.exe -X POST http://localhost:8080/api/commandes -H "Content-Type: application/json" -d "{\"client\":\"Alice\",\"email\":\"alice@exemple.fr\",\"produit\":\"clavier\",\"quantite\":2}"
```

2. Cockpit → *Processes* → `Processus de livraison` : le jeton est arrêté sur **Préparer la commande**.
   Onglet *Variables* : `commandeId`, `disponible`, `produit`, `quantite`.
3. Tasklist, connecté en `employeLogistique` → compléter la tâche.
4. Les logs affichent l'email simulé, puis l'instance se termine.
   Cockpit → *History* pour revoir le chemin parcouru.

Stock de démo : `clavier` 25, `souris` 40, `ecran` 3, `casque` 0.
Commander `casque` met la commande en `STOCK_INSUFFISANT` — le processus continue quand même,
car le modèle n'a pas encore de passerelle. C'est un bon premier exercice à ajouter.

## Base de données

H2 en fichier (`livraison-process/data/camunda-db.mv.db`) : l'historique survit aux redémarrages.
Pour repartir de zéro, supprime le dossier `data/`.

## Tests

```powershell
mvn -pl livraison-process test
```

`ProcessusLivraisonTest` démarre une instance, vérifie l'arrêt sur la user task, la complète et
contrôle que le processus se termine. Le job executor est désactivé en test (exécution synchrone).

## Pistes d'exercices

1. Passerelle exclusive après `Task_1` sur `${disponible}` → branche « rupture de stock ».
2. Remplacer `camunda:assignee` par `camunda:candidateGroups="logistique"` (le groupe existe déjà).
3. Passer `Task_3` en `camunda:asyncBefore="true"` et observer le job dans Cockpit.
4. Lever une exception dans un delegate pour voir apparaître un **incident**.
5. Ajouter un *timer boundary event* sur `Task_2` (escalade après 1 heure).
