package com.nore.cobblebash.beacon;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ChampionBeaconScreen extends AbstractContainerScreen<ChampionBeaconMenu> {
   private static final ResourceLocation UI = texture("champion_beacon_ui");
   private static final ResourceLocation BUTTON_UNLOCKED = texture("button_unlocked");
   private static final ResourceLocation BUTTON_LOCKED = texture("button_locked");
   private static final ResourceLocation BUTTON_HOVERED = texture("button_hovered");
   private static final ResourceLocation BUTTON_SELECTED = texture("button_selected");
   private static final ResourceLocation BUTTON_CHECK_ACTIVE = texture("button_check_active");
   private static final ResourceLocation BUTTON_CLOSE_HOVER = texture("button_close_hover");
   private static final ResourceLocation CHECK = texture("check");
   private static final ResourceLocation CLOSE = texture("close");
   private static final ResourceLocation INFO_BUTTON_TEXTURE = texture("info_button");
   private static final ResourceLocation INFO_WINDOW_TEXTURE = texture("info_window");
   private static final ResourceLocation INFO_WINDOW_CLOSE_BUTTON_TEXTURE = texture("info_window_close_button");
   private static final ResourceLocation INFO_SCROLL_WHEEL_TEXTURE = texture("scroll_wheel");
   private static final Component PRIMARY_EFFECT_LABEL = Component.translatable("block.minecraft.beacon.primary");
   private static final Component SECONDARY_EFFECT_LABEL = Component.translatable("block.minecraft.beacon.secondary");
   private static final Component DEFAULT_INFO_TITLE = Component.literal("Champion Beacon");
   private static final Component DEFAULT_INFO_TEXT = Component.literal(
      "Champion Beacons can have one active primary power and one active secondary power. The pyramid is built from evolution stone blocks, and fueled by evolution stones."
   );
   private static final String DEFAULT_INFO_ICON = "shiny_stone";
   private static final int BUTTON_SIZE = 22;
   private static final int INFO_BUTTON_WIDTH = 11;
   private static final int INFO_BUTTON_HEIGHT = 37;
   private static final int INFO_WINDOW_WIDTH = 132;
   private static final int INFO_WINDOW_HEIGHT = 132;
   private static final int INFO_CLOSE_BUTTON_WIDTH = 12;
   private static final int INFO_CLOSE_BUTTON_HEIGHT = 37;
   private static final int INFO_SCROLL_THUMB_WIDTH = 10;
   private static final int INFO_SCROLL_THUMB_HEIGHT = 13;
   private static final ChampionBeaconScreen.ButtonArea CLOSE_BUTTON = new ChampionBeaconScreen.ButtonArea(16, 130);
   private static final ChampionBeaconScreen.ButtonArea CONFIRM_BUTTON = new ChampionBeaconScreen.ButtonArea(42, 130);
   private static final ChampionBeaconScreen.PowerButton[] PRIMARY_BUTTONS = new ChampionBeaconScreen.PowerButton[]{
      new ChampionBeaconScreen.PowerButton(ChampionBeaconPower.REPEL, 51, 21, true, "terrain_extender"),
      new ChampionBeaconScreen.PowerButton(ChampionBeaconPower.LURE, 77, 21, true, "magnet"),
      new ChampionBeaconScreen.PowerButton(ChampionBeaconPower.APRICORN, 51, 46, true, "red_apricorn"),
      new ChampionBeaconScreen.PowerButton(ChampionBeaconPower.BERRY, 77, 46, true, "oran_berry"),
      new ChampionBeaconScreen.PowerButton(ChampionBeaconPower.DAYCARE, 64, 71, true, "exp_share"),
      new ChampionBeaconScreen.PowerButton(ChampionBeaconPower.EV, 64, 96, true, "hp_up")
   };
   private static final ChampionBeaconScreen.PowerButton[] SECONDARY_BUTTONS = new ChampionBeaconScreen.PowerButton[]{
      new ChampionBeaconScreen.PowerButton(ChampionBeaconPower.SHINY, 170, 46, false, "max_revive"),
      new ChampionBeaconScreen.PowerButton(ChampionBeaconPower.REPEL, 143, 71, false, "terrain_extender"),
      new ChampionBeaconScreen.PowerButton(ChampionBeaconPower.LURE, 170, 71, false, "magnet")
   };
   private static final ChampionBeaconScreen.ButtonArea UPGRADE_BUTTON = new ChampionBeaconScreen.ButtonArea(143, 46);
   private static final ChampionBeaconScreen.ButtonArea INFO_BUTTON = new ChampionBeaconScreen.ButtonArea(230, 15, 11, 37);
   private static final ChampionBeaconScreen.ButtonArea INFO_WINDOW = new ChampionBeaconScreen.ButtonArea(230, 15, 132, 132);
   private static final ChampionBeaconScreen.ButtonArea INFO_CLOSE_BUTTON = new ChampionBeaconScreen.ButtonArea(361, 15, 12, 37);
   private static final ChampionBeaconScreen.ButtonArea INFO_ICON_BOX = new ChampionBeaconScreen.ButtonArea(235, 21, 16, 16);
   private static final ChampionBeaconScreen.ButtonArea INFO_TITLE_BOX = new ChampionBeaconScreen.ButtonArea(255, 29, 101, 11);
   private static final ChampionBeaconScreen.ButtonArea INFO_TEXT_BOX = new ChampionBeaconScreen.ButtonArea(239, 45, 94, 92);
   private static final ChampionBeaconScreen.ButtonArea INFO_SCROLL_TRACK = new ChampionBeaconScreen.ButtonArea(340, 59, 10, 70);
   private static final ChampionBeaconScreen.ItemIcon[] PAYMENT_ICONS = new ChampionBeaconScreen.ItemIcon[]{
      new ChampionBeaconScreen.ItemIcon("ice_stone", 120, 108),
      new ChampionBeaconScreen.ItemIcon("fire_stone", 142, 108),
      new ChampionBeaconScreen.ItemIcon("moon_stone", 164, 108),
      new ChampionBeaconScreen.ItemIcon("leaf_stone", 186, 108),
      new ChampionBeaconScreen.ItemIcon("dawn_stone", 208, 108),
      new ChampionBeaconScreen.ItemIcon("dusk_stone", 120, 134),
      new ChampionBeaconScreen.ItemIcon("thunder_stone", 142, 134),
      new ChampionBeaconScreen.ItemIcon("water_stone", 164, 134),
      new ChampionBeaconScreen.ItemIcon("sun_stone", 186, 134),
      new ChampionBeaconScreen.ItemIcon("shiny_stone", 208, 134)
   };
   private boolean infoOpen;
   private Component infoTitle = DEFAULT_INFO_TITLE;
   private Component infoText = DEFAULT_INFO_TEXT;
   private String infoIcon = "shiny_stone";
   private int infoScrollLine;
   private boolean draggingInfoScroll;

   public ChampionBeaconScreen(ChampionBeaconMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.imageWidth = 256;
      this.imageHeight = 256;
      this.inventoryLabelY = 10000;
      this.titleLabelY = 10000;
   }

   private static ResourceLocation texture(String name) {
      return ResourceLocation.fromNamespaceAndPath("cobblebash", "textures/gui/champion_beacon/" + name + ".png");
   }

   protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      guiGraphics.blit(UI, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

      for (ChampionBeaconScreen.PowerButton championbeaconscreen$powerbutton : PRIMARY_BUTTONS) {
         this.drawPowerButton(guiGraphics, championbeaconscreen$powerbutton, mouseX, mouseY);
      }

      this.drawUpgradeButton(guiGraphics, mouseX, mouseY);

      for (ChampionBeaconScreen.PowerButton championbeaconscreen$powerbutton1 : SECONDARY_BUTTONS) {
         this.drawPowerButton(guiGraphics, championbeaconscreen$powerbutton1, mouseX, mouseY);
      }

      this.drawControlButtons(guiGraphics, mouseX, mouseY);
      this.drawPaymentIcons(guiGraphics);
      this.drawInfo(guiGraphics, mouseX, mouseY);
   }

   protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      guiGraphics.drawCenteredString(this.font, PRIMARY_EFFECT_LABEL, 62, 10, 14737632);
      guiGraphics.drawCenteredString(this.font, SECONDARY_EFFECT_LABEL, 169, 10, 14737632);
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (CLOSE_BUTTON.contains(this.leftPos, this.topPos, mouseX, mouseY)) {
            this.onClose();
            return true;
         }

         if (CONFIRM_BUTTON.contains(this.leftPos, this.topPos, mouseX, mouseY)) {
            this.minecraft.gameMode.handleInventoryButtonClick(((ChampionBeaconMenu)this.menu).containerId, 60);
            return true;
         }

         if (this.infoOpen && INFO_CLOSE_BUTTON.contains(this.leftPos, this.topPos, mouseX, mouseY)) {
            this.infoOpen = false;
            this.resetInfoText();
            return true;
         }

         if (this.infoOpen && INFO_SCROLL_TRACK.contains(this.leftPos, this.topPos, mouseX, mouseY) && this.getMaxInfoScroll() > 0) {
            this.draggingInfoScroll = true;
            this.updateInfoScrollFromMouse(mouseY);
            return true;
         }

         if (INFO_BUTTON.contains(this.leftPos, this.topPos, mouseX, mouseY)) {
            this.infoOpen = true;
            return true;
         }

         if (UPGRADE_BUTTON.contains(this.leftPos, this.topPos, mouseX, mouseY)) {
            this.minecraft.gameMode.handleInventoryButtonClick(((ChampionBeaconMenu)this.menu).containerId, 50);
            return true;
         }

         for (ChampionBeaconScreen.PowerButton championbeaconscreen$powerbutton : PRIMARY_BUTTONS) {
            if (championbeaconscreen$powerbutton.area().contains(this.leftPos, this.topPos, mouseX, mouseY)) {
               this.minecraft
                  .gameMode
                  .handleInventoryButtonClick(((ChampionBeaconMenu)this.menu).containerId, 10 + championbeaconscreen$powerbutton.power.id());
               return true;
            }
         }

         for (ChampionBeaconScreen.PowerButton championbeaconscreen$powerbutton1 : SECONDARY_BUTTONS) {
            if (championbeaconscreen$powerbutton1.area().contains(this.leftPos, this.topPos, mouseX, mouseY)) {
               this.minecraft
                  .gameMode
                  .handleInventoryButtonClick(((ChampionBeaconMenu)this.menu).containerId, 30 + championbeaconscreen$powerbutton1.power.id());
               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0 && this.draggingInfoScroll) {
         this.draggingInfoScroll = false;
         return true;
      } else {
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (button == 0 && this.draggingInfoScroll) {
         this.updateInfoScrollFromMouse(mouseY);
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.infoOpen && INFO_WINDOW.contains(this.leftPos, this.topPos, mouseX, mouseY) && this.getMaxInfoScroll() > 0) {
         this.infoScrollLine = clamp(this.infoScrollLine - (int)Math.signum(scrollY), 0, this.getMaxInfoScroll());
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   private void drawPowerButton(GuiGraphics guiGraphics, ChampionBeaconScreen.PowerButton button, int mouseX, int mouseY) {
      boolean flag = button.primary
         ? ((ChampionBeaconMenu)this.menu).canSelectPrimary(button.power)
         : ((ChampionBeaconMenu)this.menu).canSelectSecondary(button.power);
      boolean flag1 = button.primary
         ? ((ChampionBeaconMenu)this.menu).getPrimaryPower() == button.power
         : ((ChampionBeaconMenu)this.menu).getSecondaryPower() == button.power;
      this.drawButton(guiGraphics, button.area(), flag, flag1, mouseX, mouseY);
      this.drawCobblemonItem(guiGraphics, button.iconItem, button.x + 3, button.y + 3);
   }

   private void drawUpgradeButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      this.drawButton(
         guiGraphics, UPGRADE_BUTTON, ((ChampionBeaconMenu)this.menu).canUpgradePrimary(), ((ChampionBeaconMenu)this.menu).isUpgraded(), mouseX, mouseY
      );
      this.drawCobblemonItem(guiGraphics, "relic_coin", UPGRADE_BUTTON.x + 3, UPGRADE_BUTTON.y + 3);
   }

   private void drawControlButtons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      boolean flag = CLOSE_BUTTON.contains(this.leftPos, this.topPos, mouseX, mouseY);
      this.blitButton(guiGraphics, flag ? BUTTON_CLOSE_HOVER : BUTTON_UNLOCKED, CLOSE_BUTTON);
      this.blitButton(guiGraphics, CLOSE, CLOSE_BUTTON);
      boolean flag1 = CONFIRM_BUTTON.contains(this.leftPos, this.topPos, mouseX, mouseY);
      boolean flag2 = ((ChampionBeaconMenu)this.menu).hasPayment() && ((ChampionBeaconMenu)this.menu).getPrimaryPower() != ChampionBeaconPower.NONE;
      ResourceLocation resourcelocation = flag2 && flag1 ? BUTTON_CHECK_ACTIVE : (flag2 ? BUTTON_UNLOCKED : BUTTON_LOCKED);
      this.blitButton(guiGraphics, resourcelocation, CONFIRM_BUTTON);
      this.blitButton(guiGraphics, CHECK, CONFIRM_BUTTON);
   }

   private void drawInfo(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      if (!this.infoOpen) {
         guiGraphics.blit(INFO_BUTTON_TEXTURE, this.leftPos + INFO_BUTTON.x, this.topPos + INFO_BUTTON.y, 0.0F, 0.0F, 11, 37, 11, 37);
      } else {
         this.updateInfoText(mouseX, mouseY);
         guiGraphics.blit(INFO_WINDOW_TEXTURE, this.leftPos + INFO_WINDOW.x, this.topPos + INFO_WINDOW.y, 0.0F, 0.0F, 132, 132, 132, 132);
         guiGraphics.blit(INFO_WINDOW_CLOSE_BUTTON_TEXTURE, this.leftPos + INFO_CLOSE_BUTTON.x, this.topPos + INFO_CLOSE_BUTTON.y, 0.0F, 0.0F, 12, 37, 12, 37);
         this.drawCobblemonItem(guiGraphics, this.infoIcon, INFO_ICON_BOX.x, INFO_ICON_BOX.y);
         int i = this.leftPos + INFO_TITLE_BOX.x;
         int j = this.topPos + INFO_TITLE_BOX.y;
         int k = this.leftPos + INFO_TEXT_BOX.x;
         int l = this.topPos + INFO_TEXT_BOX.y;
         guiGraphics.enableScissor(i, j, i + INFO_TITLE_BOX.width, j + INFO_TITLE_BOX.height);
         guiGraphics.drawString(this.font, this.infoTitle, i, j, 16777215, true);
         guiGraphics.disableScissor();
         guiGraphics.enableScissor(k, l, k + INFO_TEXT_BOX.width, l + INFO_TEXT_BOX.height);
         this.drawWordWrapWithShadow(guiGraphics, this.infoText, k, l, INFO_TEXT_BOX.width, INFO_TEXT_BOX.height, this.infoScrollLine);
         guiGraphics.disableScissor();
         this.drawInfoScrollThumb(guiGraphics);
      }
   }

   private void updateInfoText(int mouseX, int mouseY) {
      for (ChampionBeaconScreen.PowerButton championbeaconscreen$powerbutton : PRIMARY_BUTTONS) {
         if (championbeaconscreen$powerbutton.area().contains(this.leftPos, this.topPos, mouseX, mouseY)) {
            this.setInfoText(
               this.describePowerTitle(championbeaconscreen$powerbutton.power),
               this.describePower(championbeaconscreen$powerbutton.power, true),
               championbeaconscreen$powerbutton.iconItem
            );
            return;
         }
      }

      for (ChampionBeaconScreen.PowerButton championbeaconscreen$powerbutton1 : SECONDARY_BUTTONS) {
         if (championbeaconscreen$powerbutton1.area().contains(this.leftPos, this.topPos, mouseX, mouseY)) {
            this.setInfoText(
               this.describePowerTitle(championbeaconscreen$powerbutton1.power),
               this.describePower(championbeaconscreen$powerbutton1.power, false),
               championbeaconscreen$powerbutton1.iconItem
            );
            return;
         }
      }

      if (UPGRADE_BUTTON.contains(this.leftPos, this.topPos, mouseX, mouseY)) {
         this.setInfoText(
            Component.literal("Upgrade"),
            Component.literal("Upgrade boosts the selected primary aura. Only Apricorn, Berry, Daycare and EV auras can be upgraded."),
            "relic_coin"
         );
      }
   }

   private void resetInfoText() {
      this.setInfoText(DEFAULT_INFO_TITLE, DEFAULT_INFO_TEXT, "shiny_stone");
   }

   private void setInfoText(Component title, Component text, String icon) {
      if (!this.infoTitle.equals(title) || !this.infoText.equals(text) || !this.infoIcon.equals(icon)) {
         this.infoTitle = title;
         this.infoText = text;
         this.infoIcon = icon;
         this.infoScrollLine = 0;
         this.draggingInfoScroll = false;
      }
   }

   private void drawWordWrapWithShadow(GuiGraphics guiGraphics, Component text, int x, int y, int width, int height, int scrollLine) {
      List<FormattedCharSequence> list = this.splitLines(text, width);
      int i = Math.max(1, height / 9);
      int j = clamp(scrollLine, 0, Math.max(0, list.size() - i));
      int k = y;

      for (int l = j; l < list.size() && k + 9 <= y + height; l++) {
         guiGraphics.drawString(this.font, list.get(l), x, k, 16777215, true);
         k += 9;
      }
   }

   private List<FormattedCharSequence> splitLines(Component text, int width) {
      List<FormattedCharSequence> list = new ArrayList<>();
      String[] astring = text.getString().split("\\n", -1);

      for (String s : astring) {
         if (s.isEmpty()) {
            list.add(FormattedCharSequence.EMPTY);
         } else {
            list.addAll(this.font.split(Component.literal(s), width));
         }
      }

      return list;
   }

   private void drawInfoScrollThumb(GuiGraphics guiGraphics) {
      int i = this.getMaxInfoScroll();
      int j = this.leftPos + INFO_SCROLL_TRACK.x;
      int k = this.topPos + this.getScrollThumbY(i);
      guiGraphics.blit(INFO_SCROLL_WHEEL_TEXTURE, j, k, 0.0F, 0.0F, 10, 13, 10, 13);
   }

   private int getMaxInfoScroll() {
      int i = Math.max(1, INFO_TEXT_BOX.height / 9);
      return Math.max(0, this.splitLines(this.infoText, INFO_TEXT_BOX.width).size() - i);
   }

   private int getScrollThumbY(int maxScroll) {
      int i = INFO_SCROLL_TRACK.height - 13;
      return maxScroll > 0 && i > 0 ? INFO_SCROLL_TRACK.y + Math.round(i * ((float)this.infoScrollLine / maxScroll)) : INFO_SCROLL_TRACK.y;
   }

   private void updateInfoScrollFromMouse(double mouseY) {
      int i = this.getMaxInfoScroll();
      int j = INFO_SCROLL_TRACK.height - 13;
      if (i > 0 && j > 0) {
         double d0 = mouseY - (this.topPos + INFO_SCROLL_TRACK.y) - 6.5;
         this.infoScrollLine = clamp(Math.round((float)(d0 / j * i)), 0, i);
      } else {
         this.infoScrollLine = 0;
      }
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   private Component describePowerTitle(ChampionBeaconPower power) {
      return (Component)(switch (power) {
         case REPEL -> Component.literal("Repel Aura");
         case LURE -> Component.literal("Lure Aura");
         case APRICORN -> Component.literal("Apricorn Aura");
         case BERRY -> Component.literal("Berry Aura");
         case DAYCARE -> Component.literal("Daycare Aura");
         case EV -> Component.literal("EV Aura");
         case SHINY -> Component.literal("Shiny Aura");
         case NONE -> DEFAULT_INFO_TITLE;
      });
   }

   private Component describePower(ChampionBeaconPower power, boolean primary) {
      return (Component)(switch (power) {
         case REPEL -> Component.literal(
            primary
               ? "Repel Aura prevents wild Pokemon from spawning inside the beacon radius."
               : "Secondary Repel gives spawn protection while your primary aura handles another job."
         );
         case LURE -> Component.literal(
            primary
               ? "Lure Aura increases wild Pokemon spawn activity inside the beacon radius."
               : "Secondary Lure adds extra spawn activity while your primary aura stays active."
         );
         case APRICORN -> Component.literal(
            "Apricorn Aura speeds up apricorn growth inside the beacon radius. The upgrade option makes the growth boost stronger."
         );
         case BERRY -> Component.literal(
            "Berry Aura speeds up Cobblemon berry plant growth inside the beacon radius. The upgrade option makes the growth boost stronger."
         );
         case DAYCARE -> Component.literal(
            "Daycare Aura grants a passive 60 XP evenly dispersed to Pokemon in pasture blocks inside the beacon radius. Upgraded version gives 120 XP."
         );
         case EV -> Component.literal(
            "EV Aura slowly grants selected EVs to Pokemon in pasture blocks withing beacon radius.\nEV type can be chosed by stone type used to ignite beacon.\nFire Stone / Shiny Stone: Attack\nWater Stone / Leaf Stone: HP\nThunder Stone: Speed\nDawn Stone / Sun Stone: Special Attack\nIce Stone / Dusk Stone: Defense\nMoon Stone: Special Defense"
         );
         case SHINY -> Component.literal("Shiny Aura adds a small extra shiny chance for wild Pokemon that spawn inside the beacon radius.");
         case NONE -> DEFAULT_INFO_TEXT;
      });
   }

   private void drawButton(GuiGraphics guiGraphics, ChampionBeaconScreen.ButtonArea area, boolean enabled, boolean selected, int mouseX, int mouseY) {
      ResourceLocation resourcelocation = BUTTON_LOCKED;
      if (enabled) {
         resourcelocation = selected ? BUTTON_SELECTED : (area.contains(this.leftPos, this.topPos, mouseX, mouseY) ? BUTTON_HOVERED : BUTTON_UNLOCKED);
      }

      this.blitButton(guiGraphics, resourcelocation, area);
   }

   private void blitButton(GuiGraphics guiGraphics, ResourceLocation texture, ChampionBeaconScreen.ButtonArea area) {
      guiGraphics.blit(texture, this.leftPos + area.x, this.topPos + area.y, 0.0F, 0.0F, 22, 22, 22, 22);
   }

   private void drawPaymentIcons(GuiGraphics guiGraphics) {
      for (ChampionBeaconScreen.ItemIcon championbeaconscreen$itemicon : PAYMENT_ICONS) {
         this.drawCobblemonItem(guiGraphics, championbeaconscreen$itemicon.itemName, championbeaconscreen$itemicon.x, championbeaconscreen$itemicon.y);
      }
   }

   private void drawCobblemonItem(GuiGraphics guiGraphics, String itemName, int x, int y) {
      ItemStack itemstack = ((Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("cobblemon", itemName))).getDefaultInstance();
      guiGraphics.renderItem(itemstack, this.leftPos + x, this.topPos + y);
   }

   private record ButtonArea(int x, int y, int width, int height) {
      ButtonArea(int x, int y) {
         this(x, y, 22, 22);
      }

      boolean contains(int left, int top, double mouseX, double mouseY) {
         return mouseX >= left + this.x && mouseX < left + this.x + this.width && mouseY >= top + this.y && mouseY < top + this.y + this.height;
      }
   }

   private record ItemIcon(String itemName, int x, int y) {
   }

   private record PowerButton(ChampionBeaconPower power, int x, int y, boolean primary, String iconItem) {
      ChampionBeaconScreen.ButtonArea area() {
         return new ChampionBeaconScreen.ButtonArea(this.x, this.y);
      }
   }
}
