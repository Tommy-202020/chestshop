package dev.minecore.asteroidChests.listeners;

import dev.minecore.asteroidChests.ChestShopPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public final class ChunkLoadListener implements Listener {

    private final ChestShopPlugin plugin;

    public ChunkLoadListener(ChestShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.getShopManager().restoreShopsInChunk(event.getChunk());
    }
}