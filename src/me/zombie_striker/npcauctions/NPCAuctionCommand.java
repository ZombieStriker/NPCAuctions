package me.zombie_striker.npcauctions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import net.citizensnpcs.npc.skin.SkinnableEntity;

public class NPCAuctionCommand implements CommandExecutor, TabExecutor {
	private Main m;

	public NPCAuctionCommand(Main k) {
		this.m = k;
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
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
							if (Main.auctionhouseSkin != null && !(Main.auctionhouseSkin.equalsIgnoreCase("null"))) {
								SkinnableEntity se = ((SkinnableEntity) npc.getEntity());
								se.setSkinName(Main.auctionhouseSkin);
							}
						}

					}
				}.runTaskLater(m, 20);
				sender.sendMessage(Main.prefix + " NPC has been created");
			} else {
				sender.sendMessage(Main.prefix + ChatColor.RED + Main.s_NOPERM);
			}
		} else if (args[0].equalsIgnoreCase("reload")) {
			if (sender.hasPermission("npcauctions.reload")) {
				m.reloadConfig();
				m.reloadVals();
				sender.sendMessage("Reloaded config values");
			} else {
				sender.sendMessage(ChatColor.RED + " You are not allowed to use this command.");
			}
		} else if (args[0].equalsIgnoreCase("open")) {
			if (sender.hasPermission("npcauctions.openGUIFromCommand"))
				if (sender instanceof Player)
					((Player) sender).openInventory(Main.gui[0]);

		} else if (args[0].equalsIgnoreCase("cancelAuction")) {
			if (!sender.hasPermission("npcauctions.cancelauction"))
				return false;
			OfflinePlayer user = null;
			if (args.length > 1) {
				user = Bukkit.getOfflinePlayer(args[1]);
			} else {
				user = (Player) sender;
			}
			int endAuctions = 0;
			for (Auction aa : Main.instance.auctions) {
				if (aa.owner.equals(user.getUniqueId())) {
					endAuctions++;

					if (!user.isOnline()) {
						List<ItemStack> items = (List<ItemStack>) m.getConfig()
								.get(aa.lastBid.toString() + ".recievedItems");
						if (items == null)
							items = new ArrayList<ItemStack>();
						items.add(aa.is);
						m.getConfig().set(aa.lastBid.toString() + ".recievedItems", items);
						m.saveConfig();
					} else {
						((HumanEntity) user).getInventory().addItem(aa.is);
					}
					if (aa.lastBid != null) {
						Main.econ.depositPlayer(Bukkit.getOfflinePlayer(aa.lastBid), aa.currentPrice);
						if (Bukkit.getPlayer(aa.lastBid) != null)
							Bukkit.getPlayer(aa.lastBid)
									.sendMessage(Main.prefix + Main.s_auctionCancelRefund
											.replace("%amount%", "" + aa.currentPrice).replace("%item%",
													(aa.is.getItemMeta().hasDisplayName()
															? aa.is.getItemMeta().getDisplayName()
															: aa.is.getType().name()) + ".x." + aa.is.getAmount()));
					}
					m.auctions.remove(aa);
					m.getConfig().set("Auctions." + aa.owner.toString() + "." + aa.auctionID.toString(), null);
					m.saveConfig();

				}
			}
			sender.sendMessage(Main.prefix + " Ended " + endAuctions + " auctions for " + user.getName() + ".");

			/**
			 * 
			 * 
			 * if (!creator.isOnline()) { double i = (withBuyItNow ? a.buyitnow :
			 * a.currentPrice); if (getConfig().contains(a.owner.toString() +
			 * ".offlineAmount")) i += getConfig().getDouble(a.owner.toString() +
			 * ".offlineAmount"); getConfig().set(a.owner.toString() + ".offlineAmount", i);
			 * saveConfig(); }
			 * 
			 * 
			 * 
			 * e.getWhoClicked().getInventory().addItem(aa.is); if (aa.lastBid != null) {
			 * econ.depositPlayer(Bukkit.getOfflinePlayer(aa.lastBid), aa.currentPrice); if
			 * (Bukkit.getPlayer(aa.lastBid) != null) Bukkit.getPlayer(aa.lastBid)
			 * .sendMessage(prefix + s_auctionCancelRefund .replace("%amount%", "" +
			 * aa.currentPrice).replace("%item%", (aa.is.getItemMeta().hasDisplayName() ?
			 * aa.is.getItemMeta().getDisplayName() : aa.is.getType().name()) + ".x." +
			 * aa.is.getAmount())); } auctions.remove(aa); getConfig().set("Auctions." +
			 * aa.owner.toString() + "." + aa.auctionID, null); saveConfig();
			 */
			sender.sendMessage(Main.prefix + " Ending all auctions");
		} else if (args[0].equalsIgnoreCase("endAllAuctions")) {
			if (!sender.hasPermission("npcauctions.endall"))
				return false;
			for (Auction a : Main.instance.auctions) {
				a.setWait(0);
			}
			sender.sendMessage(Main.prefix + " Ending all auctions");

		} else if (args[0].equalsIgnoreCase("respawn")) {
			for (Entry<UUID, Location> k : new HashSet<>(Main.tpbackto.entrySet())) {
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
		s.sendMessage("/npca endAllAuctions: Ends all auctions.");
		s.sendMessage("/npca cancelAuction: Ends all of a user's auctions.");
		s.sendMessage("/npca reload: Reload config values (does not affect auctions).");
	}

	public void a(List<String> k, String test, String arg) {
		if (test.toLowerCase().startsWith(arg.toLowerCase()))
			k.add(test);
	}

	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] args) {
		if (args.length == 1) {
			List<String> k = new ArrayList<String>();
			a(k, "cancelAuction", args[0]);
			a(k, "spawnNPC", args[0]);
			a(k, "removeallnpcs", args[0]);
			a(k, "usevillagers", args[0]);
			a(k, "removenpc", args[0]);
			a(k, "respawn", args[0]);
			a(k, "open", args[0]);
			a(k, "reload", args[0]);
			a(k, "endAllAuctions", args[0]);
			return k;
		} else if (args.length == 2) {
			if (args[0].equalsIgnoreCase("cancelAuction")) {
				List<String> k = new ArrayList<String>();
				for (Auction aa : m.auctions) {
					if (!k.contains(Bukkit.getOfflinePlayer(aa.owner).getName()))
						a(k, Bukkit.getOfflinePlayer(aa.owner).getName(), args[0]);
				}
				return k;
			}
		}
		return null;
	}

}
