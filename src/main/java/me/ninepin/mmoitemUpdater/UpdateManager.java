package me.ninepin.mmoitemUpdater;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.ReforgeOptions;
import net.Indyuce.mmoitems.api.util.MMOItemReforger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * MMOItems 物品更新管理器
 * 負責自動檢查和更新玩家背包中的所有 MMOItems 到最新版本
 *
 * @author YourName
 */
public class UpdateManager {

    private final MmoitemUpdater plugin;

    // 玩家冷卻時間追蹤（UUID -> 上次檢查時間）
    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();

    // 玩家背包雜湊值追蹤（UUID -> 背包雜湊值）
    private final Map<UUID, Integer> playerInventoryHashes = new ConcurrentHashMap<>();

    // 輪詢索引，確保所有玩家都會被檢查到
    private int playerIndex = 0;

    // 統計資料
    private long totalChecks = 0;
    private long totalUpdates = 0;
    private long lastResetTime = System.currentTimeMillis();

    public UpdateManager(MmoitemUpdater plugin) {
        this.plugin = plugin;
    }

    /**
     * 執行定時更新（性能優化版本）
     * 每秒調用一次，智能檢查玩家背包
     */
    public void performScheduledUpdate() {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        if (onlinePlayers.isEmpty()) {
            return;
        }

        List<Player> players = new ArrayList<>(onlinePlayers);
        int maxPlayersPerTick = plugin.getConfig().getInt("forced-item-update.performance.max-players-per-tick", 3);
        boolean smartCheck = plugin.getConfig().getBoolean("forced-item-update.performance.smart-check", true);
        long playerCooldownMs = plugin.getConfig().getLong("forced-item-update.performance.player-cooldown", 5) * 1000L;

        int playersChecked = 0;
        long currentTime = System.currentTimeMillis();

        // 輪詢檢查玩家，避免每次都從同一個玩家開始
        for (int i = 0; i < players.size() && playersChecked < maxPlayersPerTick; i++) {
            Player player = players.get((playerIndex + i) % players.size());

            try {
                // 檢查玩家是否在線且有效
                if (!player.isOnline() || !player.isValid()) {
                    continue;
                }

                // 檢查冷卻時間
                Long lastCheck = playerCooldowns.get(player.getUniqueId());
                if (lastCheck != null && (currentTime - lastCheck) < playerCooldownMs) {
                    continue;
                }

                // 檢查世界白名單
                List<String> whitelistWorlds = plugin.getConfig().getStringList("forced-item-update.whitelist-worlds");
                if (!whitelistWorlds.isEmpty() && !whitelistWorlds.contains(player.getWorld().getName())) {
                    continue;
                }

                // 智能檢查：比較背包雜湊值
                if (smartCheck) {
                    int currentHash = calculateInventoryHash(player);
                    Integer lastHash = playerInventoryHashes.get(player.getUniqueId());
                    if (lastHash != null && lastHash == currentHash) {
                        // 背包沒有變化，但還是要更新冷卻時間（降低檢查頻率）
                        playerCooldowns.put(player.getUniqueId(), currentTime);
                        continue;
                    }
                    playerInventoryHashes.put(player.getUniqueId(), currentHash);
                }

                // 執行更新檢查（在主線程中執行）
                final Player finalPlayer = player;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        int updatedCount = updatePlayerInventory(finalPlayer, false);
                        if (updatedCount > 0) {
                            totalUpdates += updatedCount;
                        }
                        totalChecks++;
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error updating player inventory for " + finalPlayer.getName(), e);
                    }
                });

