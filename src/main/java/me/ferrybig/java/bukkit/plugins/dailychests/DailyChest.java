package me.ferrybig.java.bukkit.plugins.dailychests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class DailyChest extends JavaPlugin implements Listener {

	private static final int MAX_ITEMS = 10;
	private static final int DEFAULT_TIME_BETWEEN_CHESTS = 24 * 60 * 60 * 1000;
	private long maxOverTime = 0;
	private long timeBetweenChests = 0;
	private final Map<UUID, Inventory> chests = new HashMap<>();
	private String prefix = null;
	private Random random = new Random();

	public void sendMessage(CommandSender sender, String message) {
		if (prefix == null) {
			prefix = this.getConfig().getString("prefix", "[DailyChests] ");
		}
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
	}

	private BukkitRunnable task;

	private void refreshTask() {
		this.task = this.createTask();
	}

	@Override
	public void onEnable() {
		maxOverTime = this.getConfig().getLong("MaxOverTime", 0);
		timeBetweenChests = this.getConfig().getLong("timeBetweenChests", DEFAULT_TIME_BETWEEN_CHESTS);
		this.getServer().getPluginManager().registerEvents(this, this);
		this.getConfig().set("prefix", this.getConfig().getString("prefix", "[DailyChests] "));
		this.getConfig().set("MaxOverTime", maxOverTime);
		this.getConfig().set("timeBetweenChests", timeBetweenChests);
		this.save();
	}

	@Override
	public void onDisable() {
		if (this.task != null) {
			this.task.cancel();
			this.task.run();
		}
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
			final String blockIdentifier = block.toString();
			final ConfigurationSection chestSection = this.getConfig().getConfigurationSection("chests");
			if (chestSection == null) {
				return;
			}
			if (!chestSection.isConfigurationSection(blockIdentifier)) {
				return;
			}
			Player p = evt.getPlayer();
			ConfigurationSection c = chestSection.getConfigurationSection(blockIdentifier);
			boolean allowed;
			long newOverTime = 0;
			long now = System.currentTimeMillis();
			if (c.contains("players." + p.getUniqueId())) {
				long overTime = c.getLong("players." + p.getUniqueId() + ".overTime", 0);
				long lastOpen = c.getLong("players." + p.getUniqueId() + ".lastOpen", 0);

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
				Inventory items = Bukkit.createInventory(p, InventoryType.CHEST, "Daily Reward @ " + c.getString("name", block.toString()));
				if (c.contains("Items")) {
					c.set("players." + p.getUniqueId() + ".lastOpen", now);
					if (newOverTime <= 0) {
						c.set("players." + p.getUniqueId() + ".overTime", null);
					} else {
						c.set("players." + p.getUniqueId() + ".overTime", newOverTime);
					}
					List<?> itemList = c.getList("Items");
					for (int i = 0; i < Math.min(itemList.size(), MAX_ITEMS); i++) {
						items.all((ItemStack) itemList.get(random.nextInt(itemList.size())));
					}
					p.openInventory(items);
					this.chests.put(p.getUniqueId(), items);
					scheduleSave();
				} else {
					p.sendMessage("This daily chest isn't been setup yet, ask the administrator off the server to set it up.");
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
		final ConfigurationSection chestSection = this.getConfig().getConfigurationSection("chests");
		switch (command.getName()) {
			case "addChest": {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					@SuppressWarnings("deprecation")
					BlockLocation loc = new BlockLocation(player.getLineOfSight((Set<Material>) null, 10).get(1));
					if (chestSection.isConfigurationSection(loc.toString())) {
						sendMessage(sender, "There is already a chest defined at that location!");
						return true;
					}
					if (loc.getBlock().getState() instanceof Chest) {
						chestSection.createSection(loc.toString());
						sendMessage(sender, "Added chest succesfully");
						scheduleSave();
					} else {
						sendMessage(sender, "You need to look at a chest");
						return true;
					}
				} else {
					sendMessage(sender, "Adding chests from the console isn't supported at the time");
				}
			}
			return true;
			case "addChestItems": {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					@SuppressWarnings("deprecation")
					BlockLocation loc = new BlockLocation(player.getLineOfSight((Set<Material>) null, 10).get(1));
					if (loc.getBlock().getState() instanceof Chest) {
						if (!chestSection.isConfigurationSection(loc.toString())) {
							sendMessage(sender, "This chest isn't a daily chest!");
							return true;
						}
						ConfigurationSection c = chestSection.getConfigurationSection(loc.toString());
						List<Object> items = new ArrayList<>();
						if (c.contains("items")) {
							items.addAll(c.getList("items"));
						}
						ItemStack item = player.getItemInHand();
						if (item != null) {
							items.add(item);
							c.set("items", items);
							sendMessage(sender, "Added chest succesfully");
							scheduleSave();
						} else {
							sendMessage(sender, "Hold the item in hand you would like to add to the daily chest");
						}
					} else {
						sendMessage(sender, "You need to look at a chest");
						return true;
					}
				} else {
					sendMessage(sender, "Adding chests from the console isn't supported at the time");
				}
			}
			break;
			case "listChestItems": {
				if (args.length > 1) {
					String loc = args[0];
					if (!chestSection.isConfigurationSection(loc)) {
						sendMessage(sender, "There is no chest there!");
						return true;
					}
					sendMessage(sender, "Items inside this chest");
					sendMessage(sender, chestSection.getList("items").toString());
				} else if (sender instanceof Player) {
					Player player = (Player) sender;
					@SuppressWarnings("deprecation")
					BlockLocation loc = new BlockLocation(player.getLineOfSight((Set<Material>) null, 10).get(1));
					if (!chestSection.isConfigurationSection(loc.toString())) {
						sendMessage(sender, "There is no chest there!");
						return true;
					}
					sendMessage(sender, "Items inside this chest");
					List<?> list = chestSection.getList("items");
					for (int i = 0; i < list.size(); i++) {
						sendMessage(sender, i + ": " + list.get(i));
					}
				} else {
					sendMessage(sender, "Listing chests from the console isn't supported at the time");
				}
			}
			break;
			case "removeChestItems": {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					@SuppressWarnings("deprecation")
					BlockLocation loc = new BlockLocation(player.getLineOfSight((Set<Material>) null, 10).get(1));
					if (!chestSection.isConfigurationSection(loc.toString())) {
						sendMessage(sender, "There is no chest there!");
						return true;
					}
					List<?> list = chestSection.getList("items");
					int number = Integer.parseInt(args[0]);
					if (list.size() >= number) {
						sendMessage(sender, "No item found at that slot");
					} else {
						list.remove(number);
						sendMessage(sender, "Removed item from the random chest rewards");
						scheduleSave();
					}
				} else {
					sendMessage(sender, "Removing chest items from the console isn't supported at the time");
				}
			}
			break;
			case "removeChest": {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					BlockLocation loc = new BlockLocation(player.getLineOfSight((Set<Material>) null, 10).get(1));
					if (!chestSection.isConfigurationSection(loc.toString())) {
						sendMessage(sender, "There is no chest there!");
						return true;
					}
					sendMessage(sender, "Removed chest at " + loc + "!");
					chestSection.set(loc.toString(), null);
					scheduleSave();
				} else {
					sendMessage(sender, "Adding chests from the console isn't supported at the time");
				}
			}
			return true;
			case "listChests": {
				Set<String> configChests = chestSection.getKeys(false);
				List<String> toRemove = new ArrayList<>(Math.min(configChests.size(), 8));
				if (!configChests.isEmpty()) {
					for (String str : configChests) {
						BlockLocation chest = BlockLocation.parseLocation(str);
						if (chest == null) {
							toRemove.add(str);
							continue;
						}
						sendMessage(sender, str);
					}
				} else {
					sendMessage(sender, "There aren't any chests inside the worlds!");
				}
				for (String remove : toRemove) {
					chestSection.set(remove, null);
				}
			}
			break;
			default: {
				sendMessage(sender, "Unknown command: " + command.getName());
			}
			return true;
		}
		return false;
	}

	@EventHandler
	public void onEvent(InventoryCloseEvent event) {
		if (this.chests.containsKey(event.getPlayer().getUniqueId())) {
			Inventory inv = this.chests.get(event.getPlayer().getUniqueId());
			int size = inv.getSize();
			ItemStack[] contents = inv.getContents();
			for (int i = 0; i < size; i++) {
				if (contents[i] != null) {
					event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), contents[i]);
				}
			}
			inv.clear();
			this.chests.remove(event.getPlayer().getUniqueId());
		}
	}

	@EventHandler
	public void onEvent(PlayerQuitEvent event) {
		if (this.chests.containsKey(event.getPlayer().getUniqueId())) {
			Inventory inv = this.chests.get(event.getPlayer().getUniqueId());
			int size = inv.getSize();
			ItemStack[] contents = inv.getContents();
			for (int i = 0; i < size; i++) {
				if (contents[i] != null) {
					event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), contents[i]);
				}
			}
			inv.clear();
			this.chests.remove(event.getPlayer().getUniqueId());
		}
	}
}
