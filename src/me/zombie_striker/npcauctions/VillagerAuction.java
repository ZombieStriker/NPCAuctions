package me.zombie_striker.npcauctions;

import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class VillagerAuction implements Listener {

	private Main m;

	public VillagerAuction(Main m) {
		this.m = m;
	}

	@EventHandler
	public void interact(final PlayerInteractAtEntityEvent e) {
		if (e.getRightClicked() instanceof Villager) {
			if (e.getRightClicked().getCustomName() != null)
				if (e.getRightClicked().getCustomName().equalsIgnoreCase(Main.s_VillagerName)) {
					new BukkitRunnable() {
						@Override
						public void run() {
							e.getPlayer().openInventory(Main.gui[0]);
						}
					}.runTaskLater(m, 1);
					e.setCancelled(true);
				}
		}
	}

	@EventHandler
	public void onhit(EntityDamageByEntityEvent e) {
		if (e.getEntity().getCustomName() != null)
			if (e.getEntity().getCustomName().equalsIgnoreCase(Main.s_VillagerName)) {
				if (!(e.getDamager() instanceof Player)) {
					e.setCancelled(true);
					return;
				}
				if (!e.getDamager().hasPermission("npcauctions.destroy")) {
					e.setCancelled(true);
				} else if (Main.removeAuctions.contains(e.getDamager().getUniqueId())) {
					Main.removeAuctions.remove(e.getDamager().getUniqueId());
					e.getEntity().remove();
					if (e.getEntity() instanceof Villager)
						((Player) e.getDamager()).sendMessage(Main.prefix + " Villager has been removed");
				} else {
					Main.removeAuctions.remove(e.getDamager().getUniqueId());
					if (e.getEntity() instanceof Villager)
						((Player) e.getDamager()).sendMessage(Main.prefix + " Villager removal canceled");
				}
			}
	}

	public static Entity spawnVillager(Location loc) {
		Villager v = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
		v.setAdult();
		v.setAI(false);
		v.setSilent(true);
		v.setCustomNameVisible(true);
		v.setCustomName(Main.s_VillagerName);
		return v;
	}
}
