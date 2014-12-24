/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.bukkit.plugins.dailychests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class DailyChest extends JavaPlugin {

    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("prefix", "[DailyChests by ferrybig]") + message));
    }

    private BukkitRunnable task;

    private void refreshTask() {
        this.task = this.createTask();
    }

    public void scheduleSave() {
        if (this.task == null) {
            refreshTask();
            this.task.runTaskLater(this, 6000);
        }
    }

    private BukkitRunnable createTask() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                task = null;
                save();

            }
        };
    }

    private void save() {
        this.saveConfig();
        this.getLogger().info("AutoSaved!");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.<String>emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.testPermission(sender)) {
            return true;
        }
        switch (command.getName()) {
            case "addChest": {

            }
            break;
            case "changeChestItems": {

            }
            break;
            case "removeChest": {

            }
            break;
            case "listChests": {
                List<String> toRemove = new ArrayList<>();
                for (String str : this.getConfig().getConfigurationSection("chests").getKeys(false)) {
                    BlockLocation chest = BlockLocation.parseLocation(str);
                    if(chest == null) {
                        toRemove.add(str);
                        continue;
                    }
                    sender.sendMessage(str);
                }
                for(String remove : toRemove) {
                    this.getConfig().getConfigurationSection("chests").set(remove,null);
                }
            }
            break;
            default: {
                sender.sendMessage("Unknown command: " + command.getName());
            }
            return true;
        }
        return false;
    }
}
