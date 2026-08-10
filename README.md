# CobbleBash — portage Fabric

Portage sur Fabric de [CobbleBash](https://github.com/NorevexG/CobbleBash) de **Norevex**,
mod NeoForge pour Minecraft 1.21.1 qui ajoute dix-huit arènes instanciées par type :
deux dresseurs et un boss dans une dimension privée, avec progression et badges.

**Sous licence LGPL-3.0-only, comme l'original.** Le copyright reste à Norevex (2026).
Toute redistribution du jar doit s'accompagner de ces sources — c'est la condition de la
licence, pas une politesse.

## Ce qui a changé, et pourquoi

Sur les 28 fichiers Java de l'original, **21 n'avaient aucune dépendance à NeoForge** et
sont repris tels quels : les commandes, le placement des structures, la dimension, la
gestion des instances, la progression, toute l'intégration Cobblemon et RCTAPI. Seuls
7 fichiers ont été réécrits, plus 3 mixins ajoutés.

### Les événements

Neuf des douze crochets ont un équivalent direct dans l'API Fabric :

| NeoForge | Fabric |
|---|---|
| `PlayerEvent.PlayerLoggedIn/OutEvent` | `ServerPlayConnectionEvents.JOIN` / `.DISCONNECT` |
| `PlayerInteractEvent.EntityInteract` | `UseEntityCallback` |
| `PlayerInteractEvent.RightClickItem` | `UseItemCallback` |
| `PlayerInteractEvent.RightClickBlock` | `UseBlockCallback` |
| `PlayerTickEvent.Post` | `ServerTickEvents.END_SERVER_TICK`, en balayant les joueurs |
| `LivingIncomingDamageEvent` + `LivingDamageEvent.Pre` | `ServerLivingEntityEvents.ALLOW_DAMAGE` |

Les deux crochets de dégâts fusionnent : tous deux ne faisaient qu'annuler des dégâts,
ce que `ALLOW_DAMAGE` fait en rendant `false`.

Les trois derniers n'existent pas côté Fabric et passent par un mixin :

| NeoForge | Mixin |
|---|---|
| `LivingEntityUseItemEvent.Start` / `.Tick` | `LivingEntity#startUsingItem` / `#updateUsingItem` |
| `EntityJoinLevelEvent` (annulable) | `ServerLevel#addFreshEntity` |
| `AdvancementEvent.AdvancementEarnEvent` | `PlayerAdvancements#award` |

> `ServerEntityEvents.ENTITY_LOAD` existe bien chez Fabric, mais il est purement
> informatif : impossible de refuser l'arrivée d'une entité. Or c'est tout l'objet du
> crochet d'origine.

> Le mixin de l'arrivée d'entité vise `ServerLevel`, pas `Level` : `Level` hérite
> `addFreshEntity` de `LevelWriter` sans le déclarer, si bien que le processeur
> d'annotations n'écrit aucune entrée de refmap et que l'injection échoue au chargement.

### Le reste

- **Registres** — `DeferredRegister` n'a pas d'équivalent : Fabric appelle
  `onInitialize()` pendant que les registres sont modifiables, donc enregistrement direct.
- **Configuration** — `ModConfigSpec` est propre à NeoForge. La liste noire d'items se lit
  dans `config/cobblebash.json`, créé au premier lancement.
- **Écran de configuration** — `IConfigScreenFactory` n'existe pas sur Fabric (ModMenu le
  fournit, dépendance qu'on n'impose pas). Le fichier se modifie à la main.
- **`withTabsBefore`** est une extension NeoForge du constructeur d'onglet créatif ; en
  vanilla, l'ordre suit celui de l'enregistrement.
- **`SavedData.Factory`** prend trois arguments en vanilla — NeoForge ajoute une surcharge
  à deux. Le type de DataFixer est `null` : ces données ne sont pas versionnées.

## Ajouté par rapport à l'amont

```
/gym badges [<joueurs>]
```

Accorde les **dix-huit badges de type**. Réservée aux opérateurs (niveau 2) — le reste
de l'arbre `/gym` est ouvert à tous, mais celle-ci donne dix-huit badges d'un coup.

Rien n'est forcé côté CobbleBadges : le palier de base de chaque badge s'obtient en
*possédant* l'avancement `cobblebash:gym/<type>` — un déclencheur `minecraft:tick`
conditionné à cet avancement. La commande accorde donc les avancements, et le badge suit
au tick suivant, par le même chemin qu'une arène réellement terminée. Badge, statistique
`gyms_completed` et progression restent cohérents, et les paliers Great / Ultra / Master
(100 / 200 / 300 points) restent à gagner en jeu.

La boucle ne connaît que l'énumération `GymType` : **tout autre badge, Conseil 4 compris,
est hors d'atteinte par construction**, y compris si on en ajoute plus tard.

## Construire

```bash
./gradlew build
```

JDK 21. Le jar sort dans `build/libs/`. Les dépendances de compilation sont dans `libs/`
et proviennent directement du pack, donc aucun écart de version avec ce qui tourne en jeu.

## Dépendances d'exécution

| Mod | Version |
|---|---|
| Cobblemon | 1.7.3+1.21.1 |
| RCTAPI | 0.15.2-beta |
| CobbleBadges | 4.0.0+Beta-1 |
| Fabric API | 0.116.12+1.21.1 |
| Fabric Language Kotlin | 1.13.12 |

## ⚠ Connu, et venant de l'amont

**Le bloc `training_simulator` et les 18 disques d'entraînement n'ont ni modèle ni
texture** dans les sources de l'original : ils s'afficheront en cube violet et noir.
Le jar publié `cobblebash-0.1.0.jar` contient bien des assets, mais pour un bloc
`champion_beacon` qui n'existe nulle part dans les sources — le jar et le dépôt ont
divergé. Il faut demander les assets à l'auteur, ou en poser de provisoires.

**Rien n'a été testé en jeu.** Le portage compile et les trois mixins sont correctement
remappés, mais le cycle de vie des instances, les combats RCTAPI et le placement des
structures demandent une vérification manette en main.
