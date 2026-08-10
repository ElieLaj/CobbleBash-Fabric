package com.nore.cobblebash.client.tooltip;

import com.nore.cobblebash.item.RibbonAttributeManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;

public class ClientRibbonTooltipComponent implements ClientTooltipComponent {
   private static final ResourceLocation TYPE_ICONS = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/types_small.png");
   private static final int ICON_SIZE = 18;
   private static final int ICON_SHEET_WIDTH = 324;
   private static final int ICON_SHEET_HEIGHT = 18;
   private static final int ROW_HEIGHT = 20;
   private static final int ICON_TEXT_GAP = 4;
   private static final int TEXT_COLOR = 11993014;
   private final List<ClientRibbonTooltipComponent.Row> rows;

   public ClientRibbonTooltipComponent(RibbonTooltipComponent component) {
      this.rows = component.rows().stream().map(ClientRibbonTooltipComponent.Row::from).filter(row -> !row.text.isEmpty()).toList();
   }

   public int getHeight() {
      return this.rows.isEmpty() ? 0 : this.rows.size() * 20;
   }

   public int getWidth(Font font) {
      int i = 0;

      for (ClientRibbonTooltipComponent.Row clientribbontooltipcomponent$row : this.rows) {
         i = Math.max(i, 22 + font.width(clientribbontooltipcomponent$row.text));
      }

      return i;
   }

   public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
      for (int i = 0; i < this.rows.size(); i++) {
         ClientRibbonTooltipComponent.Row clientribbontooltipcomponent$row = this.rows.get(i);
         int j = y + i * 20;
         graphics.blit(TYPE_ICONS, x, j, clientribbontooltipcomponent$row.textureIndex * 18, 0.0F, 18, 18, 324, 18);
         graphics.drawString(font, clientribbontooltipcomponent$row.text, x + 18 + 4, j + 5, 11993014, false);
      }
   }

   private record Row(int textureIndex, String text) {
      private static ClientRibbonTooltipComponent.Row from(RibbonAttributeManager.TooltipTypeBonus bonus) {
         List<String> list = new ArrayList<>();

         for (Entry<RibbonAttributeManager.AttributeKey, Double> entry : bonus.attributes().entrySet()) {
            if (!(Math.abs(entry.getValue()) < 1.0E-6)) {
               list.add(formatSigned(entry.getValue()) + " " + entry.getKey().displayName());
            }
         }

         return new ClientRibbonTooltipComponent.Row(bonus.textureIndex(), String.join(", ", list));
      }

      private static String formatSigned(double value) {
         String s = value > 0.0 ? "+" : "";
         double d0 = Math.round(value * 1000.0) / 1000.0;
         return Math.abs(d0 - Math.rint(d0)) < 1.0E-6
            ? s + String.format(Locale.ROOT, "%.0f", d0)
            : s + String.format(Locale.ROOT, "%.3f", d0).replaceAll("0+$", "").replaceAll("\\.$", "");
      }
   }
}