                // 更新冷卻時間
                playerCooldowns.put(player.getUniqueId(), currentTime);
                playersChecked++;

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error in scheduled update for player " + player.getName(), e);
            }
        }

        // 更新輪詢索引
        playerIndex = (playerIndex + maxPlayersPerTick) % Math.max(players.size(), 1);
    }

    /**
     * 計算玩家背包的雜湊值（用於智能檢查）
     * 當背包內容有變化時，雜湊值會改變
     */
    private int calculateInventoryHash(Player player) {
        int hash = 0;

        try {
            // 計算主背包
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item != null && !item.getType().isAir()) {
                    // 使用位置、類型、數量和NBT來計算雜湊值
                    hash += (i * 31) + item.getType().hashCode() + (item.getAmount() * 7);
                    if (item.hasItemMeta()) {
                        hash += item.getItemMeta().hashCode();
                    }
                }
            }

            // 計算裝備欄
            ItemStack[] armor = player.getInventory().getArmorContents();
            for (int i = 0; i < armor.length; i++) {
                ItemStack item = armor[i];
                if (item != null && !item.getType().isAir()) {
                    hash += ((i + 100) * 31) + item.getType().hashCode() + (item.getAmount() * 7);
                    if (item.hasItemMeta()) {
                        hash += item.getItemMeta().hashCode();
                    }
                }
            }

            // 計算副手
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (offHand != null && !offHand.getType().isAir()) {
                hash += 200 * 31 + offHand.getType().hashCode() + (offHand.getAmount() * 7);
                if (offHand.hasItemMeta()) {
                    hash += offHand.getItemMeta().hashCode();
                }
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error calculating inventory hash for " + player.getName(), e);
            return 0;
        }

        return hash;
    }

    /**
     * 完整更新玩家背包（顯示訊息）
     */
    public int updatePlayerInventory(Player player) {
        return updatePlayerInventory(player, true);
    }

    /**
     * 靜默更新玩家背包（不顯示訊息）
     */
    public void updatePlayerInventoryQuietly(Player player) {
        // 在主線程中執行更新
        new BukkitRunnable() {
            @Override
            public void run() {
                updatePlayerInventory(player, false);
            }
        }.runTask(plugin);
    }

    /**
     * 更新玩家背包的核心方法
     *
     * @param player      要更新的玩家
     * @param showMessage 是否顯示更新訊息
     * @return 更新的物品數量
     */
    private int updatePlayerInventory(Player player, boolean showMessage) {
        try {
            // 檢查玩家狀態
            if (!player.isOnline() || !player.isValid()) {
                return 0;
            }

            // 檢查世界白名單
            List<String> whitelistWorlds = plugin.getConfig().getStringList("forced-item-update.whitelist-worlds");
            if (!whitelistWorlds.isEmpty() && !whitelistWorlds.contains(player.getWorld().getName())) {
                return 0;
            }

            int updatedCount = 0;
            ReforgeOptions options = MMOItems.plugin.getLanguage().revisionOptions;
            List<String> blacklistItems = plugin.getConfig().getStringList("forced-item-update.blacklist-items");

            // 更新完整背包（所有40格）
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                ItemStack updated = updateSingleItem(item, player, options, blacklistItems);
                if (updated != null) {
                    player.getInventory().setItem(i, updated);
                    updatedCount++;
                }
            }

            // 更新裝備欄位
            updatedCount += updateArmorSlots(player, options, blacklistItems);

            // 更新副手
            ItemStack offHand = player.getInventory().getItemInOffHand();
            ItemStack updatedOffHand = updateSingleItem(offHand, player, options, blacklistItems);
            if (updatedOffHand != null) {
                player.getInventory().setItemInOffHand(updatedOffHand);
                updatedCount++;
            }

            // 更新背包顯示
            if (updatedCount > 0) {
                player.updateInventory();

                // 顯示更新訊息（根據最小數量設定）
                if (showMessage && plugin.getConfig().getBoolean("forced-item-update.notifications.enabled", true)) {
                    int minimumCount = plugin.getConfig().getInt("forced-item-update.notifications.minimum-count-to-show", 3);
                    if (updatedCount >= minimumCount) {
                        String message = plugin.getConfig().getString("forced-item-update.notifications.message",
                                "&a系統自動更新了你的 {count} 個物品到最新版本！");
                        message = ChatColor.translateAlternateColorCodes('&',
                                message.replace("{count}", String.valueOf(updatedCount)));
                        player.sendMessage(message);
                    }
                }
            }

            return updatedCount;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating inventory for player " + player.getName(), e);
            return 0;
        }
    }

    /**
     * 更新單個物品
     *
     * @param item           要檢查的物品
     * @param player         物品所屬的玩家
     * @param options        重鑄選項
     * @param blacklistItems 黑名單物品列表
     * @return 更新後的物品，如果不需要更新則返回 null
     */
    private ItemStack updateSingleItem(ItemStack item, Player player, ReforgeOptions options, List<String> blacklistItems) {
        try {
            if (item == null || item.getType().isAir()) {
                return null;
            }

            NBTItem nbtItem = NBTItem.get(item);
            if (!nbtItem.hasType()) {
                return null; // 不是 MMOItem
            }

            // 檢查黑名單
            String itemId = nbtItem.getString("MMOITEMS_ITEM_ID");
            if (blacklistItems.contains(itemId)) {
                return null;
            }

            // 檢查是否為 VANILLA 物品（GooP Converter 產生的）
            if ("VANILLA".equals(itemId)) {
                return null;
            }

            MMOItemReforger reforger = new MMOItemReforger(item);

            if (!reforger.hasTemplate()) {
                return null;
            }

            // 檢查是否需要更新
            int templateRevision = reforger.getTemplate().getRevisionId();
            int itemRevision = nbtItem.hasTag(ItemStats.REVISION_ID.getNBTPath()) ?
                    nbtItem.getInteger(ItemStats.REVISION_ID.getNBTPath()) : 1;

            if (templateRevision <= itemRevision) {
                return null; // 不需要更新
            }

            // 執行更新
            if (reforger.reforge(options, player)) {
                ItemStack updatedItem = reforger.getResult();

                if (updatedItem != null) {
                    // 保持原本的數量
                    updatedItem.setAmount(item.getAmount());

                    // 處理額外掉落物（如寶石等）
                    for (ItemStack extraItem : reforger.getReforgingOutput()) {
                        // 嘗試放入背包，放不下就掉落到地面
                        player.getInventory().addItem(extraItem).values().forEach(drop ->
                                player.getWorld().dropItem(player.getLocation(), drop)
                        );
                    }

                    return updatedItem;
                }
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error updating single item for player " + player.getName(), e);
        }

        return null;
    }

    /**
     * 更新裝備欄位
     *
     * @param player         玩家
     * @param options        重鑄選項
     * @param blacklistItems 黑名單物品
     * @return 更新的物品數量
     */
    private int updateArmorSlots(Player player, ReforgeOptions options, List<String> blacklistItems) {
        try {
            int updatedCount = 0;
            ItemStack[] armorContents = player.getInventory().getArmorContents();

            for (int i = 0; i < armorContents.length; i++) {
                ItemStack armor = armorContents[i];
                ItemStack updated = updateSingleItem(armor, player, options, blacklistItems);
                if (updated != null) {
                    armorContents[i] = updated;
                    updatedCount++;
                }
            }

            if (updatedCount > 0) {
                player.getInventory().setArmorContents(armorContents);
            }

            return updatedCount;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error updating armor slots for player " + player.getName(), e);
            return 0;
        }
    }

    /**
     * 強制更新指定玩家的所有物品（無視冷卻時間和智能檢查）
     *
     * @param player 要更新的玩家
     * @return 更新的物品數量
     */
    public int forceUpdatePlayer(Player player) {
        // 清除該玩家的冷卻時間和雜湊值，強制執行完整檢查
        playerCooldowns.remove(player.getUniqueId());
        playerInventoryHashes.remove(player.getUniqueId());

        return updatePlayerInventory(player, true);
    }

    /**
     * 清理離線玩家的數據以節省記憶體
     *
     * @param playerUUID 離線玩家的 UUID
     */
    public void cleanupPlayerData(UUID playerUUID) {
        playerCooldowns.remove(playerUUID);
        playerInventoryHashes.remove(playerUUID);
    }

    /**
     * 清理所有離線玩家的數據
     */
    public void cleanupOfflinePlayersData() {
        Set<UUID> onlinePlayerUUIDs = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            onlinePlayerUUIDs.add(player.getUniqueId());
        }

        // 移除離線玩家的數據
        playerCooldowns.entrySet().removeIf(entry -> !onlinePlayerUUIDs.contains(entry.getKey()));
        playerInventoryHashes.entrySet().removeIf(entry -> !onlinePlayerUUIDs.contains(entry.getKey()));
    }

    /**
     * 獲取統計資訊
     *
     * @return 統計資訊字符串
     */
    public String getStatistics() {
        long currentTime = System.currentTimeMillis();
        long timeSinceReset = (currentTime - lastResetTime) / 1000; // 秒

        return String.format(
                "§6=== MMOItems 更新器統計 ===\n" +
                        "§e總檢查次數: §f%d\n" +
                        "§e總更新物品: §f%d\n" +
                        "§e運行時間: §f%d 秒\n" +
                        "§e在線玩家追蹤: §f%d\n" +
                        "§e平均檢查頻率: §f%.2f 次/秒",
                totalChecks,
                totalUpdates,
                timeSinceReset,
                playerCooldowns.size(),
                timeSinceReset > 0 ? (double) totalChecks / timeSinceReset : 0.0
        );
    }

    /**
     * 重置統計資料
     */
    public void resetStatistics() {
        totalChecks = 0;
        totalUpdates = 0;
        lastResetTime = System.currentTimeMillis();
    }

    /**
     * 獲取當前追蹤的玩家數量
     *
     * @return 追蹤的玩家數量
     */
    public int getTrackedPlayersCount() {
        return playerCooldowns.size();
    }

    /**
     * 檢查指定玩家是否在冷卻中
     *
     * @param player 要檢查的玩家
     * @return 是否在冷卻中
     */
    public boolean isPlayerOnCooldown(Player player) {
        Long lastCheck = playerCooldowns.get(player.getUniqueId());
        if (lastCheck == null) {
            return false;
        }

        long playerCooldownMs = plugin.getConfig().getLong("forced-item-update.performance.player-cooldown", 5) * 1000L;
        return (System.currentTimeMillis() - lastCheck) < playerCooldownMs;
    }
}