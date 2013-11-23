package eu.phiwa.dt.payment;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import eu.phiwa.dt.DragonTravelMain;
import eu.phiwa.dt.filehandlers.Config;

public class EconomyPaymentHandler implements PaymentHandler {
	private Economy economyProvider;

	public EconomyPaymentHandler() { }

	@Override
	public boolean setup() {
		if (DragonTravelMain.byEconomy == false) {
			// Don't try
			return false;
		}

		RegisteredServiceProvider<Economy> economyRSP = DragonTravelMain.plugin.getServer().getServicesManager().getRegistration(Economy.class);
		if (economyRSP != null) {
			economyProvider = economyRSP.getProvider();
			return true;
		}
		DragonTravelMain.logger.severe("You enabled economy in the config, but DragonTravel could not find a Vault economy provider. :(");
		return false;
	}

	@Override
	public String toString() {
		return ChatColor.GREEN + "Vault";
	}

	@Override
	public boolean chargePlayer(ChargeType type, Player player) {

		if (type.hasNoCostPermission(player)) {
			player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.Payment.Free"));
			return true;
		}

		double amount;
		switch (type) {
		case TRAVEL_TOSTATION:
			amount = Config.config.getDouble("Payment.Economy.Prices.toStation");
			break;
		case TRAVEL_TORANDOM:
			amount = Config.config.getDouble("Payment.Economy.Prices.toRandom");
			break;
		case TRAVEL_TOPLAYER:
			amount = Config.config.getDouble("Payment.Economy.Prices.toPlayer");
			break;
		case TRAVEL_TOCOORDINATES:
			amount = Config.config.getDouble("Payment.Economy.Prices.toCoordinates");
			break;
		case TRAVEL_TOHOME:
			amount = Config.config.getDouble("Payment.Economy.Prices.toHome");
			break;
		case TRAVEL_TOFACTIONHOME:
			amount = Config.config.getDouble("Payment.Economy.Prices.toFactionhome");
			break;
		case SETHOME:
			amount = Config.config.getDouble("Payment.Economy.Prices.setHome");
			break;
		case FLIGHT:
			amount = Config.config.getDouble("Payment.Economy.Prices.Flight");
			break;
		default:
			throw new UnsupportedOperationException("EconomyPaymentHandler doesn't know how to deal with a ChargeType of " + type.name() + ". Fix immediately!");
		}

		return subtractBalance(player, amount);
	}

	@Override
	public boolean chargePlayerExact(ChargeType type, Player player, double customCost) {
		if (type.hasNoCostPermission(player)) {
			player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.Payment.Free"));
			return true;
		}

		return subtractBalance(player, customCost);
	}

	private boolean subtractBalance(Player player, double amount) {
		if (amount == 0.0)
			return true;

		String playerName = player.getName();

		if (!economyProvider.has(playerName, amount)) {
			player.sendMessage(DragonTravelMain.messagesHandler.getMessage("Messages.Economy.Error.NotEnoughMoney"));
			return false;
		} else {
			economyProvider.withdrawPlayer(playerName, amount);

			String message = DragonTravelMain.messagesHandler.getMessage("Messages.Payment.Economy.Successful.WithdrawMessage");
			message = message.replace("{amount}", Double.toString(amount));
			player.sendMessage(message);
			return true;
		}
	}
}
