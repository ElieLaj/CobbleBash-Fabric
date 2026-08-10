package com.nore.cobblebash.gymlevel;

import com.nore.cobblebash.gym.GymLevelOverride;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Reglage du niveau, avec une glissiere de 10 a 100.
 *
 * <p>Pas de fond de conteneur : l'ecran n'a aucun emplacement, seulement un
 * titre, une glissiere et deux boutons.
 */
public class GymLevelScreen extends AbstractContainerScreen<GymLevelMenu> {
   private static final int WIDTH = 200;
   private static final int HEIGHT = 96;

   private int level = 30;

   public GymLevelScreen(GymLevelMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageWidth = WIDTH;
      this.imageHeight = HEIGHT;
   }

   @Override
   protected void init() {
      super.init();
      int x = this.leftPos;
      int y = this.topPos;

      this.addRenderableWidget(new LevelSlider(x + 10, y + 30, WIDTH - 20, 20));

      this.addRenderableWidget(Button.builder(
            Component.translatable("gui.cobblebash.gym_level.start"),
            button -> {
               // Le serveur revalide la borne : le bouton n'est qu'un canal.
               this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, this.level);
            })
         .bounds(x + 10, y + 60, 90, 20).build());

      this.addRenderableWidget(Button.builder(
            Component.translatable("gui.cobblebash.gym_level.cancel"),
            button -> this.onClose())
         .bounds(x + WIDTH - 100, y + 60, 90, 20).build());
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.fill(this.leftPos, this.topPos, this.leftPos + WIDTH, this.topPos + HEIGHT, 0xC0101010);
      graphics.renderOutline(this.leftPos, this.topPos, WIDTH, HEIGHT, 0xFF5A5A5A);
   }

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(this.font,
         Component.translatable("gui.cobblebash.gym_level.title",
            Component.translatable("cobblebash.gym." + this.menu.gymType())),
         10, 10, 0xFFFFFF, false);
   }

   /** Le titre par defaut du conteneur ferait doublon avec le notre. */
   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics, mouseX, mouseY, partialTick);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderTooltip(graphics, mouseX, mouseY);
   }

   private class LevelSlider extends AbstractSliderButton {
      LevelSlider(int x, int y, int width, int height) {
         super(x, y, width, height, Component.empty(),
            (GymLevelScreen.this.level - GymLevelOverride.MIN)
               / (double) (GymLevelOverride.MAX - GymLevelOverride.MIN));
         this.updateMessage();
      }

      @Override
      protected void updateMessage() {
         this.setMessage(Component.translatable("gui.cobblebash.gym_level.slider", GymLevelScreen.this.level));
      }

      @Override
      protected void applyValue() {
         int span = GymLevelOverride.MAX - GymLevelOverride.MIN;
         GymLevelScreen.this.level = GymLevelOverride.MIN + (int) Math.round(this.value * span);
      }
   }
}
