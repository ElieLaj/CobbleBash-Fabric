package com.nore.cobblebash.mixin;

import com.nore.cobblebash.event.GymEventHandler;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Remplace {@code LivingEntityUseItemEvent.Start} et {@code .Tick}.
 *
 * <p>Fabric expose l'usage d'un item par un clic ({@code UseItemCallback}),
 * mais pas la phase de maintien : sans ces deux crochets, un item de la liste
 * noire deja commence continuerait a se consommer dans l'arene.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityUseItemMixin {
    @Inject(method = "startUsingItem", at = @At("HEAD"), cancellable = true)
    private void cobblebash$blockStart(InteractionHand hand, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (GymEventHandler.blockItemUse(self, self.getItemInHand(hand))) {
            ci.cancel();
        }
    }

    @Inject(method = "updateUsingItem", at = @At("HEAD"), cancellable = true)
    private void cobblebash$blockTick(ItemStack stack, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (GymEventHandler.blockItemUse(self, stack)) {
            self.stopUsingItem();
            ci.cancel();
        }
    }
}
