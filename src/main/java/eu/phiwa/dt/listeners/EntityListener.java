package eu.phiwa.dt.listeners;

import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import eu.phiwa.dt.DragonTravelMain;
import eu.phiwa.dt.RyeDragon;
import eu.phiwa.dt.filehandlers.Config;


public class EntityListener implements Listener {

	DragonTravelMain plugin;

	public EntityListener(DragonTravelMain plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onEnderDragonExplode(EntityExplodeEvent event) {
		if (!(event.getEntity() instanceof EnderDragon)) {
			return;
		}
		if (Config.getInstance().shouldAntigriefAllDragons()) {
			event.setCancelled(true);
		} else if (Config.getInstance().shouldAntigriefDTDragons() && event.getEntity() instanceof RyeDragon) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDamage(EntityDamageEvent event) {

		if (!(event.getEntity() instanceof Player))
			return;

		Player player = (Player) event.getEntity();
		if (DragonTravelMain.listofDragonriders.containsKey(player))
			if (!Config.config.getBoolean("VulnerableRiders"))
				event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerDeath(EntityDeathEvent event) {

		if (!(event.getEntity() instanceof Player))
			return;

		Player player = (Player) event.getEntity();

		if (!DragonTravelMain.listofDragonriders.containsKey(player))
			return;

		RyeDragon dragon = DragonTravelMain.listofDragonriders.get(player);
		dragon.getBukkitEntity().remove();
		DragonTravelMain.listofDragonriders.remove(player);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (event.getSpawnReason() != SpawnReason.CUSTOM)
			return;

		if (!event.getEntity().getType().toString().equals("ENDER_DRAGON"))
			return;

		if (!event.isCancelled())
			return;

		if (Config.getInstance().doWorldguardBypass() == true)
			event.setCancelled(false);
	}
}
