# -*- coding: utf-8 -*-
"""Reprend les assets officiels du jar 0.1.2 publie sur Modrinth.

Le depot GitHub s'est arrete a la 0.1.0 et n'a jamais contenu d'assets ; la
0.1.2 en a 219, faits sous Blockbench. On prend ceux des objets que ce portage
possede reellement — le simulateur et les dix-huit disques — et rien d'autre.

Une seule adaptation : le blockstate. En 0.1.2 le simulateur est un bloc de
deux hauteurs, oriente (`facing`, `half`) ; celui du code publie sur GitHub est
un bloc simple, sans etat. On ecrit donc une variante unique, sinon aucune ne
correspondrait et le bloc resterait invisible.

Usage : python tools/extrait_assets.py <chemin du jar 0.1.2>
"""
from __future__ import unicode_literals
import io, json, os, sys, zipfile

RACINE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEST = os.path.join(RACINE, "src", "main", "resources", "assets", "cobblebash")

TYPES = ["bug", "dark", "dragon", "electric", "fairy", "fighting", "fire",
         "flying", "ghost", "grass", "ground", "ice", "normal", "poison",
         "psychic", "rock", "steel", "water"]

# Le simulateur du 0.1.2 est un bloc a deux moities orientees ; le notre est un
# bloc simple. Le modele « lower » dessine la machine entiere (ses elements
# montent jusqu'a y=24), une variante unique suffit donc a l'afficher en entier.
BLOCKSTATE = {"variants": {"": {"model": "cobblebash:block/training_simulator"}}}

A_COPIER = (
    ["assets/cobblebash/models/block/training_simulator.json",
     "assets/cobblebash/models/item/training_simulator.json",
     "assets/cobblebash/models/item/training_disc_base.json",
     "assets/cobblebash/textures/block/training_simulator.png",
     "assets/cobblebash/textures/item/training_disc_base_texture.png"]
    + ["assets/cobblebash/models/item/%s_training_disk.json" % t for t in TYPES]
    + ["assets/cobblebash/textures/item/training_disc_%s_texture.png" % t for t in TYPES]
)


def main(jar):
    with zipfile.ZipFile(jar) as z:
        presents = set(z.namelist())
        manquants = [n for n in A_COPIER if n not in presents]
        if manquants:
            print("absents du jar :")
            for n in manquants:
                print("   ", n)
            return 1

        for nom in A_COPIER:
            cible = os.path.join(DEST, nom.split("assets/cobblebash/", 1)[1].replace("/", os.sep))
            d = os.path.dirname(cible)
            if not os.path.isdir(d):
                os.makedirs(d)
            with open(cible, "wb") as f:
                f.write(z.read(nom))

    chemin = os.path.join(DEST, "blockstates", "training_simulator.json")
    io.open(chemin, "w", encoding="utf-8", newline="\n").write(
        json.dumps(BLOCKSTATE, indent=2) + "\n")

    print("%d assets officiels repris, blockstate adapte" % len(A_COPIER))
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(2)
    sys.exit(main(sys.argv[1]))
