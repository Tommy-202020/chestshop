package dev.minecore.asteroidChests;

import dev.minecore.asteroidChests.commands.ChestShopCommand;
import dev.minecore.asteroidChests.listeners.BlockBreakListener;
import dev.minecore.asteroidChests.listeners.ChatInputListener;
import dev.minecore.asteroidChests.listeners.ChestClickListener;
import dev.minecore.asteroidChests.listeners.ChunkLoadListener;
import dev.minecore.asteroidChests.listeners.PlayerJoinListener;
import dev.minecore.asteroidChests.listeners.SignClickListener;
import dev.minecore.asteroidChests.managers.MessageManager;
import dev.minecore.asteroidChests.managers.ShopManager;
import dev.minecore.asteroidChests.models.PendingAction;
import dev.minecore.asteroidChests.utils.VaultHook;
import dev.minecore.asteroidChests.utils.WorldGuardHook;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChestShopPlugin extends JavaPlugin {

    private static ChestShopPlugin instance;

    private final Map<UUID, PendingAction> pendingActions = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> offlineNotifications = new ConcurrentHashMap<>();
    private MessageManager messageManager;
    private ShopManager shopManager;
    private VaultHook vaultHook;
    private WorldGuardHook worldGuardHook;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        ensureMessagesFile();

        this.messageManager = new MessageManager(this);
        this.messageManager.reload();
        this.vaultHook = new VaultHook(this);
        if (!vaultHook.isHooked()) {
            getLogger().severe("Vault economy service was not found. Disabling ChestShop.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.worldGuardHook = new WorldGuardHook(this);
        if (worldGuardHook.isHooked()) {
            getLogger().info("WorldGuard integration enabled.");
        }

        this.shopManager = new ShopManager(this, messageManager, vaultHook);
        shopManager.loadShops();

        Bukkit.getPluginManager().registerEvents(new ChestClickListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SignClickListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatInputListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChunkLoadListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockBreakListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        ChestShopCommand command = new ChestShopCommand(this);
        if (getCommand("chestshop") != null) {
            getCommand("chestshop").setExecutor(command);
            getCommand("chestshop").setTabCompleter(command);
        }

        Bukkit.getScheduler().runTask(this, shopManager::restoreLoadedShops);
    }

    @Override
    public void onDisable() {
        if (shopManager != null) {
            shopManager.saveAll();
        }
    }

    private void ensureMessagesFile() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveResource("messages.yml", false);
        }
    }

    public static ChestShopPlugin getInstance() {
        return instance;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    public Map<UUID, PendingAction> getPendingActions() {
        return pendingActions;
    }

    public void queueOfflineNotification(UUID playerId, String message) {
        offlineNotifications.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(message);
    }

    public List<String> drainOfflineNotifications(UUID playerId) {
        List<String> messages = offlineNotifications.remove(playerId);
        return messages == null ? List.of() : messages;
    }
}
