package com.nore.cobblebash.mixin;

import com.nore.cobblebash.event.GymEventHandler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Remplace {@code EntityJoinLevelEvent}.
 *
 * <p>Fabric a bien {@code ServerEntityEvents.ENTITY_LOAD}, mais il est purement
 * informatif : impossible de refuser l'arrivee. Or c'est tout l'objet du
 * crochet d'origine — empecher un Pokemon etranger au combat d'apparaitre dans
 * l'arene instanciee.
 *
 * <p>La cible est {@code ServerLevel} et non {@code Level} : {@code Level}
 * herite {@code addFreshEntity} de l'interface {@code LevelWriter} sans le
 * declarer, si bien que le processeur d'annotations ne trouve pas la methode et
 * n'ecrit aucune entree de refmap — l'injection echouerait au chargement.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelEntityJoinMixin {
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void cobblebash$blockGymIntruders(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (GymEventHandler.blockEntityJoin((Level) (Object) this, entity)) {
            cir.setReturnValue(false);
        }
    }
}
