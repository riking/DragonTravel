package eu.phiwa.dt.payment;

import org.bukkit.entity.Player;

public enum ChargeType {
	TRAVEL_TOSTATION("dt.nocost.travel", "toStation"),
	TRAVEL_TORANDOM("dt.nocost.randomtravel", "toRandom"),
	TRAVEL_TOPLAYER("dt.nocost.ptravel", "toPlayer"),
	TRAVEL_TOCOORDINATES("dt.nocost.ctravel", "toCoordinates"),
	TRAVEL_TOHOME("dt.nocost.home", "toHome"),
	TRAVEL_TOFACTIONHOME("dt.nocost.fhome", "toFactionhome"),
	FLIGHT("dt.nocost.flight", "Flight"),
	SETHOME("dt.nocost.home.set", "setHome"),
	;

	private String noCostPermission;
	private String configString;

	private ChargeType(String noCostPermission, String configString) {
		this.noCostPermission = noCostPermission;
		this.configString = configString;
	}

	public boolean hasNoCostPermission(Player player) {
		return player.hasPermission(noCostPermission);
	}

	public String getConfigString() {
		return configString;
	}
}
