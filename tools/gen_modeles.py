# -*- coding: utf-8 -*-
"""Modeles du simulateur et des dix-huit disques, sans un seul fichier de texture.

Les assets n'existent nulle part en amont : ni dans les sources (verifie sur les
quatre commits de l'historique), ni dans le jar publie, qui ne contient que ceux
d'un bloc `champion_beacon` absent du code.

On reprend la methode de l'auteur pour ce bloc-la : heriter d'un modele vanilla
plutot que peindre une texture.

  - le simulateur herite du JUKEBOX. Ce n'est pas qu'un pis-aller : on clique
    dessus avec un disque en main pour entrer dans l'arene, exactement le geste
    du juke-box.
  - chaque disque d'entrainement herite d'un DISQUE DE MUSIQUE different, choisi
    pour que la couleur de la pochette evoque le type. Vanilla en compte
    dix-neuf, il en faut dix-huit.

Relancer ce script apres avoir change la table ci-dessous ; les fichiers
produits sont versionnes, il n'est pas necessaire de l'executer pour construire.

    python tools/gen_modeles.py
"""
from __future__ import unicode_literals
import io, json, os

RACINE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FB = os.path.join(RACINE, "src", "main", "resources", "assets", "cobblebash")

# type -> disque vanilla, apparie a la couleur de la pochette
DISQUES = {
    "normal":   "13",                    # gris
    "fire":     "chirp",                 # rouge orange
    "water":    "creator_music_box",     # bleu
    "grass":    "cat",                   # vert
    "electric": "strad",                 # jaune orange
    "ice":      "wait",                  # cyan clair
    "fighting": "5",                     # rouge sombre
    "poison":   "mellohi",               # violet
    "ground":   "stal",                  # brun
    "flying":   "otherside",             # cyan pale
    "psychic":  "creator",               # magenta
    "bug":      "blocks",                # vert lime
    "rock":     "relic",                 # ocre
    "ghost":    "precipice",             # violet sombre
    "dragon":   "far",                   # sarcelle
    "dark":     "11",                    # noir raye
    "steel":    "ward",                  # gris vert
    "fairy":    "pigstep",               # rose
}


def ecrit(chemin, contenu):
    d = os.path.dirname(chemin)
    if not os.path.isdir(d):
        os.makedirs(d)
    io.open(chemin, "w", encoding="utf-8", newline="\n").write(
        json.dumps(contenu, indent=2, ensure_ascii=False) + "\n")


assert len(DISQUES) == 18, "il faut un disque par type"
assert len(set(DISQUES.values())) == 18, "deux types partagent le meme disque"

ecrit(os.path.join(FB, "blockstates", "training_simulator.json"),
      {"variants": {"": {"model": "cobblebash:block/training_simulator"}}})
ecrit(os.path.join(FB, "models", "block", "training_simulator.json"),
      {"parent": "minecraft:block/jukebox"})
ecrit(os.path.join(FB, "models", "item", "training_simulator.json"),
      {"parent": "cobblebash:block/training_simulator"})

for type_, disque in sorted(DISQUES.items()):
    ecrit(os.path.join(FB, "models", "item", "%s_training_disk.json" % type_),
          {"parent": "minecraft:item/music_disc_%s" % disque})

print("ecrit : 1 blockstate, 1 modele de bloc, %d modeles d'item" % (len(DISQUES) + 1))
