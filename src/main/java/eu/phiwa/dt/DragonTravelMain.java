package eu.phiwa.dt;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.server.v1_6_R3.EntityTypes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.bukkit.util.BukkitCommandsManager;
import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.minecraft.util.commands.CommandUsageException;
import com.sk89q.minecraft.util.commands.MissingNestedCommandException;
import com.sk89q.minecraft.util.commands.WrappedCommandException;

import eu.phiwa.dt.anticheatplugins.CheatProtectionHandler;
import eu.phiwa.dt.commands.CommandHelpTopic;
import eu.phiwa.dt.commands.DragonTravelCommands;
import eu.phiwa.dt.filehandlers.Config;
import eu.phiwa.dt.filehandlers.FlightsDB;
import eu.phiwa.dt.filehandlers.HomesDB;
import eu.phiwa.dt.filehandlers.Messages;
import eu.phiwa.dt.filehandlers.StationsDB;
import eu.phiwa.dt.flights.FlightEditor;
import eu.phiwa.dt.listeners.BlockListener;
import eu.phiwa.dt.listeners.EntityListener;
import eu.phiwa.dt.listeners.PlayerListener;
import eu.phiwa.dt.modules.MountingScheduler;
import eu.phiwa.dt.payment.ChargeType;
import eu.phiwa.dt.payment.PaymentManager;


public class DragonTravelMain extends JavaPlugin {
	public static DragonTravelMain plugin;

	// Commands
	public CustomCommandsManager commands;
	public CommandHelpTopic help;

	// Listeners
	private EntityListener entityListener;
	private PlayerListener playerListener;
	private BlockListener blocklistener;
	private FlightEditor flighteditor;

	// Config
	public static final double configVersion = 0.2;
	public static Config config;

	public static File databaseFolder;

	public static Messages messagesHandler;
	public static StationsDB dbStationsHandler;
	public static HomesDB dbHomesHandler;
	public static FlightsDB dbFlightsHandler;

	// Hashmaps
	public static HashMap<Player, RyeDragon> listofDragonriders = new HashMap<Player, RyeDragon>();
	public static HashMap<Player, Location> listofDragonsridersStartingpoints = new HashMap<Player, Location>();
	public static HashMap<Block, Block> globalwaypointmarkers = new HashMap<Block, Block>();
	public static HashMap<String, Boolean> ptogglers = new HashMap<String, Boolean>();

	// Payment (Costs are directly read from the config/sign on-the-fly)
	public PaymentManager paymentManager;

	@Override
	public void onLoad() {
		ConfigurationSerialization.registerClass(Home.class);
		ConfigurationSerialization.registerClass(Station.class);
		ConfigurationSerialization.registerClass(Flight.class);

		commands = new CustomCommandsManager();

		final CommandsManagerRegistration cmdRegister = new CommandsManagerRegistration(this, commands);
		cmdRegister.register(DragonTravelCommands.DragonTravelParentCommand.class);
	}

	public static class CustomCommandsManager extends BukkitCommandsManager {
		public Map<String, Method> getSubcommandMethods(String rootCommand) {
			Method m = this.commands.get(null).get(rootCommand);
			return this.commands.get(m);
		}
	}

