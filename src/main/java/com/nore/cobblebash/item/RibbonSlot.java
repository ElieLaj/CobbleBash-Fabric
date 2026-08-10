package com.nore.cobblebash.item;

import net.minecraft.world.entity.LivingEntity;

/**
 * Emplacement d'accessoire, vu par le gestionnaire de rubans.
 *
 * <p>La version NeoForge passe le {@code SlotContext} de Curios, qui n'existe
 * pas sur Fabric. Des quatre informations qu'elle en tire — porteur, nom
 * d'emplacement, indice, cosmetique — trois se retrouvent telles quelles dans
 * le {@code SlotReference} d'Accessories. On les recopie ici pour que
 * {@link RibbonAttributeManager} ne connaisse aucune API d'accessoires : seuls
 * les deux objets ruban font le pont.
 *
 * <p>Accessories n'expose pas de notion de cosmetique a cet endroit ; le champ
 * reste pour que les identifiants d'emplacement gardent la meme forme, et vaut
 * toujours {@code false}.
 */
public record RibbonSlot(LivingEntity entity, String identifier, int index, boolean cosmetic) {

    public static RibbonSlot of(LivingEntity entity, String identifier, int index) {
        return new RibbonSlot(entity, identifier, index, false);
    }
}
