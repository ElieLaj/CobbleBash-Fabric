package com.nore.cobblebash.client.tooltip;

import com.nore.cobblebash.item.RibbonAttributeManager;
import java.util.List;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record RibbonTooltipComponent(List<RibbonAttributeManager.TooltipTypeBonus> rows) implements TooltipComponent {
}
