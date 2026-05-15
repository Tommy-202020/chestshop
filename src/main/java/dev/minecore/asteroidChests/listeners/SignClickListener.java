package dev.minecore.asteroidChests.listeners;

import dev.minecore.asteroidChests.ChestShopPlugin;
import dev.minecore.asteroidChests.models.Shop;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class SignClickListener implements Listener {

    private final ChestShopPlugin plugin;

    public SignClickListener(ChestShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof Sign)) {
            return;
        }

        Shop shop = plugin.getShopManager().getShopAtSign(block.getLocation());
        if (shop == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (shop.getOwnerUuid().equals(player.getUniqueId())) {
            plugin.getMessageManager().sendOwnerMenu(player, shop);
        } else {
            plugin.getMessageManager().sendPlayerMenu(player, shop);
        }
    }
}