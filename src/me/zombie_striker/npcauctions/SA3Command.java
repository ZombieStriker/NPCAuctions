package me.zombie_striker.npcauctions;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class SA3Command implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (sender.hasPermission("npcauctions.destroy")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;

				int j = 0;
				NPCRegistry inter = CitizensAPI.getNPCRegistry();
				for (Entity e : player.getWorld().getEntities()) {
					if (inter.isNPC(e)) {
						NPC npc = inter.getNPC(e);
						if (npc.getName().equals(Main.s_VillagerName)) {
							npc.destroy();
							j++;
						}
					}
				}
				if (j > 0) {
					sender.sendMessage(Main.prefix + " " + j
							+ " auction house NPCs have been removed.");
				} else {
					sender.sendMessage(Main.prefix
							+ " There are no auction house NPCs.");
				}
			} else {
				sender.sendMessage(Main.prefix + ChatColor.RED
						+ " You must be a player in order to use this command.");

			}
		} else {
			sender.sendMessage(Main.prefix + ChatColor.RED + Main.s_NOPERM);
		}
		return true;
	}
}
