package eu.phiwa.dt.movement;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.UPlayer;

import eu.phiwa.dt.DragonTravelMain;
import eu.phiwa.dt.Home;
import eu.phiwa.dt.RyeDragon;
import eu.phiwa.dt.Station;
import eu.phiwa.dt.filehandlers.Config;
import eu.phiwa.dt.modules.DragonManagement;
import eu.phiwa.dt.payment.ChargeType;

public class Travels {

	/**
	 * Travel to a specified station
	 *
	 * @param player
	 * @param stationname
	 * @param checkForStation Whether or not DragonTravel should check if the
	 *             player is at a station and return if not. If the admin
	 *             disabled the station-check globally, this has no function.
	 */
	public static void toStation(Player player, String stationname, Boolean checkForStation) {

		Station destination = DragonTravelMain.dbStationsHandler.getStation(stationname);

		if (destination == null) {
			player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.Stations.Error.StationDoesNotExist").replace("{stationname}", stationname));
			return;
		}

		if (DragonTravelMain.config.requiresItem(ChargeType.TRAVEL_TOSTATION)) {
			if (!player.getInventory().contains(DragonTravelMain.config.getRequiredMaterial()) && !player.hasPermission("dt.notrequireitem.travel")) {
				player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.General.Error.RequiredItemMissing"));
				return;
			}
		}

		player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.Travels.Successful.TravellingToStation").replace("{stationname}", destination.displayName));

		travel(player, destination.toLocation(), checkForStation);
	}

	/**
	 * Travel to a random destination within the borders set in the config
	 *
	 * @param player Player to bring to a random destination
	 * @param checkForStation Whether or not DragonTravel should check if the
	 *             player is at a station and return if not
	 */
	public static void toRandomdest(Player player, Boolean checkForStation) {

		if (DragonTravelMain.config.requiresItem(ChargeType.TRAVEL_TORANDOM)) {
			if (!player.getInventory().contains(DragonTravelMain.config.getRequiredMaterial()) && !player.hasPermission("dt.notrequireitem.travel")) {
				player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.General.Error.RequiredItemMissing"));
				return;
			}
		}

		int minX = Config.config.getInt("X-Axis.MinX");
		int maxX = Config.config.getInt("X-Axis.MaxX");
		int minZ = Config.config.getInt("Z-Axis.MinZ");
		int maxZ = Config.config.getInt("Z-Axis.MaxZ");
		double x = minX + (Math.random() * (maxX - 1));
		double z = minZ + (Math.random() * (maxZ - 1));
		Location randomLoc = new Location(player.getWorld(), x, 10, z);
		randomLoc.setY(randomLoc.getWorld().getHighestBlockAt(randomLoc).getY());

		player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.Travels.Successful.TravellingToRandomLocation"));

		travel(player, randomLoc, checkForStation);
	}

	/**
	 * Travel to specified coordinates
	 *
	 * @param player
	 * @param x
	 * @param y
	 * @param z
	 * @param worldname If "null", the player's current world is used.
	 * @param checkForStation Whether or not DragonTravel should check if the
	 *             player is at a station and return if not. If the admin
	 *             disabled the station-check globally, this has no function.
	 */
	public static void toCoordinates(Player player, int x, int y, int z, String worldname, Boolean checkForStation) {

		World world;

		if (worldname == null) {
			//No world was used in the command, using player's current world
			world = player.getWorld();
		} else {
			// Trying to find the world with the name the player used in the command
			world = Bukkit.getWorld(worldname);

			// If the world cannot be found, send an error-message to the player
			if (world == null) {
				player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.Travels.Error.WorldNotFound"));
				return;
			}
		}

		if (DragonTravelMain.config.requiresItem(ChargeType.TRAVEL_TOCOORDINATES)) {
			if (!player.getInventory().contains(DragonTravelMain.config.getRequiredMaterial()) && !player.hasPermission("dt.notrequireitem.travel")) {
				player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.General.Error.RequiredItemMissing"));
				return;
			}
		}

		Location loc = new Location(world, x, y, z);

		if (world.equals(player.getWorld())) {
			String message = DragonTravelMain.messagesHandler.getMessage("Messages.Travels.Successful.TravellingToCoordinatesSameWorld");
			message = message.replace("{x}", "%d");
			message = message.replace("{y}", "%d");
			message = message.replace("{z}", "%d");
			message = String.format(message, x, y, z);
			player.sendMessage(message);
		} else {
			String message = DragonTravelMain.messagesHandler.getMessage("Messages.Travels.Successful.TravellingToCoordinatesOtherWorld");
			message = message.replace("{x}", "%d");
			message = message.replace("{y}", "%d");
			message = message.replace("{z}", "%d");
			message = message.replace("{worldname}", "%4$s"); // guaranteed to be the 4th arg even if it's in front
			message = String.format(message, x, y, z, world.getName());
			player.sendMessage(message);
		}

		travel(player, loc, checkForStation);
	}

