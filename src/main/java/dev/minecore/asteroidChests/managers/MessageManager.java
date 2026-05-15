package dev.minecore.asteroidChests.managers;

import dev.minecore.asteroidChests.ChestShopPlugin;
import dev.minecore.asteroidChests.models.PendingAction;
import dev.minecore.asteroidChests.models.PendingActionType;
import dev.minecore.asteroidChests.models.Shop;
import dev.minecore.asteroidChests.models.ShopMode;
import dev.minecore.asteroidChests.models.TransactionType;
import dev.minecore.asteroidChests.utils.ColorUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class MessageManager {

    private final ChestShopPlugin plugin;
    private final File file;
    private FileConfiguration messages;

    public MessageManager(ChestShopPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(format(key));
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(format(key, placeholders));
    }

    public void sendActionBar(Player player, String key, Map<String, String> placeholders) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(format(key, placeholders)));
    }

    public void sendHelp(CommandSender sender) {
        sender.sendMessage(format("help.header"));
        sender.sendMessage(format("help.help-line"));
        sender.sendMessage(format("help.list-line"));
        sender.sendMessage(format("help.reload-line"));
        sender.sendMessage(format("help.remove-line"));
        sender.sendMessage(format("help.edit-buy-price-line"));
        sender.sendMessage(format("help.edit-sell-price-line"));
    }

    public void sendShopList(Player player, List<Shop> shops) {
        if (shops.isEmpty()) {
            player.sendMessage(format("list.empty"));
            return;
        }
        player.sendMessage(format("list.header", Map.of("count", String.valueOf(shops.size()))));
        for (Shop shop : shops) {
            player.sendMessage(format("list.entry", placeholders(shop, null, null, null, null, player, null, null, null)));
        }
    }

    public void sendPlayerMenu(Player player, Shop shop) {
        Map<String, String> placeholders = placeholders(shop, null, null, null, null, player, null, null, null);
        player.sendMessage(format("menu.player.header", placeholders));
        player.sendMessage(format("menu.player.item", placeholders));
        player.sendMessage(format("menu.player.owner", placeholders));
        player.sendMessage(format("menu.player.price", placeholders));
        if (shop.getMode() == ShopMode.BUY || shop.getMode() == ShopMode.BOTH) {
            player.sendMessage(format("menu.player.stock", placeholders));
        }
        if (shop.getMode() == ShopMode.SELL || shop.getMode() == ShopMode.BOTH) {
            player.sendMessage(format("menu.player.space", placeholders));
        }

        if (shop.getMode() == ShopMode.BUY) {
            promptTransactionAmount(player, shop, TransactionType.BUY);
            return;
        }
        if (shop.getMode() == ShopMode.SELL) {
            promptTransactionAmount(player, shop, TransactionType.SELL);
            return;
        }

        List<BaseComponent[]> buttons = new ArrayList<>();
        buttons.add(button("button.buy.label", "button.buy.hover", "/chestshop transact buy " + shop.getId(), placeholders));
        buttons.add(button("button.sell.label", "button.sell.hover", "/chestshop transact sell " + shop.getId(), placeholders));
        player.spigot().sendMessage(joinButtons(buttons));
    }

    public void promptTransactionAmount(Player player, Shop shop, TransactionType transactionType) {
        PendingAction pendingAction = new PendingAction(PendingActionType.TRANSACTION_AMOUNT);
        pendingAction.setShopId(shop.getId());
        pendingAction.setTransactionType(transactionType);
        plugin.getPendingActions().put(player.getUniqueId(), pendingAction);
        send(player, "shop.amount-prompt", placeholders(shop, null, null, null, null, player, null, null, null));
    }

    public void sendOwnerMenu(Player player, Shop shop) {
        Map<String, String> placeholders = placeholders(shop, null, null, null, null, player, null, null, null);
        player.sendMessage(format("menu.owner.header", placeholders));
        player.sendMessage(format("menu.owner.item", placeholders));
        player.sendMessage(format("menu.owner.mode", placeholders));
        player.sendMessage(format("menu.owner.price", placeholders));
        player.sendMessage(format("menu.owner.instructions", placeholders));
        if (shop.getMode() == ShopMode.BOTH) {
            player.spigot().sendMessage(joinButtons(Arrays.asList(
                button("button.open-chest.label", "button.open-chest.hover", "/chestshop openchest " + shop.getId(), placeholders),
                button("button.edit-buy-price.label", "button.edit-buy-price.hover", "/chestshop editbuyprice " + shop.getId(), placeholders),
                button("button.edit-sell-price.label", "button.edit-sell-price.hover", "/chestshop editsellprice " + shop.getId(), placeholders),
                button("button.edit-mode.label", "button.edit-mode.hover", "/chestshop editmode " + shop.getId(), placeholders),
                button("button.delete.label", "button.delete.hover", "/chestshop delete " + shop.getId(), placeholders)
            )));
            return;
        }
        player.spigot().sendMessage(joinButtons(Arrays.asList(
            button("button.open-chest.label", "button.open-chest.hover", "/chestshop openchest " + shop.getId(), placeholders),
            button("button.edit-price.label", "button.edit-price.hover", "/chestshop editprice " + shop.getId(), placeholders),
            button("button.edit-mode.label", "button.edit-mode.hover", "/chestshop editmode " + shop.getId(), placeholders),
            button("button.delete.label", "button.delete.hover", "/chestshop delete " + shop.getId(), placeholders)
        )));
    }

    public void sendDeleteConfirm(Player player, Shop shop) {
        Map<String, String> placeholders = placeholders(shop, null, null, null, null, player, null, null, null);
        player.sendMessage(format("menu.delete.confirm", placeholders));
        player.spigot().sendMessage(joinButtons(Arrays.asList(
                button("button.confirm.label", "button.confirm.hover", "/chestshop deleteconfirm " + shop.getId(), placeholders),
                button("button.cancel.label", "button.cancel.hover", "/chestshop deletecancel " + shop.getId(), placeholders)
        )));
    }

    public String format(String key) {
        return format(key, new HashMap<>());
    }

    public String format(String key, Map<String, String> placeholders) {
        if (placeholders == null) {
            placeholders = new HashMap<>();
        }
        if (!placeholders.containsKey("prefix")) {
            placeholders.put("prefix", messages.getString("prefix", ""));
        }
        String value = resolve(key);
        return ColorUtils.color(applyPlaceholders(value, placeholders));
    }

    public String formatPlain(String key, Map<String, String> placeholders) {
        if (placeholders == null) {
            placeholders = new HashMap<>();
        }
        if (!placeholders.containsKey("prefix")) {
            placeholders.put("prefix", messages.getString("prefix", ""));
        }
        String value = resolve(key);
        return ColorUtils.color(applyPlaceholders(value, placeholders));
    }

    public BaseComponent[] button(String labelKey, String hoverKey, String command, Map<String, String> placeholders) {
        BaseComponent[] components = TextComponent.fromLegacyText(formatPlain(labelKey, placeholders));
        BaseComponent[] hover = TextComponent.fromLegacyText(formatPlain(hoverKey, placeholders));
        for (BaseComponent component : components) {
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
        }
        return components;
    }

    public BaseComponent[] joinButtons(List<BaseComponent[]> buttons) {
        List<BaseComponent> result = new ArrayList<>();
        for (int index = 0; index < buttons.size(); index++) {
            if (index > 0) {
                result.add(new TextComponent(" "));
            }
            result.addAll(Arrays.asList(buttons.get(index)));
        }
        return result.toArray(new BaseComponent[0]);
    }

    public Map<String, String> placeholders(Shop shop, String item, String price, String mode, String stock, Player player, String owner, String amount, String total) {
        return placeholders(shop, item, price, mode, stock, player, owner, amount, total, null);
    }

    public Map<String, String> placeholders(Shop shop, String item, String price, String mode, String stock, Player player, String owner, String amount, String total, String unitPrice) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("prefix", messages.getString("prefix", ""));
        values.put("item", item == null ? (shop == null ? "" : shop.getDisplayName()) : item);
        values.put("price", price == null ? (shop == null ? "" : String.valueOf(shop.getPrice())) : price);
        values.put("buy-price", shop == null ? "" : String.valueOf(shop.getBuyPrice()));
        values.put("sell-price", shop == null ? "" : String.valueOf(shop.getSellPrice()));
        values.put("price-display", buildPriceDisplay(shop));
        values.put("mode", mode == null ? (shop == null ? "" : shop.getMode().name()) : mode);
        values.put("stock", stock == null ? (shop == null ? "0" : String.valueOf(plugin.getShopManager().getStock(shop))) : stock);
        values.put("space", shop == null ? "0" : String.valueOf(plugin.getShopManager().getSpace(shop)));
        values.put("player", player == null ? "" : player.getName());
        values.put("owner", owner == null ? (shop == null ? "" : shop.getOwnerName()) : owner);
        values.put("amount", amount == null ? "" : amount);
        values.put("total", total == null ? "" : total);
        values.put("unit-price", unitPrice == null ? (shop == null ? "" : String.valueOf(shop.getPrice())) : unitPrice);
        values.put("currency", plugin.getConfig().getString("currency-symbol", "$"));
        values.put("world", shop == null ? "" : shop.getWorld());
        values.put("x", shop == null ? "" : String.valueOf(shop.getX()));
        values.put("y", shop == null ? "" : String.valueOf(shop.getY()));
        values.put("z", shop == null ? "" : String.valueOf(shop.getZ()));
        return values;
    }

    private String buildPriceDisplay(Shop shop) {
        if (shop == null) {
            return "";
        }
        String currency = plugin.getConfig().getString("currency-symbol", "$");
        if (shop.getMode() == ShopMode.BOTH) {
            return "Buy " + shop.getBuyPrice() + currency + " / Sell " + shop.getSellPrice() + currency;
        }
        return shop.getPrice() + currency;
    }

    private String resolve(String key) {
        String value = messages.getString(key);
        return value == null ? key : value;
    }

    private String applyPlaceholders(String value, Map<String, String> placeholders) {
        String result = value;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }
}