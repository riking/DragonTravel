package eu.phiwa.dt.filehandlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import eu.phiwa.dt.DragonTravelMain;
import eu.phiwa.dt.payment.ChargeType;

public class Config {
	private DragonTravelMain plugin;
	private File configFile;
	public static FileConfiguration config;

	// Sections
	private ConfigurationSection requiredItemSection;
	private ConfigurationSection econPriceSection;
	private ConfigurationSection resourcePriceSection;

	public Config(DragonTravelMain plugin) {
		this.plugin = plugin;
	}

	/**
	 * Returns the singleton instance of the Config.
	 */
	public static Config getInstance() { return DragonTravelMain.config; }

	public void loadConfig() {
		configFile = new File(plugin.getDataFolder(), "config.yml");
		if (!configFile.exists())
			deployDefaultFile("config.yml");
		config = YamlConfiguration.loadConfiguration(configFile);
		updateConfig();

		requiredItemSection = config.getConfigurationSection("RequiredItem.For");
		econPriceSection = config.getConfigurationSection("Payment.Economy.Prices");
		resourcePriceSection = config.getConfigurationSection("Payment.Resources.Prices");

		// Validation
		if (usePayment()) {
			if (useEcon() && useResources()) {
				plugin.getLogger().severe("Both Payment.byEconomy and Payment.byResources are set to true. Attempting Economy first...");
			}
		}
	}

	private void updateConfig() {
		if (config.getDouble("File.Version") != DragonTravelMain.configVersion) {
			newlyRequiredConfig();
		}
		noLongerRequiredConfig();
		// Refresh file and config variables for persistence.
		try {
			config.save(configFile);
			config = YamlConfiguration.loadConfiguration(configFile);
		} catch (IOException e) {
			e.printStackTrace();
			DragonTravelMain.logger.severe("Could not update config, disabling plugin!");
		}
	}

	private void newlyRequiredConfig() {

		// New options in version 0.2
		if (!config.isSet("PToggleDefault"))
			config.set("PToggleDefault", true);


		// Update the file version
		config.set("File.Version", DragonTravelMain.configVersion);

	}

	private void noLongerRequiredConfig() {
		// DragonTravelMain.config.set("example key", null);
	}


	private void deployDefaultFile(String name) {
		try {
			File target = new File(this.plugin.getDataFolder(), name);
			InputStream source = this.plugin.getResource(name);

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

	// Payment
	public boolean usePayment() { return config.getBoolean("Payment.usePayment"); }
	public boolean useEcon() { return config.getBoolean("Payment.byEconomy"); }
	public boolean useResources() { return config.getBoolean("Payment.byResources"); }
	public boolean requiresItem(ChargeType type) { return requiredItemSection.getBoolean(type.getConfigString()); }
	public int getEconPrice(ChargeType type) { return econPriceSection.getInt(type.getConfigString()); }
	public int getResourcePrice(ChargeType type) { return resourcePriceSection.getInt(type.getConfigString()); }

	public Material getRequiredMaterial() {
		return Material.matchMaterial(config.getString("RequiredItem.Item", "DRAGON_EGG"));
	}
}
