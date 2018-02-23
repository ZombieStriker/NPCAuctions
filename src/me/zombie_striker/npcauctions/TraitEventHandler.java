package me.zombie_striker.npcauctions;

import net.citizensnpcs.api.event.*;

import org.bukkit.entity.Villager;
import org.bukkit.event.*;

public class TraitEventHandler implements Listener {

	@EventHandler
	public void click(NPCRightClickEvent event) {
		if (event.getNPC().getName().equals(Main.s_VillagerName)) {
			event.getClicker().openInventory(Main.gui[0]);
		}
	}

	@EventHandler
	public void click(NPCLeftClickEvent event) {
		if (event.getNPC().getName().equals(Main.s_VillagerName)) {
			if (event.getClicker().hasPermission("npcauctions.destroy")
					&& (!(event.getNPC().getEntity() instanceof Villager))) {
				event.getNPC().destroy();
				event.getClicker().sendMessage(
						Main.prefix + " NPC has been removed.");
			}
		}
	}
}
