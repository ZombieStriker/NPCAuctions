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

public class RemoveAHCommand implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (sender.hasPermission("npcauctions.destroy")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;

				Main.removeAuctions.add(player.getUniqueId());
					sender.sendMessage(Main.prefix + "Attack an auction house to remove it.");
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
