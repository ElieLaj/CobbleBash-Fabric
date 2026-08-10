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

## Les modèles manquants

Le bloc `training_simulator` et les 18 disques n'avaient **ni modèle ni texture** en
amont : vérifié sur les quatre commits de l'historique, seul `lang/en_us.json` a jamais
existé. Le jar publié en contient, mais pour un bloc `champion_beacon` absent du code —
le jar et le dépôt ont divergé.

On reprend donc la méthode que l'auteur applique à ce `champion_beacon` : hériter d'un
modèle vanilla plutôt que peindre une texture.

```json
{ "parent": "minecraft:block/beacon", "render_type": "minecraft:translucent" }
```

- Le **simulateur** hérite du `jukebox`. Ce n'est pas qu'un pis-aller : on clique dessus
  avec un disque en main pour entrer dans l'arène, exactement le geste du juke-box.
- Chaque **disque d'entraînement** hérite d'un disque de musique différent, choisi pour
  que la couleur de la pochette évoque le type — Feu sur `chirp` (rouge orangé), Eau sur
  `creator_music_box` (bleu), Ténèbres sur `11` (noir rayé). Vanilla en compte dix-neuf,
  il en fallait dix-huit.

**Zéro fichier de texture ajouté**, donc rien à maintenir : 21 modèles JSON de deux
lignes. Si Norevex fournit un jour de vraies textures, il suffit de remplacer les
parents. Les 21 modèles sont produits par [`tools/gen_modeles.py`](tools/gen_modeles.py) — la table type ↔ disque y est en clair.

## État

Le portage **tourne en jeu**. Reste à éprouver dans la durée : le cycle de vie des
instances sur un serveur fréquenté, et la tenue des combats RCTAPI quand plusieurs
arènes sont ouvertes en même temps.

Un piège rencontré au premier lancement, qui vaut d'être noté : la contrainte
`"rctapi": ">=0.15.2"` refusait le seul jar existant, `0.15.2-beta`. En semver une
pré-version se classe **avant** la version — il faut écrire `>=0.15.2-beta`. Cobblemon
et CobbleBadges n'étaient pas concernés : leur suffixe commence par `+`, ce sont des
métadonnées de build, ignorées à la comparaison.
