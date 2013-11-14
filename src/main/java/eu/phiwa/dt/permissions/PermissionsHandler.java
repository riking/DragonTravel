package eu.phiwa.dt.permissions;

import org.bukkit.command.CommandSender;

public class PermissionsHandler {

	/**
	 * Checks if the specified player has the permission to use the specified
	 * travel-type to travel to the specified destination.
	 *
	 * @param player Player to check the permission for
	 * @param traveltype Type of travel ("travel", "...", ...)
	 * @param destination Destination to check the permission for (if null,
	 *             only checks the general permission)
	 * @return "True" if the player has the permissions, "false" if he hasn't
	 */
	public static boolean hasTravelPermission(CommandSender player, String traveltype, String destinationname) {
		return player.hasPermission("dt.travel.*") || player.hasPermission("dt.travel." + destinationname);
	}

	/**
	 * Checks if the specified player has the permission to use the specified
	 * flight
	 *
	 * @param player Player to check the permission for
	 * @param flightname Flight to check the permission for (if null, only
	 *             checks the general permission)
	 * @return "True" if the player has the permissions, "false" if he hasn't
	 */
	public static boolean hasFlightPermission(CommandSender player, String flightname) {
		return player.hasPermission("dt.flight.*") || player.hasPermission("dt.flight." + flightname);
	}
}
