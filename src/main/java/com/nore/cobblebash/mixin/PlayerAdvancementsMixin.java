package com.nore.cobblebash.mixin;

import com.nore.cobblebash.event.GymEventHandler;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Remplace {@code AdvancementEvent.AdvancementEarnEvent}.
 *
 * <p>C'est ce crochet qui accorde le badge CobbleBadges quand l'avancement
 * {@code cobblebash:gym/<type>} tombe. {@code award} ne rend {@code true} que
 * si la progression a change, donc le badge n'est accorde qu'une fois.
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At("RETURN"))
    private void cobblebash$onAdvancementEarned(AdvancementHolder advancement, String criterion,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || this.player == null) {
            return;
        }

        PlayerAdvancements self = (PlayerAdvancements) (Object) this;
        if (self.getOrStartProgress(advancement).isDone()) {
            GymEventHandler.onAdvancementEarned(this.player, advancement.id());
        }
    }
}
