package dev.minecore.asteroidChests.listeners;

import dev.minecore.asteroidChests.ChestShopPlugin;
import dev.minecore.asteroidChests.models.PendingAction;
import dev.minecore.asteroidChests.models.PendingActionType;
import dev.minecore.asteroidChests.models.Shop;
import dev.minecore.asteroidChests.models.ShopMode;
import dev.minecore.asteroidChests.models.TransactionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatInputListener implements Listener {

    private final ChestShopPlugin plugin;

    public ChatInputListener(ChestShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingAction action = plugin.getPendingActions().get(player.getUniqueId());
        if (action == null) {
            return;
        }

        event.setCancelled(true);
        String input = event.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> handle(player, action, input));
    }

    private void handle(Player player, PendingAction action, String input) {
        if (action.getType() == PendingActionType.CREATE_MODE) {
            ShopMode mode = ShopMode.fromInput(input);
            if (mode == null) {
                plugin.getMessageManager().send(player, "creation.invalid-mode");
                plugin.getMessageManager().send(player, "creation.mode-prompt");
                return;
            }
            action.setMode(mode);
            action.setType(PendingActionType.CREATE_PRICE);
            plugin.getMessageManager().send(player, "creation.price-prompt", plugin.getMessageManager().placeholders(null, plugin.getShopManager().getDisplayName(action.getItem()), null, null, null, player, null, null, null));
            return;
        }

        if (action.getType() == PendingActionType.CREATE_PRICE) {
            Double price = parsePositiveDouble(input);
            if (price == null) {
                plugin.getMessageManager().send(player, "creation.invalid-price");
                plugin.getMessageManager().send(player, "creation.price-prompt", plugin.getMessageManager().placeholders(null, plugin.getShopManager().getDisplayName(action.getItem()), null, null, null, player, null, null, null));
                return;
            }
            plugin.getPendingActions().remove(player.getUniqueId());
            plugin.getShopManager().createShop(player, action.getChestLocation(), action.getItem(), action.getMode(), price, action.getSignFace());
            return;
        }

        if (action.getType() == PendingActionType.TRANSACTION_AMOUNT) {
            Integer amount = parsePositiveInteger(input);
            if (amount == null) {
                plugin.getMessageManager().send(player, "shop.invalid-amount");
                Shop shop = plugin.getShopManager().getShop(action.getShopId());
                plugin.getMessageManager().send(player, "shop.amount-prompt", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
                return;
            }
            Shop shop = plugin.getShopManager().getShop(action.getShopId());
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                plugin.getPendingActions().remove(player.getUniqueId());
                return;
            }
            plugin.getPendingActions().remove(player.getUniqueId());
            TransactionType transactionType = action.getTransactionType();
            if (transactionType == null) {
                transactionType = TransactionType.BUY;
            }
            plugin.getShopManager().processTransaction(player, shop, transactionType, amount);
            return;
        }

        if (action.getType() == PendingActionType.OWNER_EDIT_PRICE) {
            Double price = parsePositiveDouble(input);
            if (price == null) {
                plugin.getMessageManager().send(player, "shop.invalid-price");
                plugin.getMessageManager().send(player, "owner.edit-price-prompt", plugin.getMessageManager().placeholders(plugin.getShopManager().getShop(action.getShopId()), null, null, null, null, player, null, null, null));
                return;
            }
            Shop shop = plugin.getShopManager().getShop(action.getShopId());
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                plugin.getPendingActions().remove(player.getUniqueId());
                return;
            }
            plugin.getShopManager().updatePrice(shop, price);
            plugin.getPendingActions().remove(player.getUniqueId());
            plugin.getMessageManager().send(player, "owner.price-updated", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
            return;
        }

        if (action.getType() == PendingActionType.OWNER_EDIT_BUY_PRICE) {
            Double price = parsePositiveDouble(input);
            if (price == null) {
                plugin.getMessageManager().send(player, "shop.invalid-price");
                plugin.getMessageManager().send(player, "owner.edit-buy-price-prompt", plugin.getMessageManager().placeholders(plugin.getShopManager().getShop(action.getShopId()), null, null, null, null, player, null, null, null));
                return;
            }
            Shop shop = plugin.getShopManager().getShop(action.getShopId());
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                plugin.getPendingActions().remove(player.getUniqueId());
                return;
            }
            plugin.getShopManager().updateBuyPrice(shop, price);
            plugin.getPendingActions().remove(player.getUniqueId());
            plugin.getMessageManager().send(player, "owner.buy-price-updated", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
            return;
        }

        if (action.getType() == PendingActionType.OWNER_EDIT_SELL_PRICE) {
            Double price = parsePositiveDouble(input);
            if (price == null) {
                plugin.getMessageManager().send(player, "shop.invalid-price");
                plugin.getMessageManager().send(player, "owner.edit-sell-price-prompt", plugin.getMessageManager().placeholders(plugin.getShopManager().getShop(action.getShopId()), null, null, null, null, player, null, null, null));
                return;
            }
            Shop shop = plugin.getShopManager().getShop(action.getShopId());
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                plugin.getPendingActions().remove(player.getUniqueId());
                return;
            }
            plugin.getShopManager().updateSellPrice(shop, price);
            plugin.getPendingActions().remove(player.getUniqueId());
            plugin.getMessageManager().send(player, "owner.sell-price-updated", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
            return;
        }

        if (action.getType() == PendingActionType.OWNER_EDIT_MODE) {
            ShopMode mode = ShopMode.fromInput(input);
            if (mode == null) {
                plugin.getMessageManager().send(player, "shop.invalid-mode");
                plugin.getMessageManager().send(player, "owner.edit-mode-prompt", plugin.getMessageManager().placeholders(plugin.getShopManager().getShop(action.getShopId()), null, null, null, null, player, null, null, null));
                return;
            }
            Shop shop = plugin.getShopManager().getShop(action.getShopId());
            if (shop == null) {
                plugin.getMessageManager().send(player, "general.shop-not-found");
                plugin.getPendingActions().remove(player.getUniqueId());
                return;
            }
            plugin.getShopManager().updateMode(shop, mode);
            plugin.getPendingActions().remove(player.getUniqueId());
            plugin.getMessageManager().send(player, "owner.mode-updated", plugin.getMessageManager().placeholders(shop, null, null, null, null, player, null, null, null));
        }
    }

    private Double parsePositiveDouble(String input) {
        try {
            double value = Double.parseDouble(input);
            return value > 0.0D ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer parsePositiveInteger(String input) {
        try {
            int value = Integer.parseInt(input);
            return value > 0 ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}