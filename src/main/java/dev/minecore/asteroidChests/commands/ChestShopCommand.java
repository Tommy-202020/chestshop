package dev.minecore.asteroidChests.commands;

import dev.minecore.asteroidChests.ChestShopPlugin;
import dev.minecore.asteroidChests.models.PendingAction;
import dev.minecore.asteroidChests.models.PendingActionType;
import dev.minecore.asteroidChests.models.Shop;
import dev.minecore.asteroidChests.models.ShopMode;
import dev.minecore.asteroidChests.models.TransactionType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class ChestShopCommand implements CommandExecutor, TabCompleter {

    private final ChestShopPlugin plugin;

    public ChestShopCommand(ChestShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            plugin.getMessageManager().sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (!(sender instanceof Player player)) {
                plugin.getMessageManager().send(sender, "general.player-only");
                return true;
            }
            plugin.getMessageManager().sendShopList(player, plugin.getShopManager().getShopsByOwner(player.getUniqueId()));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("chestshop.admin")) {
                plugin.getMessageManager().send(sender, "general.no-permission");
                return true;
            }
            plugin.reloadConfig();
            plugin.getMessageManager().reload();
            plugin.getMessageManager().send(sender, "general.reloaded");
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("chestshop.admin")) {
                plugin.getMessageManager().send(sender, "general.no-permission");
                return true;
            }
            if (args.length < 5) {
                plugin.getMessageManager().send(sender, "general.remove-usage");
                return true;
            }
            int x;
            int y;
            int z;
            try {
                x = Integer.parseInt(args[1]);
                y = Integer.parseInt(args[2]);
                z = Integer.parseInt(args[3]);
            } catch (NumberFormatException exception) {
                plugin.getMessageManager().send(sender, "general.invalid-location");
                return true;
            }
            World world = Bukkit.getWorld(args[4]);
            if (world == null) {
                plugin.getMessageManager().send(sender, "general.invalid-world");
                return true;
            }
            Shop shop = plugin.getShopManager().getShopAtChest(new Location(world, x, y, z));
            if (shop == null) {
                plugin.getMessageManager().send(sender, "general.shop-not-found");
                return true;
            }
            plugin.getShopManager().deleteShop(shop, true);
            plugin.getMessageManager().send(sender, "general.removed");
            return true;
        }

        if (args[0].equalsIgnoreCase("transact")) {
            if (!(sender instanceof Player player) || args.length < 3) {
                return true;
            }
            UUID shopId = parseUuid(args[2]);
            if (shopId == null) {
                return true;
            }
            Shop shop = plugin.getShopManager().getShop(shopId);
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                return true;
            }
            TransactionType type = args[1].equalsIgnoreCase("sell") ? TransactionType.SELL : TransactionType.BUY;
            plugin.getMessageManager().promptTransactionAmount(player, shop, type);
            return true;
        }

        if (args[0].equalsIgnoreCase("editprice")) {
            if (!(sender instanceof Player player) || args.length < 2) {
                return true;
            }
            UUID shopId = parseUuid(args[1]);
            Shop shop = shopId == null ? null : plugin.getShopManager().getShop(shopId);
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                return true;
            }
            if (!canManageShop(player, shop)) {
                plugin.getMessageManager().send(player, "general.no-permission");
                return true;
            }
            PendingAction pendingAction = new PendingAction(PendingActionType.OWNER_EDIT_PRICE);
            pendingAction.setShopId(shop.getId());
            plugin.getPendingActions().put(player.getUniqueId(), pendingAction);
            plugin.getMessageManager().send(player, "owner.edit-price-prompt", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
            return true;
        }

        if (args[0].equalsIgnoreCase("editbuyprice")) {
            if (!(sender instanceof Player player) || args.length < 2) {
                return true;
            }
            UUID shopId = parseUuid(args[1]);
            Shop shop = shopId == null ? null : plugin.getShopManager().getShop(shopId);
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                return true;
            }
            if (!canManageShop(player, shop)) {
                plugin.getMessageManager().send(player, "general.no-permission");
                return true;
            }
            PendingAction pendingAction = new PendingAction(PendingActionType.OWNER_EDIT_BUY_PRICE);
            pendingAction.setShopId(shop.getId());
            plugin.getPendingActions().put(player.getUniqueId(), pendingAction);
            plugin.getMessageManager().send(player, "owner.edit-buy-price-prompt", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
            return true;
        }

        if (args[0].equalsIgnoreCase("editsellprice")) {
            if (!(sender instanceof Player player) || args.length < 2) {
                return true;
            }
            UUID shopId = parseUuid(args[1]);
            Shop shop = shopId == null ? null : plugin.getShopManager().getShop(shopId);
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                return true;
            }
            if (!canManageShop(player, shop)) {
                plugin.getMessageManager().send(player, "general.no-permission");
                return true;
            }
            PendingAction pendingAction = new PendingAction(PendingActionType.OWNER_EDIT_SELL_PRICE);
            pendingAction.setShopId(shop.getId());
            plugin.getPendingActions().put(player.getUniqueId(), pendingAction);
            plugin.getMessageManager().send(player, "owner.edit-sell-price-prompt", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
            return true;
        }

        if (args[0].equalsIgnoreCase("editmode")) {
            if (!(sender instanceof Player player) || args.length < 2) {
                return true;
            }
            UUID shopId = parseUuid(args[1]);
            Shop shop = shopId == null ? null : plugin.getShopManager().getShop(shopId);
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                return true;
            }
            if (!canManageShop(player, shop)) {
                plugin.getMessageManager().send(player, "general.no-permission");
                return true;
            }
            PendingAction pendingAction = new PendingAction(PendingActionType.OWNER_EDIT_MODE);
            pendingAction.setShopId(shop.getId());
            plugin.getPendingActions().put(player.getUniqueId(), pendingAction);
            plugin.getMessageManager().send(player, "owner.edit-mode-prompt", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            if (!(sender instanceof Player player) || args.length < 2) {
                return true;
            }
            UUID shopId = parseUuid(args[1]);
            Shop shop = shopId == null ? null : plugin.getShopManager().getShop(shopId);
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                return true;
            }
            if (!canManageShop(player, shop)) {
                plugin.getMessageManager().send(player, "general.no-permission");
                return true;
            }
            plugin.getMessageManager().sendDeleteConfirm(player, shop);
            PendingAction pendingAction = new PendingAction(PendingActionType.DELETE_CONFIRMATION);
            pendingAction.setShopId(shop.getId());
            plugin.getPendingActions().put(player.getUniqueId(), pendingAction);
            return true;
        }

        if (args[0].equalsIgnoreCase("deleteconfirm")) {
            if (!(sender instanceof Player player) || args.length < 2) {
                return true;
            }
            UUID shopId = parseUuid(args[1]);
            Shop shop = shopId == null ? null : plugin.getShopManager().getShop(shopId);
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                return true;
            }
            if (!canManageShop(player, shop)) {
                plugin.getMessageManager().send(player, "general.no-permission");
                return true;
            }
            plugin.getShopManager().deleteShop(shop, true);
            plugin.getPendingActions().remove(player.getUniqueId());
            plugin.getMessageManager().send(player, "owner.delete-success", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
            return true;
        }

        if (args[0].equalsIgnoreCase("deletecancel")) {
            if (!(sender instanceof Player player) || args.length < 2) {
                return true;
            }
            UUID shopId = parseUuid(args[1]);
            Shop shop = shopId == null ? null : plugin.getShopManager().getShop(shopId);
            if (shop != null) {
                plugin.getPendingActions().remove(player.getUniqueId());
                plugin.getMessageManager().send(player, "owner.delete-cancelled", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("setprice")) {
            if (!(sender instanceof Player player) || args.length < 3) {
                return true;
            }
            UUID shopId = parseUuid(args[1]);
            Shop shop = shopId == null ? null : plugin.getShopManager().getShop(shopId);
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                return true;
            }
            if (!canManageShop(player, shop)) {
                plugin.getMessageManager().send(player, "general.no-permission");
                return true;
            }
            double price;
            try {
                price = Double.parseDouble(args[2]);
            } catch (NumberFormatException exception) {
                plugin.getMessageManager().send(player, "shop.invalid-price");
                return true;
            }
            plugin.getShopManager().updatePrice(shop, price);
            plugin.getPendingActions().remove(player.getUniqueId());
            plugin.getMessageManager().send(player, "owner.price-updated", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
            return true;
        }

        if (args[0].equalsIgnoreCase("setbuyprice")) {
            if (!(sender instanceof Player player) || args.length < 3) {
                return true;
            }
            UUID shopId = parseUuid(args[1]);
            Shop shop = shopId == null ? null : plugin.getShopManager().getShop(shopId);
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                return true;
            }
            if (!canManageShop(player, shop)) {
                plugin.getMessageManager().send(player, "general.no-permission");
                return true;
            }
            double price;
            try {
                price = Double.parseDouble(args[2]);
            } catch (NumberFormatException exception) {
                plugin.getMessageManager().send(player, "shop.invalid-price");
                return true;
            }
            plugin.getShopManager().updateBuyPrice(shop, price);
            plugin.getPendingActions().remove(player.getUniqueId());
            plugin.getMessageManager().send(player, "owner.buy-price-updated", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
            return true;
        }

        if (args[0].equalsIgnoreCase("setsellprice")) {
            if (!(sender instanceof Player player) || args.length < 3) {
                return true;
            }
            UUID shopId = parseUuid(args[1]);
            Shop shop = shopId == null ? null : plugin.getShopManager().getShop(shopId);
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                return true;
            }
            if (!canManageShop(player, shop)) {
                plugin.getMessageManager().send(player, "general.no-permission");
                return true;
            }
            double price;
            try {
                price = Double.parseDouble(args[2]);
            } catch (NumberFormatException exception) {
                plugin.getMessageManager().send(player, "shop.invalid-price");
                return true;
            }
            plugin.getShopManager().updateSellPrice(shop, price);
            plugin.getPendingActions().remove(player.getUniqueId());
            plugin.getMessageManager().send(player, "owner.sell-price-updated", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
            return true;
        }

        if (args[0].equalsIgnoreCase("setmode")) {
            if (!(sender instanceof Player player) || args.length < 3) {
                return true;
            }
            UUID shopId = parseUuid(args[1]);
            Shop shop = shopId == null ? null : plugin.getShopManager().getShop(shopId);
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                return true;
            }
            if (!canManageShop(player, shop)) {
                plugin.getMessageManager().send(player, "general.no-permission");
                return true;
            }
            ShopMode mode = ShopMode.fromInput(args[2]);
            if (mode == null) {
                plugin.getMessageManager().send(player, "shop.invalid-mode");
                return true;
            }
            plugin.getShopManager().updateMode(shop, mode);
            plugin.getPendingActions().remove(player.getUniqueId());
            plugin.getMessageManager().send(player, "owner.mode-updated", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
            return true;
        }

        if (args[0].equalsIgnoreCase("openchest")) {
            if (!(sender instanceof Player player) || args.length < 2) {
                return true;
            }
            UUID shopId = parseUuid(args[1]);
            Shop shop = shopId == null ? null : plugin.getShopManager().getShop(shopId);
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                return true;
            }
            if (!canManageShop(player, shop)) {
                plugin.getMessageManager().send(player, "general.no-permission");
                return true;
            }
            Location chestLocation = shop.getChestLocation();
            if (chestLocation != null && chestLocation.getBlock().getState() instanceof org.bukkit.block.Chest chest) {
                player.openInventory(chest.getBlockInventory());
                plugin.getMessageManager().send(player, "restock.opened");
            } else {
                plugin.getMessageManager().send(player, "general.chest-not-found");
            }
            return true;
        }

        return false;
    }

    private UUID parseUuid(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("help", "list", "reload", "remove", "editprice", "editbuyprice", "editsellprice", "setprice", "setbuyprice", "setsellprice", "setmode"), args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String token) {
        if (token == null || token.isEmpty()) {
            return values;
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(token.toLowerCase(Locale.ROOT))) {
                result.add(value);
            }
        }
        return result;
    }

    private boolean canManageShop(Player player, Shop shop) {
        return shop.getOwnerUuid().equals(player.getUniqueId()) || player.hasPermission("chestshop.admin");
    }
}