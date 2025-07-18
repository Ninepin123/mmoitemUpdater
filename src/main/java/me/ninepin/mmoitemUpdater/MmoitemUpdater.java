package me.ninepin.mmoitemUpdater;

import org.bukkit.plugin.java.JavaPlugin;

public final class MmoitemUpdater extends JavaPlugin {

    @Override
    public void onEnable() {
        // 檢查 MMOItems 依賴
        if (!getServer().getPluginManager().isPluginEnabled("MMOItems")) {
            getLogger().severe("MMOItems not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 註冊指令
        getCommand("updateitems").setExecutor(new UpdateCommand());

        getLogger().info("MMOItemsUpdater enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("MMOItemsUpdater disabled!");
    }
}
