package dev.minecore.asteroidChests.listeners;

import dev.minecore.asteroidChests.ChestShopPlugin;
import dev.minecore.asteroidChests.models.PendingAction;
import dev.minecore.asteroidChests.models.PendingActionType;
import dev.minecore.asteroidChests.models.Shop;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class ChestClickListener implements Listener {

    private final ChestShopPlugin plugin;

    public ChestClickListener(ChestShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        Block block = event.getClickedBlock();
        if (!isChest(block.getType())) {
            return;
        }

        Shop shop = plugin.getShopManager().getShopAtBlock(block.getLocation());
        if (shop != null) {
            // Only handle RIGHT_CLICK - let LEFT_CLICK pass through to mining
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                Player player = event.getPlayer();

                // Owner with shift = open chest for restocking
                if (shop.getOwnerUuid().equals(player.getUniqueId()) && player.isSneaking()) {
                    openChestForRestocking(player, block);
                    plugin.getMessageManager().send(player, "restock.opened");
                    return;
                }

                // Normal click = show menu
                if (shop.getOwnerUuid().equals(player.getUniqueId())) {
                    plugin.getMessageManager().sendOwnerMenu(player, shop);
                } else {
                    plugin.getMessageManager().sendPlayerMenu(player, shop);
                }
            }
            // Don't interfere with LEFT_CLICK - let normal mining happen
            return;
        }
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getPlayer().isSneaking()) {
            handleCreation(event);
        }   
    }

    private void openChestForRestocking(Player player, Block block) {
        if (isChest(block.getType())) {
            Chest chest = (Chest) block.getState();
            player.openInventory(chest.getBlockInventory());
        }
    }

    private void handleCreation(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            plugin.getMessageManager().send(player, "creation.no-item");
            event.setCancelled(true);
            return;
        }

        if (!player.hasPermission("chestshop.create")) {
            plugin.getMessageManager().send(player, "general.no-permission");
            event.setCancelled(true);
            return;
        }

        if (plugin.getShopManager().countShopsByOwner(player.getUniqueId()) >= plugin.getShopManager().getMaxShopsForPlayer(player)) {
            plugin.getMessageManager().send(player, "creation.max-shops");
            event.setCancelled(true);
            return;
        }

        if (plugin.getConfig().getBoolean("worldguard-block-creation", true)
                && plugin.getWorldGuardHook() != null
                && plugin.getWorldGuardHook().isHooked()
                && plugin.getWorldGuardHook().isInProtectedRegion(event.getClickedBlock().getLocation())) {
            plugin.getMessageManager().send(player, "creation.worldguard-blocked");
            event.setCancelled(true);
            return;
        }

        PendingAction pendingAction = new PendingAction(PendingActionType.CREATE_MODE);
        pendingAction.setChestLocation(event.getClickedBlock().getLocation());
        pendingAction.setItem(itemInHand.clone());
        pendingAction.getItem().setAmount(1);
        pendingAction.setSignFace(plugin.getShopManager().resolveSignFace(player));
        plugin.getPendingActions().put(player.getUniqueId(), pendingAction);

        event.setCancelled(true);
        plugin.getMessageManager().send(player, "creation.mode-prompt");
    }

    private boolean isChest(Material material) {
        return material == Material.CHEST || material == Material.TRAPPED_CHEST;
    }
}