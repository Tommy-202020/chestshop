package dev.minecore.asteroidChests.listeners;

import dev.minecore.asteroidChests.ChestShopPlugin;
import dev.minecore.asteroidChests.models.Shop;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

public final class BlockBreakListener implements Listener {

    private final ChestShopPlugin plugin;

    public BlockBreakListener(ChestShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Shop shop = plugin.getShopManager().getShopAtBlock(block.getLocation());
        if (shop == null) {
            return;
        }

        Player player = event.getPlayer();
        boolean owner = shop.getOwnerUuid().equals(player.getUniqueId());

        if (owner && plugin.getConfig().getBoolean("allow-break-to-delete", false)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getShopManager().deleteShop(shop, false);
                }
            }.runTask(plugin);
            plugin.getMessageManager().send(player, "break.deleted", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
            return;
        }

        event.setCancelled(true);
        if (owner) {
            plugin.getMessageManager().send(player, "break.use-delete", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
        } else {
            plugin.getMessageManager().send(player, "break.not-owner", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
        }
    }
}