	@Override
	public void onEnable() {
		PluginManager pm = getServer().getPluginManager();
		plugin = this;

		// Initialize Config
		config = new Config(this);
		if (!config.loadConfig()) {
			severe("Could not initialize config! Please fix the problems described above and restart the server.");
			pm.disablePlugin(this);
			return;
		}

		// Add the new entity to Minecraft's (Craftbukkit's) entities
		// Returns false if plugin disabled
		if (!registerEntity()) {
			pm.disablePlugin(this);
			return;
		}

		// Register EventListener
		entityListener = new EntityListener(this);
		playerListener = new PlayerListener(this);
		flighteditor = new FlightEditor();
		blocklistener = new BlockListener(this);

		pm.registerEvents(playerListener, this);
		pm.registerEvents(entityListener, this);
		pm.registerEvents(flighteditor, this);
		pm.registerEvents(blocklistener, this);

		databaseFolder = new File(plugin.getDataFolder(), "databases");
		if (!(databaseFolder.exists())) {
			databaseFolder.mkdirs();
		}

		// Messages-file
		messagesHandler = new Messages(this);
		if (!messagesHandler.loadMessages()) {
			pm.disablePlugin(this);
			return;
		}

		// StationsDB
		dbStationsHandler = new StationsDB(this);
		dbStationsHandler.init();

		// HomesDB
		dbHomesHandler = new HomesDB(this);
		dbHomesHandler.init();

		// StationsDB
		dbFlightsHandler = new FlightsDB(this);
		dbFlightsHandler.init();

		CheatProtectionHandler.setup();

		paymentManager = new PaymentManager(getServer());

		log("Payment set up using '%s'.", paymentManager.handler.toString());

		getServer().getHelpMap().addTopic((help = new CommandHelpTopic("DragonTravel")));
		getServer().getHelpMap().addTopic(new CommandHelpTopic("/dt"));

		// MoutingScheduler
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new MountingScheduler(), 60L, 30L);
	}

	@Override
	public void onDisable() {
		log("-----------------------------------------------");
		log("Disabled &a%s %s", getDescription().getName(), getDescription().getVersion());
		log("-----------------------------------------------");
	}

	private ConsoleCommandSender console;
	public void log(String str) {
		if (console == null && getServer().getConsoleSender() != null) {
			// setup
			console = getServer().getConsoleSender();
		}

		if (console != null) {
			console.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b[DragonTravel]&f " + str));
		} else {
			getLogger().info(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', str)));
		}
	}

	public void log(String format, Object... args) {
		log(String.format(format, args));
	}

	public void severe(String msg) {
		log(ChatColor.RED + msg);
	}

	public void warning(String msg) {
		log(ChatColor.YELLOW + msg);
	}

	public void good(String msg) {
		log(ChatColor.GREEN + msg);
	}

	public void info(String msg) {
		log(ChatColor.AQUA + msg);
	}

	private boolean registerEntity() {
		Class<?>[] paramTypes = new Class[] {Class.class, String.class, int.class };
		try {
			Method method = EntityTypes.class.getDeclaredMethod("a", paramTypes);
			method.setAccessible(true);
			method.invoke(null, RyeDragon.class, "RyeDragon", 63);
			return true;
		} catch (Exception e) {
			// MCPC+ compatibility
			// Forge Dev environment; names are not translated into func_foo
			try {
				Method method = EntityTypes.class.getDeclaredMethod("addMapping", paramTypes);
				method.setAccessible(true);
				method.invoke(null, RyeDragon.class, "RyeDragon", 63);
				return true;
			} catch (Exception ex) {
				e.addSuppressed(ex);
			}
			// Production environment: search for the method
			// This is required because the seargenames could change
			// LAST CHECKED FOR VERSION 1.6.4
			try {
				for (Method method : EntityTypes.class.getDeclaredMethods()) {
					if (Arrays.equals(paramTypes, method.getParameterTypes())) {
						method.invoke(null, RyeDragon.class, "RyeDragon", 63);
						return true;
					}
				}
			} catch (Exception ex) {
				e.addSuppressed(ex);
			}

			severe("Could not register the RyeDragon-entity! Please check for updates to DragonTravel.");
			e.printStackTrace();
			return false;
		}
	}


	public void reload() {
		warning("Reloading all files.");
		warning("WE RECOMMEND NOT TO DO THIS BECAUSE IT MIGHT CAUSE SERIUOS PROBLEMS!");
		warning("SIMPLY RESTART YOUR SERVER INSTEAD; THAT'S MUCH SAFER!");

		// Drop old data
		Config.config = null;

		// Config
		config = new Config(this);

		if (!config.loadConfig()) {
			severe("Could not initialize config! Disabling the plugin!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		} else {
			info("Config loaded successfully.");
		}

		// Messages-file
		if (!messagesHandler.loadMessages())
			return;

		dbStationsHandler.init();
		dbHomesHandler.init();
		dbFlightsHandler.init();

		good("Successfully reloaded all files.");
	}


	// Some boilerplate
	@Override
	public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {

		try {
			commands.execute(cmd.getName(), args, sender, sender);
		} catch (CommandPermissionsException e) {
			sender.sendMessage(cmd.getPermissionMessage());
		} catch (MissingNestedCommandException e) {
			sender.sendMessage(ChatColor.RED + e.getUsage());
		} catch (CommandUsageException e) {
			sender.sendMessage(ChatColor.RED + e.getMessage());
			sender.sendMessage(ChatColor.RED + e.getUsage());
		} catch (WrappedCommandException e) {
			if (e.getCause() instanceof NumberFormatException) {
				sender.sendMessage(ChatColor.RED + "Number expected, string received instead.");
			} else {
				sender.sendMessage(ChatColor.RED + "An error has occurred. See console.");
				e.printStackTrace();
			}
		} catch (CommandException e) {
			sender.sendMessage(ChatColor.RED + e.getMessage());
		}

		return true;
	}
}
