package me.ferrybig.java.bukkit.plugins.dailychests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class DailyChest extends JavaPlugin implements Listener {

    private long maxOverTime = 0;
    private long timeBetweenChests = 0;

    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("prefix", "[DailyChests by ferrybig]") + message));
    }

    private BukkitRunnable task;

    private void refreshTask() {
        this.task = this.createTask();
    }

    @Override
    public void onEnable() {
        maxOverTime = this.getConfig().getLong("MaxOverTime", 0);
        timeBetweenChests = this.getConfig().getLong("timeBetweenChests", 24 * 60 * 60 * 1000);
        this.getServer().getPluginManager().registerEvents(this, this);
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

    @EventHandler
    public void onEvent(PlayerInteractEvent evt) {
        if (evt.getAction() == Action.RIGHT_CLICK_BLOCK) {
            BlockLocation block = new BlockLocation(evt.getClickedBlock());
            if (!this.getConfig().getConfigurationSection("chests").isConfigurationSection(block.toString())) {
                return;
            }
            Player p = evt.getPlayer();
            ConfigurationSection c = this.getConfig().getConfigurationSection("chests").getConfigurationSection(block.toString());
            boolean allowed;
            long newOverTime = 0;
            long now = System.currentTimeMillis();
            if (c.contains("players." + p.getUniqueId())) {
                long overTime = c.getLong("players." + p.getUniqueId() + "overTime", 0);
                long lastOpen = c.getLong("players." + p.getUniqueId() + "lastOpen", 0);

                long difference = lastOpen + overTime + timeBetweenChests - now;

                if (difference < 0) {
                    allowed = true;
                } else if (difference - maxOverTime < 0) {
                    newOverTime = -(difference - maxOverTime);
                    allowed = true;
                } else {
                    allowed = false;
                }
            } else {
                allowed = true;
            }
            if (allowed) {
                Inventory items = Bukkit.createInventory(p, InventoryType.CHEST, "Daily Reward @ " + block);
                if (c.contains("Items")) {
                    c.set("players." + p.getUniqueId() + "lastOpen", now);
                    if (newOverTime <= 0) {
                        c.set("players." + p.getUniqueId() + "overTime", null);
                    } else {
                        c.set("players." + p.getUniqueId() + "overTime", newOverTime);
                    }
                    List<?> itemList = c.getList("Items");
                    Random random = new Random();
                    for (int i = 0; i < Math.min(itemList.size(), 10); i++) {
                        items.all((ItemStack) itemList.get(random.nextInt(itemList.size())));
                    }
                    p.openInventory(items);
                } else {
                    p.sendMessage("This daily chest haven't been setup yet, ask the administrator of the server to set it up.");
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.<String>emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.testPermission(sender)) {
            return true;
        }
        switch (command.getName()) {
            case "addChest": {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    @SuppressWarnings("deprecation")
                    BlockLocation loc = new BlockLocation(player.getLineOfSight(null, 10).get(1));
                    if (this.getConfig().getConfigurationSection("chests").isConfigurationSection(loc.toString())) {
                        sender.sendMessage("There is already a chest defined at that location!");
                        return true;
                    }
                    if (loc.getBlock().getState() instanceof Chest) {
                        this.getConfig().getConfigurationSection("chests").createSection(loc.toString());
                        sender.sendMessage("Added chest succesfully");
                    } else {
                        sender.sendMessage("You need to look at a chest");
                        return true;
                    }
                } else {
                    sender.sendMessage("Adding chests from the console isn't supported at the time");
                }
            }
            return true;
            case "addChestItems": {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    @SuppressWarnings("deprecation")
                    BlockLocation loc = new BlockLocation(player.getLineOfSight(null, 10).get(1));
                    if (!this.getConfig().getConfigurationSection("chests").isConfigurationSection(loc.toString())) {
                        sender.sendMessage("No chest found, look at a chest!");
                        return true;
                    }
                    if (loc.getBlock().getState() instanceof Chest) {
                        ConfigurationSection c = this.getConfig().getConfigurationSection("chests").getConfigurationSection(loc.toString());
                        List<Object> items = new ArrayList<>();
                        if (c.contains("items")) {
                            items.addAll(c.getList("items"));
                        }
                        ItemStack item = player.getItemInHand();
                        if (item != null) {
                            items.add(item);
                            c.set("items", items);
                            sender.sendMessage("Added chest succesfully");
                        } else {
                            sender.sendMessage("Hold the item in hand you would like to add to the daily chest");
                        }
                    } else {
                        sender.sendMessage("You need to look at a chest");
                        return true;
                    }
                } else {
                    sender.sendMessage("Adding chests from the console isn't supported at the time");
                }
            }
            break;
            case "listChestItems": {
                if (args.length > 1) {
                    String loc = args[0];
                    if (!this.getConfig().getConfigurationSection("chests").isConfigurationSection(loc)) {
                        sender.sendMessage("There is no chest there!");
                        return true;
                    }
                    sender.sendMessage("Items inside this chest");
                    sender.sendMessage(this.getConfig().getConfigurationSection("chests").getList("items").toString());
                } else {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        @SuppressWarnings("deprecation")
                        BlockLocation loc = new BlockLocation(player.getLineOfSight(null, 10).get(1));
                        if (!this.getConfig().getConfigurationSection("chests").isConfigurationSection(loc.toString())) {
                            sender.sendMessage("There is no chest there!");
                            return true;
                        }
                        sender.sendMessage("Items inside this chest");
                        sender.sendMessage(this.getConfig().getConfigurationSection("chests").getList("items").toString());
                    } else {
                        sender.sendMessage("Adding chests from the console isn't supported at the time");
                    }
                }
            }
            break;
            case "removeChestItems": {

            }
            break;
            case "removeChest": {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    @SuppressWarnings("deprecation")
                    BlockLocation loc = new BlockLocation(player.getLineOfSight(null, 10).get(1));
                    if (!this.getConfig().getConfigurationSection("chests").isConfigurationSection(loc.toString())) {
                        sender.sendMessage("There is no chest there!");
                        return true;
                    }
                    sender.sendMessage("Removed chest at " + loc + "!");
                    this.getConfig().getConfigurationSection("chests").set(loc.toString(), null);
                } else {
                    sender.sendMessage("Adding chests from the console isn't supported at the time");
                }
            }
            return true;
            case "listChests": {
                List<String> toRemove = new ArrayList<>();
                for (String str : this.getConfig().getConfigurationSection("chests").getKeys(false)) {
                    BlockLocation chest = BlockLocation.parseLocation(str);
                    if (chest == null) {
                        toRemove.add(str);
                        continue;
                    }
                    sender.sendMessage(str);
                }
                for (String remove : toRemove) {
                    this.getConfig().getConfigurationSection("chests").set(remove, null);
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
