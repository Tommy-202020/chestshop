package dev.minecore.asteroidChests.listeners;

import dev.minecore.asteroidChests.ChestShopPlugin;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {

    private final ChestShopPlugin plugin;

    public PlayerJoinListener(ChestShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        List<String> notifications = plugin.drainOfflineNotifications(player.getUniqueId());
        for (String notification : notifications) {
            player.sendMessage(notification);
        }
    }
}