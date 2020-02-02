package me.zombie_striker.npcauctions;

import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class VillagerAuction implements Listener {

	public static Entity spawnVillager(Location loc) {
		Villager v = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
		v.setAdult();
		v.setAI(false);
		v.setSilent(true);
		v.setCustomNameVisible(true);
		v.setCustomName(Main.s_VillagerName);
		Main.tpbackto.put(v.getUniqueId(), loc);
		Main.instance.getConfig().set("NPCS." + v.getUniqueId().toString(), loc);
		Main.instance.saveConfig();
		return v;
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
					}.runTaskLater(Main.instance, 1);
					e.setCancelled(true);
				}
		}
	}

	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if (e instanceof EntityDamageByEntityEvent) {
			return;
		}
		if (e.getEntity().getCustomName() != null)
			if (e.getEntity().getCustomName().equalsIgnoreCase(Main.s_VillagerName)) {
				e.setCancelled(true);
				if (e.getCause() == DamageCause.FIRE_TICK || e.getCause() == DamageCause.FIRE) {
					e.getEntity().setFireTicks(0);
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
					if (e.getEntity() instanceof Villager) {
						((Player) e.getDamager()).sendMessage(Main.prefix + " Villager has been removed");
						Main.tpbackto.remove(e.getEntity().getUniqueId());
						Main.instance.getConfig().set("NPCS." + e.getEntity().getUniqueId().toString(), null);
						Main.instance.saveConfig();
					}
				} else {
					Main.removeAuctions.remove(e.getDamager().getUniqueId());
					if (e.getEntity() instanceof Villager)
						e.getDamager().sendMessage(Main.prefix + " Villager removal canceled");
				}
			}
	}
}
