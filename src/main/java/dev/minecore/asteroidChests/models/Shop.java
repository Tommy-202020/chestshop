package dev.minecore.asteroidChests.models;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import dev.minecore.asteroidChests.models.TransactionType;

public final class Shop {

    private final UUID id;
    private UUID ownerUuid;
    private String ownerName;
    private String world;
    private int x;
    private int y;
    private int z;
    private ItemStack item;
    private double buyPrice;
    private double sellPrice;
    private ShopMode mode;
    private UUID entityUuid;
    private BlockFace signFace;

    public Shop(UUID id, UUID ownerUuid, String ownerName, String world, int x, int y, int z, ItemStack item, double buyPrice, double sellPrice, ShopMode mode, UUID entityUuid, BlockFace signFace) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.item = item;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.mode = mode;
        this.entityUuid = entityUuid;
        this.signFace = signFace;
    }

    public static Shop fromSection(UUID id, ConfigurationSection section) {
        try {
            UUID ownerUuid = UUID.fromString(section.getString("owner-uuid"));
            String ownerName = section.getString("owner-name", "Unknown");
            String world = section.getString("world");
            int x = section.getInt("x");
            int y = section.getInt("y");
            int z = section.getInt("z");
            ItemStack item = section.getItemStack("item");
            if (world == null || item == null) {
                return null;
            }
            item.setAmount(1);
            double legacyPrice = section.getDouble("price");
            double buyPrice = section.contains("buy-price") ? section.getDouble("buy-price") : legacyPrice;
            double sellPrice = section.contains("sell-price") ? section.getDouble("sell-price") : legacyPrice;
            ShopMode mode = ShopMode.valueOf(section.getString("mode", "BUY"));
            String entityId = section.getString("entity-uuid");
            UUID entityUuid = entityId == null ? null : UUID.fromString(entityId);
            BlockFace signFace = BlockFace.valueOf(section.getString("sign-face", BlockFace.SOUTH.name()));
            return new Shop(id, ownerUuid, ownerName, world, x, y, z, item, buyPrice, sellPrice, mode, entityUuid, signFace);
        } catch (Exception exception) {
            return null;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return mode == ShopMode.SELL ? sellPrice : buyPrice;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public double getPriceFor(TransactionType transactionType) {
        return transactionType == TransactionType.SELL ? sellPrice : buyPrice;
    }

    public ShopMode getMode() {
        return mode;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public BlockFace getSignFace() {
        return signFace;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public void setPrice(double price) {
        this.buyPrice = price;
        this.sellPrice = price;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public void setMode(ShopMode mode) {
        this.mode = mode;
    }

    public void setEntityUuid(UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    public void setSignFace(BlockFace signFace) {
        this.signFace = signFace;
    }

    public Location getChestLocation() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return null;
        }
        return new Location(bukkitWorld, x, y, z);
    }

    public Location getSignLocation() {
        Location chest = getChestLocation();
        if (chest == null) {
            return null;
        }
        return chest.getBlock().getRelative(signFace).getLocation();
    }

    public String getDisplayName() {
        if (item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name().toLowerCase().replace('_', ' ');
    }

    public int getStock() {
        return 0;
    }

    public int getSpace() {
        return 0;
    }
}