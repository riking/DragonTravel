package eu.phiwa.dt.filehandlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import eu.phiwa.dt.DragonTravelMain;
import eu.phiwa.dt.payment.ChargeType;

public class Config {
	private DragonTravelMain plugin;
	private File configFile;
	public static FileConfiguration config;

	public Config(DragonTravelMain plugin) {
		this.plugin = plugin;
	}

	public void loadConfig() {
		configFile = new File(plugin.getDataFolder(), "config.yml");
		if (!configFile.exists())
			deployDefaultFile("config.yml");
		Config.config = YamlConfiguration.loadConfiguration(configFile);
		updateConfig();
	}

	private void updateConfig() {
		if (Config.config.getDouble("File.Version") != DragonTravelMain.configVersion)
			newlyRequiredConfig();
		noLongerRequiredConfig();
		// Refresh file and config variables for persistence.
		try {
			Config.config.save(configFile);
			Config.config = YamlConfiguration.loadConfiguration(configFile);
		} catch (IOException e) {
			e.printStackTrace();
			DragonTravelMain.logger.severe("Could not update config, disabling plugin!");
		}
	}

	private void newlyRequiredConfig() {

		// New options in version 0.2
		if (!Config.config.isSet("PToggleDefault"))
			Config.config.set("PToggleDefault", true);


		// Update the file version
		Config.config.set("File.Version", DragonTravelMain.configVersion);

	}

	private void noLongerRequiredConfig() {
		// DragonTravelMain.config.set("example key", null);
	}


	private void deployDefaultFile(String name) {
		try {
			File target = new File(this.plugin.getDataFolder(), name);
			InputStream source = this.plugin.getResource("eu/phiwa/dt/filehandlers/" + name);

			if (!target.exists()) {
				OutputStream output = new FileOutputStream(target);
				byte[] buffer = new byte[1024];
				int len;
				while ((len = source.read(buffer)) > 0)
					output.write(buffer, 0, len);
				output.close();
			}
			source.close();
			DragonTravelMain.logger.info("Deployed " + name);
		} catch (Exception e) {
			DragonTravelMain.logger.info("Could not save default file");
		}
	}

	public String getFileVersion() { return config.getString("File.Version"); }
	public boolean shouldAntigriefDTDragons() { return config.getBoolean("AntiGriefDragons.ofDragonTravel"); }
	public boolean shouldAntigriefAllDragons() { return config.getBoolean("AntiGriefDragons.all"); }
	public boolean doWorldguardBypass() { return config.getBoolean("AntiGriefDragons.bypassWorldGuardAntiSpawn", true); }

	public Material getRequiredMaterial() {
		return Material.matchMaterial(config.getString("RequiredItem.Item", "DRAGON_EGG"));
	}

	public boolean requiresItem(ChargeType type) {
		return config.getBoolean("RequiredItem.For." + type.getConfigString());
	}
}
