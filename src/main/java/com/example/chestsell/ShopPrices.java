package com.example.chestsell;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Wraps EconomyShopGUI's sell prices.
 * Returns the total sell value for the whole stack, or 0 if the item can't be sold.
 */
public final class ShopPrices {

    public double value(Player player, ItemStack stack) {
        // TODO: replace with real EconomyShopGUI calls for your version.
        // Look up the shop entry for this item, read its sell price per item,
        // multiply by the stack amount, and return 0 if it isn't sellable.
        //
        // Example shape (method names are NOT verified):
        //   ShopItem shop = EconomyShopGUI.getInstance().getShopManager().getShopItem(stack);
        //   if (shop == null || !shop.isSellable()) return 0;
        //   return shop.getSellPrice(player) * stack.getAmount();

        return 0; // placeholder until the API calls above are filled in
    }
}
