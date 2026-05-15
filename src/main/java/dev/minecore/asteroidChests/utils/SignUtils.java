package dev.minecore.asteroidChests.utils;

import dev.minecore.asteroidChests.ChestShopPlugin;
import dev.minecore.asteroidChests.models.Shop;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;

public final class SignUtils {

    private SignUtils() {
    }

    public static boolean placeOrUpdateSign(ChestShopPlugin plugin, Shop shop) {
        if (shop.getChestLocation() == null || shop.getChestLocation().getWorld() == null) {
            return false;
        }
        Block signBlock = shop.getSignLocation().getBlock();
        if (!signBlock.getType().isAir() && !(signBlock.getState() instanceof Sign)) {
            return false;
        }
        if (signBlock.getType().isAir()) {
            signBlock.setType(Material.OAK_WALL_SIGN, false);
        }
        BlockData data = signBlock.getBlockData();
        if (data instanceof Directional directional) {
            directional.setFacing(shop.getSignFace());
            signBlock.setBlockData(directional, false);
        }
        updateSignText(plugin, shop);
        return true;
    }

    public static void updateSignText(ChestShopPlugin plugin, Shop shop) {
        if (shop.getSignLocation() == null) {
            return;
        }
        Block signBlock = shop.getSignLocation().getBlock();
        if (!(signBlock.getState() instanceof Sign sign)) {
            return;
        }
        Map<String, String> placeholders = plugin.getMessageManager().placeholders(shop, null, null, null, null, null, null, null, null);
        sign.setLine(0, ColorUtils.color(plugin.getMessageManager().formatPlain("sign.line-1", placeholders)));
        sign.setLine(1, ColorUtils.color(plugin.getMessageManager().formatPlain("sign.line-2", placeholders)));
        sign.setLine(2, ColorUtils.color(plugin.getMessageManager().formatPlain("sign.line-3", placeholders)));
        sign.setLine(3, ColorUtils.color(plugin.getMessageManager().formatPlain("sign.line-4", placeholders)));
        sign.update(true, false);
    }

    public static void removeSign(ChestShopPlugin plugin, Shop shop) {
        if (shop.getSignLocation() == null) {
            return;
        }
        Block signBlock = shop.getSignLocation().getBlock();
        if (signBlock.getType().isAir()) {
            return;
        }
        signBlock.setType(Material.AIR, false);
    }
}