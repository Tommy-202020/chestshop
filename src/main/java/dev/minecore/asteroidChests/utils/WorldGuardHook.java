package dev.minecore.asteroidChests.utils;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldGuardHook {

    private final JavaPlugin plugin;
    private boolean hooked;
    private Object regionContainer;
    private Class<?> worldClass;
    private Class<?> regionManagerClass;
    private Class<?> blockVector3Class;
    private Class<?> bukkitAdapterClass;

    public WorldGuardHook(JavaPlugin plugin) {
        this.plugin = plugin;
        hook();
    }

    private void hook() {
        try {
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
                hooked = false;
                return;
            }

            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Method getInstance = worldGuardClass.getMethod("getInstance");
            Object worldGuard = getInstance.invoke(null);

            Method getPlatform = worldGuard.getClass().getMethod("getPlatform");
            Object platform = getPlatform.invoke(worldGuard);

            Method getRegionContainer = platform.getClass().getMethod("getRegionContainer");
            regionContainer = getRegionContainer.invoke(platform);

            worldClass = Class.forName("com.sk89q.worldedit.world.World");
            regionManagerClass = Class.forName("com.sk89q.worldguard.protection.managers.RegionManager");
            blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");

            hooked = true;
        } catch (Exception exception) {
            hooked = false;
            plugin.getLogger().warning("WorldGuard detected but could not be hooked: " + exception.getMessage());
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    public boolean isInProtectedRegion(Location location) {
        if (!hooked || location == null || location.getWorld() == null) {
            return false;
        }

        try {
            Method adapt = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class);
            Object world = adapt.invoke(null, location.getWorld());

            Method getRegionManager = regionContainer.getClass().getMethod("get", worldClass);
            Object regionManager = getRegionManager.invoke(regionContainer, world);
            if (regionManager == null) {
                return false;
            }

            Method at = blockVector3Class.getMethod("at", int.class, int.class, int.class);
            Object vector = at.invoke(null, location.getBlockX(), location.getBlockY(), location.getBlockZ());

            Method getApplicableRegions = regionManagerClass.getMethod("getApplicableRegions", blockVector3Class);
            Object queryResult = getApplicableRegions.invoke(regionManager, vector);

            Method getRegions = queryResult.getClass().getMethod("getRegions");
            Object regions = getRegions.invoke(queryResult);

            Method isEmpty = regions.getClass().getMethod("isEmpty");
            return !((Boolean) isEmpty.invoke(regions));
        } catch (Exception exception) {
            return false;
        }
    }
}