	/**
	 * Travel to a specified player
	 *
	 * @param player Player to bring to the other player
	 * @param targetplayer Player to bring the traveling player to
	 * @param checkForStation Whether or not DragonTravel should check if the
	 *             player is at a station and return if not. If the admin
	 *             disabled the station-check globally, this has no function.
	 */
	public static void toPlayer(Player player, Player targetplayer, Boolean checkForStation) {

		if (DragonTravelMain.config.requiresItem(ChargeType.TRAVEL_TOPLAYER)) {
			if (!player.getInventory().contains(DragonTravelMain.config.getRequiredMaterial()) && !player.hasPermission("dt.notrequireitem.travel")) {
				player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.General.Error.RequiredItemMissing"));
				return;
			}
		}

		Location targetLoc = targetplayer.getLocation();
		travel(player, targetLoc, checkForStation);
	}

	/**
	 * Travel to the specified player's home
	 *
	 * @param player Player to bring to his home
	 * @param checkForStation Whether or not DragonTravel should check if the
	 *             player is at a station and return if not. If the admin
	 *             disabled the station-check globally, this has no function.
	 */
	public static void toHome(Player player, Boolean checkForStation) {

		Home home = DragonTravelMain.dbHomesHandler.getHome(player.getName());

		if (home == null) {
			player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.Travels.Error.NoHomeSet"));
			return;
		}

		if (DragonTravelMain.config.requiresItem(ChargeType.TRAVEL_TOHOME)) {
			if (!player.getInventory().contains(DragonTravelMain.config.getRequiredMaterial()) && !player.hasPermission("dt.notrequireitem.travel")) {
				player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.General.Error.RequiredItemMissing"));
				return;
			}
		}


		travel(player, home.toLocation(), checkForStation);

		player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.Travels.Successful.TravellingToHome"));
	}

	/**
	 * Travel to the specified player's faction's home
	 *
	 * @param player Player to bring to his faction's home
	 * @param checkForStation Whether or not DragonTravel should check if the
	 *             player is at a station and return if not. If the admin
	 *             disabled the station-check globally, this has no function.
	 */
	public static void toFactionhome(Player player, Boolean checkForStation) {

		if (Bukkit.getPluginManager().getPlugin("Factions") == null) {
			player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.Factions.Error.FactionsNotInstalled"));
			return;
		}

		if (DragonTravelMain.config.requiresItem(ChargeType.TRAVEL_TOFACTIONHOME)) {
			if (!player.getInventory().contains(DragonTravelMain.config.getRequiredMaterial()) && !player.hasPermission("dt.notrequireitem.travel")) {
				player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.General.Error.RequiredItemMissing"));
				return;
			}
		}

		Faction faction = UPlayer.get(player).getFaction();


		if (faction.isNone()) {
			player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.Factions.Error.NoFactionMember"));
			return;
		}

		if (!faction.hasHome()) {
			player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.Factions.Error.FactionHasNoHome"));
			return;
		} else
			travel(player, faction.getHome().asBukkitLocation(), checkForStation);

	}

	/**
	 * Core-method of this class, handles the travel itself (e.g. the
	 * difference between normal and interworld-travels
	 *
	 * @param player
	 * @param destination
	 * @param checkForStation Whether or not DragonTravel should check if the
	 *             player is at a station and return if not. If the admin
	 *             disabled the station-check globally, this has no function.
	 */
	private static void travel(Player player, Location destination, Boolean checkForStation) {

		if (checkForStation && Config.config.getBoolean("MountingLimit.EnableForTravels") && !player.hasPermission("dt.ignoreusestations.travels")) {
			if (!DragonTravelMain.dbStationsHandler.checkForStation(player)) {
				player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.Stations.Error.NotAtAStation"));
				return;
			}
		}

		Location temploc = player.getLocation();
		if (destination.getWorld().equals(player.getWorld())) {
			temploc.setYaw(getCorrectYawForPlayer(player, destination));
			player.teleport(temploc);
		} else {
			Location temploc2 = new Location(player.getWorld(), player.getLocation().getX() + 80, player.getLocation().getY() + 80, player.getLocation().getZ() + 80);
			temploc.setYaw(getCorrectYawForPlayer(player, temploc2));
			player.teleport(temploc);
		}

		if (!DragonManagement.mount(player))
			return;

		if (!DragonTravelMain.listofDragonriders.containsKey(player))
			return;

		RyeDragon dragon = DragonTravelMain.listofDragonriders.get(player);

		if (destination.getWorld().equals(player.getWorld()))
			dragon.startTravel(destination, false);
		else
			dragon.startTravel(destination, true);

	}

	public static void beginFree(Player player) {
		Location temploc = player.getLocation();
		player.teleport(temploc);

		if (!DragonManagement.mount(player))
			return;

		if (!DragonTravelMain.listofDragonriders.containsKey(player))
			return;

		RyeDragon dragon = DragonTravelMain.listofDragonriders.get(player);
		dragon.startFreeFlight();
	}

	private static float getCorrectYawForPlayer(Player player, Location destination) {
		if (player.getLocation().getBlockZ() > destination.getBlockZ())
			return (float) (-Math.toDegrees(Math.atan((player.getLocation().getBlockX() - destination.getBlockX()) / (player.getLocation().getBlockZ() - destination.getBlockZ())))) + 180.0F;
		else if (player.getLocation().getBlockZ() < destination.getBlockZ())
			return (float) (-Math.toDegrees(Math.atan((player.getLocation().getBlockX() - destination.getBlockX()) / (player.getLocation().getBlockZ() - destination.getBlockZ()))));
		else {
			return player.getLocation().getYaw();
		}
	}
}
