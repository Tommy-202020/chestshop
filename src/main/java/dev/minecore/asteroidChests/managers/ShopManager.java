package dev.minecore.asteroidChests.managers;

import dev.minecore.asteroidChests.ChestShopPlugin;
import dev.minecore.asteroidChests.models.Shop;
import dev.minecore.asteroidChests.models.ShopMode;
import dev.minecore.asteroidChests.models.TransactionType;
import dev.minecore.asteroidChests.utils.EntityUtils;
import dev.minecore.asteroidChests.utils.SignUtils;
import dev.minecore.asteroidChests.utils.VaultHook;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.Chunk;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ShopManager {

    private final ChestShopPlugin plugin;
    private final MessageManager messageManager;
    private final VaultHook vaultHook;
    private final File file;
    private final Map<UUID, Shop> shops = new LinkedHashMap<>();
    private final Map<String, UUID> chestIndex = new HashMap<>();
    private final Map<String, UUID> signIndex = new HashMap<>();

    public ShopManager(ChestShopPlugin plugin, MessageManager messageManager, VaultHook vaultHook) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.vaultHook = vaultHook;
        this.file = new File(plugin.getDataFolder(), "shops.yml");
    }

    public void loadShops() {
        shops.clear();
        chestIndex.clear();
        signIndex.clear();
        ensureFile();
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        for (String key : configuration.getKeys(false)) {
            ConfigurationSection section = configuration.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            UUID shopId;
            try {
                shopId = UUID.fromString(key);
            } catch (IllegalArgumentException exception) {
                continue;
            }
            Shop shop = Shop.fromSection(shopId, section);
            if (shop != null) {
                register(shop);
            }
        }
    }

    public void saveAll() {
        ensureFile();
        YamlConfiguration configuration = new YamlConfiguration();
        for (Shop shop : shops.values()) {
            writeShop(configuration, shop);
        }
        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Unable to save shops.yml: " + exception.getMessage());
        }
    }

    public void restoreLoadedShops() {
        for (Shop shop : shops.values()) {
            restoreShop(shop);
        }
    }

    public void restoreShopsInChunk(Chunk chunk) {
        for (Shop shop : shops.values()) {
            Location chestLocation = shop.getChestLocation();
            if (chestLocation == null || chestLocation.getWorld() == null) {
                continue;
            }
            if (chestLocation.getChunk().equals(chunk)) {
                restoreShop(shop);
            }
        }
    }

    public Shop createShop(Player owner, Location chestLocation, ItemStack item, ShopMode mode, double price, BlockFace signFace) {
        int maxShops = getMaxShopsForPlayer(owner);
        if (countShopsByOwner(owner.getUniqueId()) >= maxShops) {
            messageManager.send(owner, "creation.max-shops");
            return null;
        }

        if (getShopAtBlock(chestLocation) != null) {
            messageManager.send(owner, "creation.already-shop");
            return null;
        }

        Shop shop = new Shop(UUID.randomUUID(), owner.getUniqueId(), owner.getName(), chestLocation.getWorld().getName(), chestLocation.getBlockX(), chestLocation.getBlockY(), chestLocation.getBlockZ(), normalizeItem(item), price, price, mode, null, signFace);
        register(shop);
        saveShop(shop);

        if (!SignUtils.placeOrUpdateSign(plugin, shop)) {
            unregister(shop);
            deleteFromFile(shop.getId());
            messageManager.send(owner, "creation.no-sign-space");
            return null;
        }

        if (!EntityUtils.spawnFloatingEntity(plugin, shop, plugin.getConfig().getDouble("floating-item-height-offset", 1.2D))) {
            messageManager.send(owner, "creation.entity-failed");
        }

        updateSign(shop);
        messageManager.send(owner, "creation.success", messageManager.placeholders(shop, null, null, null, null, owner, null, null, null));
        return shop;
    }

    public void updatePrice(Shop shop, double price) {
        shop.setPrice(price);
        saveShop(shop);
        updateSign(shop);
    }

    public void updateBuyPrice(Shop shop, double price) {
        shop.setBuyPrice(price);
        if (shop.getMode() != ShopMode.BOTH) {
            shop.setSellPrice(price);
        }
        saveShop(shop);
        updateSign(shop);
    }

    public void updateSellPrice(Shop shop, double price) {
        shop.setSellPrice(price);
        if (shop.getMode() != ShopMode.BOTH) {
            shop.setBuyPrice(price);
        }
        saveShop(shop);
        updateSign(shop);
    }

    public void updateMode(Shop shop, ShopMode mode) {
        shop.setMode(mode);
        saveShop(shop);
        updateSign(shop);
    }

    public void deleteShop(Shop shop, boolean removeBlocks) {
        if (removeBlocks) {
            SignUtils.removeSign(plugin, shop);
        }
        EntityUtils.removeFloatingEntity(plugin, shop);
        unregister(shop);
        deleteFromFile(shop.getId());
    }

    public void processTransaction(Player player, Shop shop, TransactionType type, int amount) {
        if (amount <= 0) {
            messageManager.send(player, "shop.invalid-amount");
            return;
        }
        if (type == TransactionType.BUY) {
            handleBuy(player, shop, amount);
        } else {
            handleSell(player, shop, amount);
        }
    }

    public void handleBuy(Player player, Shop shop, int amount) {
        if (shop.getMode() == ShopMode.SELL) {
            messageManager.send(player, "shop.buy-not-available", messageManager.placeholders(shop, null, null, null, null, player, null, String.valueOf(amount), null));
            return;
        }

        Inventory chestInventory = getChestInventory(shop);
        if (chestInventory == null) {
            messageManager.send(player, "general.shop-not-found");
            return;
        }

        ItemStack item = shop.getItem().clone();
        item.setAmount(amount);

        int stock = countMatching(chestInventory, shop.getItem());
        if (stock < amount) {
            messageManager.send(player, "shop.out-of-stock", messageManager.placeholders(shop, null, null, null, String.valueOf(stock), player, null, String.valueOf(amount), null));
            return;
        }

        if (!canFit(player.getInventory(), shop.getItem(), amount)) {
            messageManager.send(player, "shop.full", messageManager.placeholders(shop, null, null, null, null, player, null, String.valueOf(amount), null));
            return;
        }

        double unitPrice = shop.getPriceFor(TransactionType.BUY);
        double total = unitPrice * amount;
        if (!vaultHook.hasBalance(player, total)) {
            messageManager.send(player, "shop.not-enough-money", messageManager.placeholders(shop, null, null, null, null, player, null, String.valueOf(amount), String.valueOf(total)));
            return;
        }

        VaultHook.EconomyResult withdraw = vaultHook.withdraw(player, total);
        if (!withdraw.success()) {
            messageManager.send(player, "shop.economy-failed", messageManager.placeholders(shop, null, null, null, null, player, null, String.valueOf(amount), String.valueOf(total)));
            return;
        }

        chestInventory.removeItem(item);
        player.getInventory().addItem(item);
        vaultHook.deposit(shop.getOwnerUuid(), total);

        saveShop(shop);
        if (plugin.getConfig().getBoolean("sign-update-on-transaction", true)) {
            updateSign(shop);
        }
        if (plugin.getConfig().getBoolean("action-bar-on-transaction", true)) {
            messageManager.sendActionBar(player, "action-bar.buy", messageManager.placeholders(shop, null, null, null, null, player, null, String.valueOf(amount), String.valueOf(total)));
        }
        messageManager.send(player, "shop.buy-success", messageManager.placeholders(shop, null, null, null, null, player, null, String.valueOf(amount), String.valueOf(total), String.valueOf(unitPrice)));
        notifyOwner(shop, "shop.owner-buy-notification", player, amount, total, unitPrice);
    }

    public void handleSell(Player player, Shop shop, int amount) {
        if (shop.getMode() == ShopMode.BUY) {
            messageManager.send(player, "shop.sell-not-available", messageManager.placeholders(shop, null, null, null, null, player, null, String.valueOf(amount), null));
            return;
        }

        Inventory chestInventory = getChestInventory(shop);
        if (chestInventory == null) {
            messageManager.send(player, "general.shop-not-found");
            return;
        }

        ItemStack item = shop.getItem().clone();
        item.setAmount(amount);

        if (countMatching(player.getInventory(), shop.getItem()) < amount) {
            messageManager.send(player, "shop.not-enough-items", messageManager.placeholders(shop, null, null, null, null, player, null, String.valueOf(amount), null));
            return;
        }

        if (!canFit(chestInventory, shop.getItem(), amount)) {
            messageManager.send(player, "shop.full", messageManager.placeholders(shop, null, null, null, null, player, null, String.valueOf(amount), null));
            return;
        }

        double unitPrice = shop.getPriceFor(TransactionType.SELL);
        double total = unitPrice * amount;
        if (!vaultHook.hasBalance(shop.getOwnerUuid(), total)) {
            messageManager.send(player, "shop.owner-no-money", messageManager.placeholders(shop, null, null, null, null, player, null, String.valueOf(amount), String.valueOf(total)));
            return;
        }

        VaultHook.EconomyResult withdraw = vaultHook.withdraw(shop.getOwnerUuid(), total);
        if (!withdraw.success()) {
            messageManager.send(player, "shop.owner-no-money", messageManager.placeholders(shop, null, null, null, null, player, null, String.valueOf(amount), String.valueOf(total)));
            return;
        }

        player.getInventory().removeItem(item);
        chestInventory.addItem(item);
        vaultHook.deposit(player.getUniqueId(), total);

        saveShop(shop);
        if (plugin.getConfig().getBoolean("sign-update-on-transaction", true)) {
            updateSign(shop);
        }
        if (plugin.getConfig().getBoolean("action-bar-on-transaction", true)) {
            messageManager.sendActionBar(player, "action-bar.sell", messageManager.placeholders(shop, null, null, null, null, player, null, String.valueOf(amount), String.valueOf(total)));
        }
        messageManager.send(player, "shop.sell-success", messageManager.placeholders(shop, null, null, null, null, player, null, String.valueOf(amount), String.valueOf(total), String.valueOf(unitPrice)));
        notifyOwner(shop, "shop.owner-sell-notification", player, amount, total, unitPrice);
    }

    public void updateSign(Shop shop) {
        SignUtils.placeOrUpdateSign(plugin, shop);
        SignUtils.updateSignText(plugin, shop);
    }

    public void restoreShop(Shop shop) {
        if (shop.getChestLocation() == null || shop.getChestLocation().getWorld() == null) {
            return;
        }
        if (shop.getChestLocation().getBlock().getType() != Material.CHEST && shop.getChestLocation().getBlock().getType() != Material.TRAPPED_CHEST) {
            return;
        }
        if (!SignUtils.placeOrUpdateSign(plugin, shop)) {
            return;
        }
        SignUtils.updateSignText(plugin, shop);
        if (!EntityUtils.hasFloatingEntity(plugin, shop)) {
            EntityUtils.spawnFloatingEntity(plugin, shop, plugin.getConfig().getDouble("floating-item-height-offset", 1.2D));
        }
    }

    public Shop getShop(UUID id) {
        return shops.get(id);
    }

    public Shop getShopAtBlock(Location location) {
        UUID id = chestIndex.get(locationKey(location));
        if (id != null) {
            return shops.get(id);
        }
        id = signIndex.get(locationKey(location));
        return id == null ? null : shops.get(id);
    }

    public Shop getShopAtChest(Location location) {
        UUID id = chestIndex.get(locationKey(location));
        return id == null ? null : shops.get(id);
    }

    public Shop getShopAtSign(Location location) {
        UUID id = signIndex.get(locationKey(location));
        return id == null ? null : shops.get(id);
    }

    public int countShopsByOwner(UUID owner) {
        int count = 0;
        for (Shop shop : shops.values()) {
            if (shop.getOwnerUuid().equals(owner)) {
                count++;
            }
        }
        return count;
    }

    public List<Shop> getShopsByOwner(UUID owner) {
        List<Shop> result = new ArrayList<>();
        for (Shop shop : shops.values()) {
            if (shop.getOwnerUuid().equals(owner)) {
                result.add(shop);
            }
        }
        return result;
    }

    public int getMaxShopsForPlayer(Player player) {
        int maxShops = plugin.getConfig().getInt("max-shops-per-player", 10);
        
        for (String perm : player.getEffectivePermissions().stream()
                .map(p -> p.getPermission())
                .filter(p -> p.startsWith("chestshop.limit."))
                .toList()) {
            try {
                String[] parts = perm.split("\\.");
                if (parts.length == 3) {
                    int limit = Integer.parseInt(parts[2]);
                    if (limit > maxShops) {
                        maxShops = limit;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        
        return maxShops;
    }

    public BlockFace resolveSignFace(Player player) {
        String configured = plugin.getConfig().getString("sign-facing", "AUTO");
        if (configured != null && !configured.equalsIgnoreCase("AUTO")) {
            try {
                return BlockFace.valueOf(configured.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return player.getFacing().getOppositeFace();
    }

    public int getStock(Shop shop) {
        Inventory inventory = getChestInventory(shop);
        return inventory == null ? 0 : countMatching(inventory, shop.getItem());
    }

    public int getSpace(Shop shop) {
        Inventory inventory = getChestInventory(shop);
        return inventory == null ? 0 : remainingCapacity(inventory, shop.getItem());
    }

    public String getDisplayName(ItemStack stack) {
        if (stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
        }
        return stack.getType().name().toLowerCase().replace('_', ' ');
    }

    private void register(Shop shop) {
        shops.put(shop.getId(), shop);
        chestIndex.put(locationKey(shop.getChestLocation()), shop.getId());
        signIndex.put(locationKey(shop.getSignLocation()), shop.getId());
    }

    private void unregister(Shop shop) {
        shops.remove(shop.getId());
        chestIndex.remove(locationKey(shop.getChestLocation()));
        signIndex.remove(locationKey(shop.getSignLocation()));
    }

    private void saveShop(Shop shop) {
        ensureFile();
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        writeShop(configuration, shop);
        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Unable to save shop " + shop.getId() + ": " + exception.getMessage());
        }
    }

    private void deleteFromFile(UUID id) {
        ensureFile();
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        configuration.set(id.toString(), null);
        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Unable to delete shop " + id + ": " + exception.getMessage());
        }
    }

    private void writeShop(YamlConfiguration configuration, Shop shop) {
        String path = shop.getId().toString();
        configuration.set(path + ".owner-uuid", shop.getOwnerUuid().toString());
        configuration.set(path + ".owner-name", shop.getOwnerName());
        configuration.set(path + ".world", shop.getWorld());
        configuration.set(path + ".x", shop.getX());
        configuration.set(path + ".y", shop.getY());
        configuration.set(path + ".z", shop.getZ());
        configuration.set(path + ".item", shop.getItem());
        configuration.set(path + ".price", shop.getPrice());
        configuration.set(path + ".buy-price", shop.getBuyPrice());
        configuration.set(path + ".sell-price", shop.getSellPrice());
        configuration.set(path + ".mode", shop.getMode().name());
        configuration.set(path + ".entity-uuid", shop.getEntityUuid() == null ? null : shop.getEntityUuid().toString());
        configuration.set(path + ".sign-face", shop.getSignFace().name());
    }

    private void ensureFile() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe("Unable to create shops.yml: " + exception.getMessage());
            }
        }
    }

    private ItemStack normalizeItem(ItemStack item) {
        ItemStack clone = item.clone();
        clone.setAmount(1);
        return clone;
    }

    private String locationKey(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private Inventory getChestInventory(Shop shop) {
        World world = Bukkit.getWorld(shop.getWorld());
        if (world == null) {
            return null;
        }
        Block block = world.getBlockAt(shop.getX(), shop.getY(), shop.getZ());
        if (!(block.getState() instanceof Chest chest)) {
            return null;
        }
        return chest.getInventory();
    }

    private int remainingCapacity(Inventory inventory, ItemStack reference) {
        int space = 0;
        int maxStack = reference.getMaxStackSize();
        for (ItemStack stack : inventory.getContents()) {
            if (stack == null || stack.getType().isAir()) {
                space += maxStack;
            } else if (stack.isSimilar(reference)) {
                space += Math.max(0, maxStack - stack.getAmount());
            }
        }
        return space;
    }

    private boolean canFit(Inventory inventory, ItemStack reference, int amount) {
        return remainingCapacity(inventory, reference) >= amount;
    }

    private int countMatching(Inventory inventory, ItemStack reference) {
        int total = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && stack.isSimilar(reference)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private void notifyOwner(Shop shop, String messageKey, Player actor, int amount, double total, double unitPrice) {
        Player owner = Bukkit.getPlayer(shop.getOwnerUuid());
        Map<String, String> placeholders = messageManager.placeholders(shop, null, null, null, null, actor, null, String.valueOf(amount), String.valueOf(total), String.valueOf(unitPrice));
        if (owner != null && owner.isOnline()) {
            messageManager.send(owner, messageKey, placeholders);
            return;
        }
        plugin.queueOfflineNotification(shop.getOwnerUuid(), messageManager.format(messageKey, placeholders));
    }
}