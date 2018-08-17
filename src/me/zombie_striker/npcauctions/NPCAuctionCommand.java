package me.zombie_striker.npcauctions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class NPCAuctionCommand implements CommandExecutor, TabExecutor {
	private Main m;

	public NPCAuctionCommand(Main k) {
		this.m = k;
	}

	@Override
	public boolean onCommand(final CommandSender sender, Command arg1, String commandname, String[] args) {
		if (args.length == 0) {
			sendMessage(sender);
		} else if (args[0].equalsIgnoreCase("spawnNPC")) {
			if (sender.hasPermission("npcauctions.create") && (sender instanceof Player)) {
				new BukkitRunnable() {

					@Override
					public void run() {
						if (Main.USE_VILLAGERS) {
							VillagerAuction.spawnVillager(((Player) sender).getLocation().add(0.1, 0.1, 0.1));
						} else {
							net.citizensnpcs.api.npc.NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry()
									.createNPC(EntityType.PLAYER, Main.s_VillagerName);
							npc.spawn(((Player) sender).getLocation().add(0.1, 0.1, 0.1));
						}

					}
				}.runTaskLater(m, 20);
				sender.sendMessage(Main.prefix + " NPC has been created");
			} else {
				sender.sendMessage(Main.prefix + ChatColor.RED + Main.s_NOPERM);
			}
		} else if (args[0].equalsIgnoreCase("open")) {
			if (sender instanceof Player)
				((Player) sender).openInventory(Main.gui[0]);

		} else if (args[0].equalsIgnoreCase("respawn")) {
			for (Entry<UUID, Location> k : Main.tpbackto.entrySet()) {
				for (Entity e : k.getValue().getWorld().getEntities()) {
					if (e.getUniqueId().equals(k.getKey())) {
						e.teleport(k.getValue());
						return true;
					}
				}
				VillagerAuction.spawnVillager(k.getValue());
			}
			sender.sendMessage(Main.prefix + " all NPCS are back at their spawn locations");

		} else if (args[0].equalsIgnoreCase("usevillagers")) {

			if (sender.hasPermission("npcauctions.create")) {
				Main.USE_VILLAGERS = !Main.USE_VILLAGERS;
				m.getConfig().set("UseVillager", Main.USE_VILLAGERS);
				m.saveConfig();
				sender.sendMessage(Main.prefix + " UseVillagers has been swiched to " + Main.USE_VILLAGERS);
			} else {
				sender.sendMessage(Main.prefix + ChatColor.RED + Main.s_NOPERM);
			}
		} else if (args[0].equalsIgnoreCase("removenpc")) {

			if (sender.hasPermission("npcauctions.destroy")) {
				if (sender instanceof Player) {
					Player player = (Player) sender;

					Main.removeAuctions.add(player.getUniqueId());
					sender.sendMessage(Main.prefix + "Attack an auction house to remove it.");
				} else {
					sender.sendMessage(
							Main.prefix + ChatColor.RED + " You must be a player in order to use this command.");

				}
			} else {
				sender.sendMessage(Main.prefix + ChatColor.RED + Main.s_NOPERM);
			}
		} else if (args[0].equalsIgnoreCase("removeallnpcs")) {

			if (sender.hasPermission("npcauctions.destroy")) {
				if (sender instanceof Player) {
					Player player = (Player) sender;

					int j = 0;
					net.citizensnpcs.api.npc.NPCRegistry inter = net.citizensnpcs.api.CitizensAPI.getNPCRegistry();
					for (Entity e : player.getWorld().getEntities()) {
						if (inter.isNPC(e)) {
							net.citizensnpcs.api.npc.NPC npc = inter.getNPC(e);
							if (npc.getName().equals(Main.s_VillagerName)) {
								npc.destroy();
								j++;
							}
						}
					}
					if (j > 0) {
						sender.sendMessage(Main.prefix + " " + j + " auction house NPCs have been removed.");
					} else {
						sender.sendMessage(Main.prefix + " There are no auction house NPCs.");
					}
				} else {
					sender.sendMessage(
							Main.prefix + ChatColor.RED + " You must be a player in order to use this command.");

				}
			} else {
				sender.sendMessage(Main.prefix + ChatColor.RED + Main.s_NOPERM);
			}
		} else {
			sendMessage(sender);
		}
		return false;
	}

	public void sendMessage(CommandSender s) {
		s.sendMessage("COMMANDS");
		s.sendMessage("/npca spawnNPC: Spawns an NPC");
		s.sendMessage("/npca removeallnpcs: Removes all NPCS from the world");
		s.sendMessage("/npca usevillagers: Toggles whether CitizensNPCs or villagers are used.");
		s.sendMessage("/npca removenpc : Allows the user to remove villagers");
		s.sendMessage("/npca respawn: In case villagers despawn, use this to readd them");
		s.sendMessage("/npca open: Opens the auction house.");
	}

	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		if (arg3.length == 1) {
			List<String> k = new ArrayList<String>();
			k.add("spawnNPC");
			k.add("removeallnpcs");
			k.add("usevillagers");
			k.add("removenpc");
			k.add("respawn");
			k.add("open");
			return k;
		}
		return null;
	}

}
