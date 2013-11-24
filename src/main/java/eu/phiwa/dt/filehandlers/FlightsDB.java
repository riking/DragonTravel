package eu.phiwa.dt.filehandlers;

import java.io.File;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.sk89q.minecraft.util.commands.ChatColor;

import eu.phiwa.dt.DragonTravelMain;
import eu.phiwa.dt.Flight;
import eu.phiwa.dt.permissions.PermissionsHandler;
import eu.phiwa.dt.util.Utils;

public class FlightsDB {
	private DragonTravelMain plugin;
	private File dbFlightsFile;
	private FileConfiguration dbFlightsConfig;
	private ConfigurationSection flightSection;

	public FlightsDB(DragonTravelMain plugin) {
		this.plugin = plugin;
	}

	public void init() {

		dbFlightsFile = new File(DragonTravelMain.databaseFolder, "flights.yml");

		try {
			create();
		} catch (Exception e) {
			plugin.warning("Could not initialize the flights-database.");
			e.printStackTrace();
		}

		dbFlightsConfig = new YamlConfiguration();
		load();

		flightSection = dbFlightsConfig.getConfigurationSection("Flights");
		if (flightSection == null) {
			flightSection = dbFlightsConfig.createSection("Flights");
		}
	}

	private void create() {

		if (dbFlightsFile.exists())
			return;

		try {
			dbFlightsFile.createNewFile();
			Utils.deployDefaultFile("databases/flights.yml", plugin);
			plugin.info("Created flights-database.");
		} catch (Exception e) {
			plugin.warning("Could not create the flights-database!");
		}


	}

	private void load() {
		try {
			dbFlightsConfig.load(dbFlightsFile);
			plugin.info("Loaded flights-database.");
		} catch (Exception e) {
			plugin.warning("No flights-database found");
		}
	}


	/**
	 * Returns the details of the flight with the given name.
	 *
	 * @param flightname Name of the flight which should be returned.
	 * @return The flight as a flight-object.
	 */
	public Flight getFlight(String flightname) {
		flightname = flightname.toLowerCase();
		Object obj = flightSection.get(flightname, null);
		if (obj == null) {
			return null;
		}

		// Transition support
		if (obj instanceof ConfigurationSection) {
			Flight f = new Flight(((ConfigurationSection) obj).getValues(true));
			f.name = flightname;
			saveFlight(f);
			return f;
		} else {
			Flight f = (Flight) obj;
			f.name = flightname;
			return f;
		}
	}

	/**
	 * Creates a new flight.
	 *
	 * @param flight Flight to create.
	 * @return Returns true if the flight was created successfully, false if
	 *         not.
	 */
	public boolean saveFlight(Flight flight) {
		flightSection.set(flight.name, flight);

		try {
			dbFlightsConfig.save(dbFlightsFile);
			return true;
		} catch (Exception e) {
			plugin.warning("Could not write new flight to config.");
			return false;
		}
	}

	/**
	 * Deletes the given flight.
	 *
	 * @param flightname Name of the flight to delete
	 * @return True if successful, false if not.
	 */
	public boolean deleteFlight(String flightname) {
		flightSection.set(flightname.toLowerCase(), null);

		try {
			dbFlightsConfig.save(dbFlightsFile);
			return true;
		} catch (Exception e) {
			plugin.warning("Could not delete flight from config.");
			return false;
		}
	}

	/**
	 * Prints all flights from the database to the specified player.
	 *
	 * @param player Player to print the flights to
	 */
	public void showFlights(CommandSender sender) {
		sender.sendMessage("Available flights: ");
		int i = 0;
		for (String string : dbFlightsConfig.getConfigurationSection("Flights").getKeys(false)) {
			Flight flight = getFlight(string);
			if (flight != null) {
				// Permission check added - green if available, red if unavailable, aqua if console
				sender.sendMessage(" - " + (sender instanceof Player ? (PermissionsHandler.hasFlightPermission((Player) sender, flight.name) ? ChatColor.GREEN : ChatColor.RED) : ChatColor.AQUA) + flight.displayname);
				i++;
			}
		}
		sender.sendMessage(String.format("(total %d)", i));
	}
}
