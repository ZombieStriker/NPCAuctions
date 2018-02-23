package me.zombie_striker.npcauctions;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SACommand implements CommandExecutor {
	private Main m;

	public SACommand(Main k) {
		this.m = k;
	}

	@Override
	public boolean onCommand(final CommandSender sender, Command command,
			String label, String[] args) {
		if (sender.hasPermission("npcauctions.create")
				&& (sender instanceof Player)) {
			new BukkitRunnable() {

				@Override
				public void run() {
					if (Main.USE_VILLAGERS) {
						VillagerAuction.spawnVillager(((Player) sender)
								.getLocation().add(0.1, 0.1, 0.1));
					} else {
						NPC npc = CitizensAPI.getNPCRegistry().createNPC(
								EntityType.PLAYER, Main.s_VillagerName);
						npc.spawn(((Player) sender).getLocation().add(0.1, 0.1,
								0.1));
					}

				}
			}.runTaskLater(m, 20);
			sender.sendMessage(Main.prefix + " NPC has been created");
		} else {
			sender.sendMessage(Main.prefix + ChatColor.RED + Main.s_NOPERM);
		}
		return true;
	}
}
