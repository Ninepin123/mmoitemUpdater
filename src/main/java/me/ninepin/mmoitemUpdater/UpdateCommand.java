package me.ninepin.mmoitemUpdater;

import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.ReforgeOptions;
import net.Indyuce.mmoitems.api.util.MMOItemReforger;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class UpdateCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // 檢查權限
        if (!sender.hasPermission("mmoupdater.update")) {
            sender.sendMessage(ChatColor.RED + "你沒有權限使用此指令！");
            return true;
        }

        Player targetPlayer;

        // 判斷目標玩家
        if (args.length == 0) {
            // 更新自己
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "控制台必須指定玩家名稱！");
                return true;
            }
            targetPlayer = (Player) sender;
        } else {
            // 更新別人
            if (!sender.hasPermission("mmoupdater.update.others")) {
                sender.sendMessage(ChatColor.RED + "你沒有權限更新其他玩家的物品！");
                return true;
            }

            targetPlayer = Bukkit.getPlayer(args[0]);
            if (targetPlayer == null) {
                sender.sendMessage(ChatColor.RED + "找不到玩家: " + args[0]);
                return true;
            }
        }

        // 執行更新
        int updatedCount = updatePlayerItems(targetPlayer);

        // 發送結果訊息
        if (updatedCount > 0) {
            sender.sendMessage(ChatColor.GREEN + "成功更新了 " + updatedCount + " 個物品！");
            if (!sender.equals(targetPlayer)) {
                targetPlayer.sendMessage(ChatColor.GREEN + "你的物品已被更新到最新版本！");
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "沒有找到需要更新的物品。");
        }

        return true;
    }

    /**
     * 更新玩家身上所有的 MMOItems
     */
    private int updatePlayerItems(Player player) {
        int updatedCount = 0;

        // 創建更新選項 - 使用 MMOItems 的預設配置
        ReforgeOptions options = MMOItems.plugin.getLanguage().revisionOptions;

        // 遍歷玩家的所有物品欄位
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];

            if (item == null || item.getType().isAir()) {
                continue;
            }

            // 檢查是否為 MMOItem
            NBTItem nbtItem = NBTItem.get(item);
            if (!nbtItem.hasType()) {
                continue; // 不是 MMOItem
            }

            // 創建重鑄器
            MMOItemReforger reforger = new MMOItemReforger(item);

            // 檢查是否可以重鑄且需要更新
            if (!reforger.hasTemplate()) {
                continue; // 沒有對應的模板
            }

            // 檢查 RevisionID 是否需要更新
            int templateRevision = reforger.getTemplate().getRevisionId();
            int itemRevision = nbtItem.hasTag(ItemStats.REVISION_ID.getNBTPath()) ?
                    nbtItem.getInteger(ItemStats.REVISION_ID.getNBTPath()) : 1;

            if (templateRevision <= itemRevision) {
                continue; // 不需要更新
            }

            // 執行更新
            if (reforger.reforge(options, player)) {
                ItemStack updatedItem = reforger.getResult();

                if (updatedItem != null) {
                    // 保持原本的數量
                    updatedItem.setAmount(item.getAmount());

                    // 替換物品
                    player.getInventory().setItem(i, updatedItem);

                    // 處理額外掉落物（如寶石）
                    for (ItemStack extraItem : reforger.getReforgingOutput()) {
                        player.getInventory().addItem(extraItem).values().forEach(drop ->
                                player.getWorld().dropItem(player.getLocation(), drop)
                        );
                    }

                    updatedCount++;
                }
            }
        }

        // 同樣處理裝備欄位
        updatedCount += updateArmorSlots(player, options);

        // 更新背包顯示
        player.updateInventory();

        return updatedCount;
    }

    /**
     * 更新裝備欄位
     */
    private int updateArmorSlots(Player player, ReforgeOptions options) {
        int updatedCount = 0;
        ItemStack[] armorContents = player.getInventory().getArmorContents();

        for (int i = 0; i < armorContents.length; i++) {
            ItemStack armor = armorContents[i];

            if (armor == null || armor.getType().isAir()) {
                continue;
            }

            NBTItem nbtItem = NBTItem.get(armor);
            if (!nbtItem.hasType()) {
                continue;
            }

            MMOItemReforger reforger = new MMOItemReforger(armor);

            if (!reforger.hasTemplate()) {
                continue;
            }

            int templateRevision = reforger.getTemplate().getRevisionId();
            int itemRevision = nbtItem.hasTag(ItemStats.REVISION_ID.getNBTPath()) ?
                    nbtItem.getInteger(ItemStats.REVISION_ID.getNBTPath()) : 1;

            if (templateRevision <= itemRevision) {
                continue;
            }

            if (reforger.reforge(options, player)) {
                ItemStack updatedArmor = reforger.getResult();

                if (updatedArmor != null) {
                    updatedArmor.setAmount(armor.getAmount());
                    armorContents[i] = updatedArmor;

                    for (ItemStack extraItem : reforger.getReforgingOutput()) {
                        player.getInventory().addItem(extraItem).values().forEach(drop ->
                                player.getWorld().dropItem(player.getLocation(), drop)
                        );
                    }

                    updatedCount++;
                }
            }
        }

        player.getInventory().setArmorContents(armorContents);
        return updatedCount;
    }
}