package me.zombie_striker.npcauctions;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SA2Command implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (sender.hasPermission("npcauctions.create")) {
			Main.USE_VILLAGERS = !Main.USE_VILLAGERS;
			sender.sendMessage(Main.prefix
					+ " UseVillagers has been swiched to " + Main.USE_VILLAGERS);
		} else {
			sender.sendMessage(Main.prefix + ChatColor.RED + Main.s_NOPERM);
		}
		return true;
	}
}
