package dev.minecore.asteroidChests.models;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

public final class PendingAction {

    private PendingActionType type;
    private UUID shopId;
    private Location chestLocation;
    private ItemStack item;
    private BlockFace signFace;
    private ShopMode mode;
    private TransactionType transactionType;

    public PendingAction(PendingActionType type) {
        this.type = type;
    }

    public PendingActionType getType() {
        return type;
    }

    public void setType(PendingActionType type) {
        this.type = type;
    }

    public UUID getShopId() {
        return shopId;
    }

    public void setShopId(UUID shopId) {
        this.shopId = shopId;
    }

    public Location getChestLocation() {
        return chestLocation;
    }

    public void setChestLocation(Location chestLocation) {
        this.chestLocation = chestLocation;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public BlockFace getSignFace() {
        return signFace;
    }

    public void setSignFace(BlockFace signFace) {
        this.signFace = signFace;
    }

    public ShopMode getMode() {
        return mode;
    }

    public void setMode(ShopMode mode) {
        this.mode = mode;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }
}