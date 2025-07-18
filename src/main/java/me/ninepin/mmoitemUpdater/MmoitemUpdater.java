package me.ninepin.mmoitemUpdater;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class MmoitemUpdater extends JavaPlugin {

    private static MmoitemUpdater instance;
    private UpdateManager updateManager;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        instance = this;

        // 檢查 MMOItems 依賴
        if (!getServer().getPluginManager().isPluginEnabled("MMOItems")) {
            getLogger().severe("MMOItems not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 載入配置
        saveDefaultConfig();
        config = getConfig();

        // 初始化更新管理器
        updateManager = new UpdateManager(this);

        // 註冊事件監聽器
        getServer().getPluginManager().registerEvents(new UpdateListener(updateManager), this);

        // 註冊指令
        getCommand("updateitems").setExecutor(new UpdateCommand(updateManager));

        // 啟動定時任務
        startScheduledUpdate();

        getLogger().info("MMOItemsUpdater enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("MMOItemsUpdater disabled!");
    }

    private void startScheduledUpdate() {
        if (!config.getBoolean("forced-item-update.enabled", true)) {
            return;
        }

        int interval = config.getInt("forced-item-update.schedule-interval", 1);
        boolean asyncCheck = config.getBoolean("forced-item-update.performance.async-check", true);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (asyncCheck) {
                    // 異步執行以避免阻塞主線程
                    Bukkit.getScheduler().runTaskAsynchronously(MmoitemUpdater.this, () -> {
                        updateManager.performScheduledUpdate();
                    });
                } else {
                    // 同步執行
                    updateManager.performScheduledUpdate();
                }
            }
        }.runTaskTimer(this, 20L, 20L * interval); // 改為以秒為單位
    }

    public static MmoitemUpdater getInstance() {
        return instance;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        config = getConfig();
    }
}
