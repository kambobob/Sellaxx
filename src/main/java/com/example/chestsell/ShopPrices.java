package com.example.chestsell;

import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Wraps EconomyShopGUI's sell prices (EconomyShopGUI 7.3.2).
 * Returns the total sell value for the whole stack, or 0 if the item can't be sold.
 */
public final class ShopPrices {

    public double value(Player player, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return 0;

        Double price = EconomyShopGUIHook.getItemSellPrice(player, stack);
        return price == null ? 0 : price;
    }
}
