package eu.phiwa.dt.payment;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
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
		if (!Config.getInstance().usePayment() || !Config.getInstance().useEcon()) {
			return false; // disabled
		}

		RegisteredServiceProvider<Economy> economyRSP = Bukkit.getServicesManager().getRegistration(Economy.class);
		if (economyRSP != null) {
			economyProvider = economyRSP.getProvider();
			return true;
		}
		DragonTravelMain.plugin.severe("You enabled economy in the config, but DragonTravel could not find a Vault economy provider. :(");
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

		double amount = Config.getInstance().getEconPrice(type);

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
