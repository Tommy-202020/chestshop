package dev.minecore.asteroidChests.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultHook {

    private final JavaPlugin plugin;
    private Object economy;
    private Class<?> economyClass;

    public VaultHook(JavaPlugin plugin) {
        this.plugin = plugin;
        hook();
    }

    private void hook() {
        try {
            this.economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration((Class) economyClass);
            if (provider != null) {
                this.economy = provider.getProvider();
            }
        } catch (Exception exception) {
            this.economy = null;
            this.economyClass = null;
        }
    }

    public boolean isHooked() {
        return economy != null;
    }

    public boolean hasBalance(Player player, double amount) {
        return hasBalance((OfflinePlayer) player, amount);
    }

    public boolean hasBalance(UUID uuid, double amount) {
        return hasBalance(Bukkit.getOfflinePlayer(uuid), amount);
    }

    public boolean hasBalance(OfflinePlayer player, double amount) {
        if (!isHooked()) {
            return false;
        }
        try {
            Method method = economyClass.getMethod("getBalance", OfflinePlayer.class);
            double balance = ((Number) method.invoke(economy, player)).doubleValue();
            return balance >= amount;
        } catch (Exception exception) {
            return false;
        }
    }

    public EconomyResult withdraw(Player player, double amount) {
        return withdraw((OfflinePlayer) player, amount);
    }

    public EconomyResult withdraw(UUID uuid, double amount) {
        return withdraw(Bukkit.getOfflinePlayer(uuid), amount);
    }

    public EconomyResult withdraw(OfflinePlayer player, double amount) {
        return invoke("withdrawPlayer", player, amount);
    }

    public EconomyResult deposit(UUID uuid, double amount) {
        return deposit(Bukkit.getOfflinePlayer(uuid), amount);
    }

    public EconomyResult deposit(OfflinePlayer player, double amount) {
        return invoke("depositPlayer", player, amount);
    }

    private EconomyResult invoke(String methodName, OfflinePlayer player, double amount) {
        if (!isHooked()) {
            return new EconomyResult(false, "Vault economy not available.");
        }
        try {
            Method method = economyClass.getMethod(methodName, OfflinePlayer.class, double.class);
            Object response = method.invoke(economy, player, amount);
            boolean success = extractSuccess(response);
            String error = extractError(response);
            return new EconomyResult(success, error);
        } catch (Exception exception) {
            return new EconomyResult(false, exception.getMessage());
        }
    }

    private boolean extractSuccess(Object response) {
        try {
            Method method = response.getClass().getMethod("transactionSuccess");
            return (boolean) method.invoke(response);
        } catch (Exception ignored) {
            try {
                Field field = response.getClass().getField("transactionSuccess");
                return field.getBoolean(response);
            } catch (Exception exception) {
                return false;
            }
        }
    }

    private String extractError(Object response) {
        try {
            Field field = response.getClass().getField("errorMessage");
            Object value = field.get(response);
            return value == null ? null : value.toString();
        } catch (Exception exception) {
            return null;
        }
    }

    public record EconomyResult(boolean success, String errorMessage) {
    }
}