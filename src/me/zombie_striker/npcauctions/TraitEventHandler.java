package me.zombie_striker.npcauctions;

import net.citizensnpcs.api.event.*;

import org.bukkit.entity.Villager;
import org.bukkit.event.*;

public class TraitEventHandler implements Listener {

	@EventHandler
	public void click(NPCRightClickEvent event) {
		if (event.getNPC().getName().equals(Main.s_VillagerName)) {
			event.getClicker().openInventory(Main.gui[0]);
			if (Main.removeAuctions.contains(event.getClicker().getUniqueId())) {
				Main.removeAuctions.remove(event.getClicker().getUniqueId());
				event.getClicker().sendMessage(Main.prefix + " Villager removal canceled");
			}
		}
	}

	@EventHandler
	public void click(NPCLeftClickEvent event) {
		if (event.getNPC().getName().equals(Main.s_VillagerName)) {
			if (event.getClicker().hasPermission("npcauctions.destroy")
					&& (!(event.getNPC().getEntity() instanceof Villager))) {
				if (Main.removeAuctions.contains(event.getClicker().getUniqueId())) {
					Main.removeAuctions.remove(event.getClicker().getUniqueId());
					event.getNPC().destroy();
					event.getClicker().sendMessage(Main.prefix + " NPC has been removed.");
				}
			}
		}
	}
}
