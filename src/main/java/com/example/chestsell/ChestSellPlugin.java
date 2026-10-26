package com.example.chestsell;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ChestSellPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private Economy economy;
    private ShopPrices prices;
    private NamespacedKey sellAxeKey;

    @Override
    public void onEnable() {
        var rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("No Vault economy provider found (is LiteCo loaded?)");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        economy = rsp.getProvider();
        prices = new ShopPrices();
        sellAxeKey = new NamespacedKey(this, "sell_axe");
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("sellaxe").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("chestsell.axe")) {
            player.sendMessage("§cYou don't have permission to do that.");
            return true;
        }

        ItemStack axe = createSellAxe();
        var leftover = player.getInventory().addItem(axe);
        leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        player.sendMessage("§aYou received the Sell Axe. Right-click a chest with it to sell the chest.");
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!isSellAxe(player.getInventory().getItemInMainHand())) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        Material type = block.getType();
        if (type != Material.CHEST && type != Material.TRAPPED_CHEST) return;
        if (!(block.getState() instanceof Container container)) return;
        if (!player.hasPermission("chestsell.use")) return;

        event.setCancelled(true); // don't open the chest, just sell

        Inventory inv = container.getInventory();
        double total = sellItems(player, inv);

        if (total > 0) {
            economy.depositPlayer(player, total);
            player.sendMessage("§aSold chest for §e" + economy.format(total) + "§a.");
        } else {
            player.sendMessage("§7Nothing sellable in this chest.");
        }
    }

    /**
     * Sells sellable items from the inventory and returns the total earned.
     * If any shulker box has items in it, only the shulker contents (and loose items)
     * are sold and the shulkers stay. Otherwise the empty shulker boxes themselves are sold.
     */
    private double sellItems(Player player, Inventory inv) {
        boolean hasFilledShulker = false;
        for (int i = 0; i < inv.getSize(); i++) {
            ShulkerBox sb = shulkerState(inv.getItem(i));
            if (sb != null && !sb.getInventory().isEmpty()) {
                hasFilledShulker = true;
                break;
            }
        }

        double total = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;

            if (shulkerState(item) != null) {
                if (hasFilledShulker) {
                    double earned = sellShulkerContents(player, item);
                    if (earned > 0) {
                        total += earned;
                        inv.setItem(i, item); // getItem returns a copy, so write it back
                    }
                } else {
                    double value = prices.value(player, item);
                    if (value > 0) {
                        total += value;
                        inv.setItem(i, null);
                    }
                }
            } else {
                double value = prices.value(player, item);
                if (value > 0) {
                    total += value;
                    inv.setItem(i, null);
                }
            }
        }
        return total;
    }

    /** Sells the sellable contents of a shulker item in place. Caller must write the item back. */
    private double sellShulkerContents(Player player, ItemStack shulkerItem) {
        if (!(shulkerItem.getItemMeta() instanceof BlockStateMeta meta)) return 0;
        if (!(meta.getBlockState() instanceof ShulkerBox shulker)) return 0;

        Inventory shulkerInv = shulker.getInventory();
        double total = 0;
        for (int j = 0; j < shulkerInv.getSize(); j++) {
            ItemStack inner = shulkerInv.getItem(j);
            if (inner == null || inner.getType().isAir()) continue;
            double value = prices.value(player, inner);
            if (value > 0) {
                total += value;
                shulkerInv.setItem(j, null);
            }
        }

        meta.setBlockState(shulker);
        shulkerItem.setItemMeta(meta);
        return total;
    }

    /** Returns the shulker box state if the item is a shulker box, otherwise null. */
    private ShulkerBox shulkerState(ItemStack item) {
        if (item == null || !item.getType().name().endsWith("SHULKER_BOX")) return null;
        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return null;
        return meta.getBlockState() instanceof ShulkerBox sb ? sb : null;
    }

    private ItemStack createSellAxe() {
        ItemStack axe = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = axe.getItemMeta();
        meta.setDisplayName("§6Sell Axe");
        meta.setLore(List.of("§7Right-click a chest to sell it"));
        meta.getPersistentDataContainer().set(sellAxeKey, PersistentDataType.BYTE, (byte) 1);
        axe.setItemMeta(meta);
        return axe;
    }

    private boolean isSellAxe(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(sellAxeKey, PersistentDataType.BYTE);
    }
}
