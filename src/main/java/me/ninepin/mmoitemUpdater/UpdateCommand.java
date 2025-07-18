package me.ninepin.mmoitemUpdater;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UpdateCommand implements CommandExecutor {

    private final UpdateManager updateManager;

    public UpdateCommand(UpdateManager updateManager) {
        this.updateManager = updateManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("mmoupdater.update")) {
            sender.sendMessage(ChatColor.RED + "你沒有權限使用此指令！");
            return true;
        }

        Player targetPlayer;

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "控制台必須指定玩家名稱！");
                return true;
            }
            targetPlayer = (Player) sender;
        } else {
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

        // 使用 UpdateManager 執行更新
        int updatedCount = updateManager.updatePlayerInventory(targetPlayer);

        if (updatedCount > 0) {
            sender.sendMessage(ChatColor.GREEN + "成功更新了 " + updatedCount + " 個物品！");
            if (!sender.equals(targetPlayer)) {
                targetPlayer.sendMessage(ChatColor.GREEN + "你的物品已被管理員更新到最新版本！");
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "沒有找到需要更新的物品。");
        }

        return true;
    }
}