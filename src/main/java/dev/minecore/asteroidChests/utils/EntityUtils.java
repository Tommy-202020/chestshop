package dev.minecore.asteroidChests.utils;

import dev.minecore.asteroidChests.ChestShopPlugin;
import dev.minecore.asteroidChests.models.Shop;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class EntityUtils {

    private EntityUtils() {
    }

    public static boolean spawnFloatingEntity(ChestShopPlugin plugin, Shop shop, double heightOffset) {
        removeFloatingEntity(plugin, shop);

        Location location = shop.getChestLocation();
        if (location == null || location.getWorld() == null) {
            return false;
        }
        Location spawnLocation = location.clone().add(0.5D, heightOffset, 0.5D);

        if (isItemDisplayAvailable()) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Entity> displayClass = (Class<? extends Entity>) Class.forName("org.bukkit.entity.ItemDisplay");
                Entity entity = location.getWorld().spawn(spawnLocation, displayClass, spawned -> {
                    try {
                        Method setItemStack = spawned.getClass().getMethod("setItemStack", ItemStack.class);
                        setItemStack.invoke(spawned, shop.getItem());
                        
                        // Set scale to be item-sized (smaller)
                        Method setScale = spawned.getClass().getMethod("setDisplayHeight", float.class);
                        setScale.invoke(spawned, 0.5f);
                        
                        spawned.setPersistent(false);
                        spawned.setInvulnerable(true);
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                });
                shop.setEntityUuid(entity.getUniqueId());
                plugin.getShopManager().saveAll();
                
                // Start rotation task for ItemDisplay
                startItemDisplayRotation(plugin, entity);
                
                return true;
            } catch (Exception exception) {
                plugin.getLogger().warning("ItemDisplay spawn failed, using ArmorStand instead: " + exception.getMessage());
            }
        }

        ArmorStand stand = location.getWorld().spawn(spawnLocation, ArmorStand.class, armorStand -> {
            armorStand.setInvisible(true);
            armorStand.setGravity(false);
            armorStand.setMarker(true);
            armorStand.setSmall(true);
            armorStand.setArms(false);
            armorStand.setBasePlate(false);
            armorStand.setInvulnerable(true);
            armorStand.setPersistent(false);
            armorStand.setSilent(true);
            armorStand.setCollidable(false);
            EntityEquipment equipment = armorStand.getEquipment();
            if (equipment != null) {
                equipment.setHelmet(shop.getItem());
            }
        });
        shop.setEntityUuid(stand.getUniqueId());
        plugin.getShopManager().saveAll();
        
        // Start rotation task for ArmorStand
        startArmorStandRotation(plugin, stand);
        
        return true;
    }
    
    private static void startItemDisplayRotation(ChestShopPlugin plugin, Entity entity) {
        try {
            new BukkitRunnable() {
                private float rotation = 0;
                
                @Override
                public void run() {
                    if (entity == null || entity.isDead()) {
                        cancel();
                        return;
                    }
                    
                    rotation += 2.5f;
                    if (rotation >= 360) {
                        rotation = 0;
                    }
                    
                    try {
                        Quaternionf quaternion = new Quaternionf().rotateY((float) Math.toRadians(rotation));
                        Transformation transformation = new Transformation(
                            new Vector3f(0, 0, 0),
                            quaternion,
                            new Vector3f(0.5f, 0.5f, 0.5f),
                            new Quaternionf()
                        );
                        Method setTransformation = entity.getClass().getMethod("setTransformation", Transformation.class);
                        setTransformation.invoke(entity, transformation);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to set ItemDisplay transformation: " + e.getMessage());
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 2);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to start ItemDisplay rotation: " + e.getMessage());
        }
    }
    
    private static void startArmorStandRotation(ChestShopPlugin plugin, ArmorStand stand) {
        new BukkitRunnable() {
            private float yaw = 0;
            
            @Override
            public void run() {
                if (stand == null || stand.isDead()) {
                    cancel();
                    return;
                }
                
                yaw += 5;
                if (yaw >= 360) {
                    yaw = 0;
                }
                
                Location location = stand.getLocation();
                location.setYaw(yaw);
                stand.teleport(location);
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    public static boolean hasFloatingEntity(ChestShopPlugin plugin, Shop shop) {
        if (shop.getEntityUuid() == null) {
            return false;
        }
        Entity entity = Bukkit.getEntity(shop.getEntityUuid());
        return entity != null && !entity.isDead();
    }

    public static void removeFloatingEntity(ChestShopPlugin plugin, Shop shop) {
        if (shop.getEntityUuid() == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(shop.getEntityUuid());
        if (entity != null) {
            entity.remove();
        }
        shop.setEntityUuid(null);
        plugin.getShopManager().saveAll();
    }

    private static boolean isItemDisplayAvailable() {
        try {
            Class.forName("org.bukkit.entity.ItemDisplay");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}