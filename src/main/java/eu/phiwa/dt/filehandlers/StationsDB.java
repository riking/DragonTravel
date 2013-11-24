package eu.phiwa.dt.filehandlers;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import eu.phiwa.dt.DragonTravelMain;
import eu.phiwa.dt.Station;
import eu.phiwa.dt.util.Utils;

public class StationsDB {
	private DragonTravelMain plugin;
	private File dbStationsFile;
	private FileConfiguration dbStationsConfig;
	private ConfigurationSection stationSection;

	public StationsDB(DragonTravelMain plugin) {
		this.plugin = plugin;
	}

	public void init() {

		dbStationsFile = new File(DragonTravelMain.databaseFolder, "stations.yml");

		try {
			create();
		} catch (Exception e) {
			plugin.warning("Could not initialize the stations-database.");
			e.printStackTrace();
		}

		dbStationsConfig = new YamlConfiguration();
		load();

		stationSection = dbStationsConfig.getConfigurationSection("Stations");
		if (stationSection == null) {
			stationSection = dbStationsConfig.createSection("Stations");
		}
	}

	private void create() {
		if (dbStationsFile.exists())
			return;

		try {
			dbStationsFile.createNewFile();
			Utils.deployDefaultFile("databases/stations.yml", plugin);
			plugin.info("Created stations-database.");
		} catch (Exception e) {
			plugin.warning("Could not create the stations-database!");
		}
	}

	private void load() {
		try {
			dbStationsConfig.load(dbStationsFile);
			plugin.info("Loaded stations-database.");
		} catch (Exception e) {
			plugin.warning("No stations-database found");
			e.printStackTrace();
		}
	}


	/**
	 * Returns the details of the station with the given name.
	 *
	 * @param stationname Name of the station which should be returned.
	 * @return The station as a station-object.
	 */
	public Station getStation(String stationname) {
		Object obj = stationSection.get(stationname, null);
		if (obj == null) {
			return null;
		}

		// Transition support
		if (obj instanceof ConfigurationSection) {
			Station s = new Station(((ConfigurationSection) obj).getValues(true));
			saveStation(s);
			return s;
		} else {
			return (Station) obj;
		}
	}

	/**
	 * Creates a new station.
	 *
	 * @param station Station to create.
	 * @return Returns true if the station was created successfully, false if
	 *         not.
	 */
	public boolean saveStation(Station station) {
		stationSection.set(station.name, station);

		try {
			dbStationsConfig.save(dbStationsFile);
			return true;
		} catch (Exception e) {
			plugin.warning("Could not write new station to config.");
			return false;
		}
	}

	/**
	 * Deletes the given station.
	 *
	 * @param stationname Name of the station to delete
	 * @return True if successful, false if not.
	 */
	public boolean deleteStation(String stationname) {
		stationSection.set(stationname.toLowerCase(), null);

		try {
			dbStationsConfig.save(dbStationsFile);
			return true;
		} catch (Exception e) {
			plugin.warning("Could not delete station from config.");
			return false;
		}
	}

	public void showStations(CommandSender sender) {
		sender.sendMessage("Available stations: ");
		int i = 0;
		for (String string : dbStationsConfig.getConfigurationSection("Stations").getKeys(false)) {
			Station station = getStation(string);
			if (station != null) {
				sender.sendMessage(" - " + station.displayName);
				i++;
			}
		}
		sender.sendMessage(String.format("(total %d)", i));
	}

	public boolean checkForStation(Player player) {
		String pathToStation;
		int x, y, z;
		World world;
		Location tempLoc;
		Location playerLoc = player.getLocation();

		for (String string : dbStationsConfig.getConfigurationSection("Stations").getKeys(true)) {
			if (string.contains(".displayname")) {
				pathToStation = "Stations." + string;
				pathToStation = pathToStation.replace(".displayname", "");

				String worldname = dbStationsConfig.getString(pathToStation + ".world");

				if (worldname == null) {
					plugin.severe("The world of the station " + dbStationsConfig.getString(pathToStation + ".displayname") + " could not be read from the database, please check it for errors!");
					player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.General.Error.DatabaseCorrupted"));
					return false;
				}

				world = Bukkit.getWorld(worldname);

				if (world == null) {
					plugin.severe("Skipping station '" + dbStationsConfig.getString(pathToStation + ".displayname") + "' while checking for a station. There is no world '" + dbStationsConfig.getString(pathToStation + ".world") + "' on the server!");
					continue;
				}


				if (!world.getName().equalsIgnoreCase(player.getWorld().getName()))
					continue;

				x = dbStationsConfig.getInt(pathToStation + ".x");
				y = dbStationsConfig.getInt(pathToStation + ".y");
				z = dbStationsConfig.getInt(pathToStation + ".z");

				tempLoc = new Location(world, x, y, z);

				if (tempLoc.distance(playerLoc) <= Config.config.getInt("MountingLimit.Radius"))
					return true;
			}
		}

		return false;
	}
}
