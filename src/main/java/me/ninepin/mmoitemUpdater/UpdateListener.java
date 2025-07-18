package me.ninepin.mmoitemUpdater;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

public class UpdateListener implements Listener {

    private final UpdateManager updateManager;
    private final MmoitemUpdater plugin;

    public UpdateListener(UpdateManager updateManager) {
        this.updateManager = updateManager;
        this.plugin = MmoitemUpdater.getInstance();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("forced-item-update.trigger-events.join", true)) {
            return;
        }

        // 延遲2秒執行，確保玩家完全載入
        new BukkitRunnable() {
            @Override
            public void run() {
                updateManager.updatePlayerInventoryQuietly(event.getPlayer());
            }
        }.runTaskLater(plugin, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!plugin.getConfig().getBoolean("forced-item-update.trigger-events.inventory-open", true)) {
            return;
        }

        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();

            // 異步檢查，避免阻塞主線程
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateManager.updatePlayerInventoryQuietly(player);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("forced-item-update.trigger-events.item-use", true)) {
            return;
        }

        if (event.hasItem()) {
            // 異步檢查
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateManager.updatePlayerInventoryQuietly(event.getPlayer());
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfig().getBoolean("forced-item-update.trigger-events.respawn", true)) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                updateManager.updatePlayerInventoryQuietly(event.getPlayer());
            }
        }.runTaskLater(plugin, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (!plugin.getConfig().getBoolean("forced-item-update.trigger-events.world-change", true)) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                updateManager.updatePlayerInventoryQuietly(event.getPlayer());
            }
        }.runTaskLater(plugin, 20L);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 清理離線玩家的數據以節省記憶體
        updateManager.cleanupPlayerData(event.getPlayer().getUniqueId());
    }
}
