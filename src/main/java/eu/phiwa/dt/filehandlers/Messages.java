package eu.phiwa.dt.filehandlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import eu.phiwa.dt.DragonTravelMain;
import eu.phiwa.dt.util.Utils;

public class Messages {

	String pathInsideJAR = "eu/phiwa/dt/filehandlers/messages/";
	String pathOnServer = "plugins/DragonTravel/messages";

	DragonTravelMain plugin;

	public Messages(DragonTravelMain plugin) {
		this.plugin = plugin;
	}

	private String language = "";

	private static final double messagesVersion = 0.2;
	private File messagesFile;
	private FileConfiguration messages;

	public boolean loadMessages() {
		language = Config.getInstance().getLanguage();

		if (language == null) {
			plugin.severe("Could not load messages-file because the language could not be read from the config! Disabling plugin!");
			new RuntimeException("No language set").printStackTrace();

			return false;
		}

		messagesFile = new File(plugin.getDataFolder(), "locale/messages-" + language + ".yml");

		if (!messagesFile.exists()) {
			Utils.deployDefaultFile("locale/messages-" + language + ".yml", plugin);
		}

		messages = YamlConfiguration.loadConfiguration(messagesFile);
		updateConfig();

		return true;
	}

	private void updateConfig() {
		if (messages.getDouble("File.Version") != messagesVersion) {
			newlyRequiredMessages();
		}

		noLongerRequiredMessages();

		// Refresh file and config variables for persistence.
		try {
			messages.save(messagesFile);
			messages = YamlConfiguration.loadConfiguration(messagesFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void newlyRequiredMessages() {

		// Add new keys here!

		// v0.0.0.9
		if (Config.config.get("Messages.Flights.Error.OnlySigns") == null)
			Config.config.set("Messages.Flights.Error.OnlySigns", "&cThis command has been disabled by the admin, you can only use flights using signs.");
		if (Config.config.get("Messages.Stations.Error.NotCreateStationWithRandomstatName") == null)
			Config.config.set("Messages.Stations.Error.NotCreateStationWithRandomstatName", "&cYou cannot create a staion with the name of the RandomDest.");
	}

	private void noLongerRequiredMessages() {
		// DragonTravelMain.config.set("example key", null);
	}

	public String getMessage(String path) {
		String message = messages.getString(path);

		if (message == null) {
			plugin.severe("Missing translation: '" + path + "' -- need to regenerate?");
			return replaceColors("&cAn error occured, please contact the admin!");
		}

		if (message.length() == 0) {
			plugin.severe("Empty translation: '" + path + "' -- need to regenerate?");
			return replaceColors("&cAn error occured, please contact the admin!");
		}

		return replaceColors(message);
	}

	private String replaceColors(String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}
}
