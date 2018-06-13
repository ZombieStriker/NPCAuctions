package me.zombie_striker.npcauctions;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class UseVillagerSwapCommand implements CommandExecutor {

	
	private JavaPlugin main;
	public UseVillagerSwapCommand(JavaPlugin m) {
		this.main = m;
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (sender.hasPermission("npcauctions.create")) {
			Main.USE_VILLAGERS = !Main.USE_VILLAGERS;
			main.getConfig().set("UseVillager", Main.USE_VILLAGERS);
			main.saveConfig();
			sender.sendMessage(Main.prefix
					+ " UseVillagers has been swiched to " + Main.USE_VILLAGERS);
		} else {
			sender.sendMessage(Main.prefix + ChatColor.RED + Main.s_NOPERM);
		}
		return true;
	}
}
