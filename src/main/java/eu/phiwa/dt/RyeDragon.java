package eu.phiwa.dt;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.v1_6_R3.AxisAlignedBB;
import net.minecraft.server.v1_6_R3.Block;
import net.minecraft.server.v1_6_R3.CrashReport;
import net.minecraft.server.v1_6_R3.CrashReportSystemDetails;
import net.minecraft.server.v1_6_R3.EntityHuman;
import net.minecraft.server.v1_6_R3.MathHelper;
import net.minecraft.server.v1_6_R3.ReportedException;
import net.minecraft.server.v1_6_R3.EntityEnderDragon;
import net.minecraft.server.v1_6_R3.EntityLiving;
import net.minecraft.server.v1_6_R3.World;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import eu.phiwa.dt.filehandlers.Config;
import eu.phiwa.dt.flights.Waypoint;
import eu.phiwa.dt.modules.DragonManagement;

public class RyeDragon extends EntityEnderDragon {

	// Travel
	boolean isTravel = false;

	private double toX;
	private double toY;
	private double toZ;
	private org.bukkit.World toWorld;
	private int travelY;

	private Location destlocOtherworld;


	// Flight
	boolean isFlight = false;

	private List<Waypoint> waypoints = new ArrayList<Waypoint>();
	private Waypoint nextWaypoint;
	private int currentindexWaypoint = 0;
	private int numberOfWaypoints;

	private boolean finalmove = false;
	private boolean move = false;

	// Free travel
	boolean isFreeFlight = false;

	// Amount to fly up/down during a flight/travel
	private double XperTick;
	private double YperTick;
	private double ZperTick;

	// Distance to the right coords
	private double distanceX;
	private double distanceY;
	private double distanceZ;

	// Start points for tick calculation
	private double startX;
	private double startY;
	private double startZ;

	Location start;
	Location spawnOtherWorld;
	Entity bukkitDragon;
	Player bukkitRider;


	public RyeDragon(Location loc, World notchWorld) {

		super(notchWorld);

		this.start = loc;
		setPosition(loc.getX(), loc.getY(), loc.getZ());
		yaw = loc.getYaw() + 180;
		while (yaw > 360)
			yaw -= 360;
		while (yaw < 0)
			yaw += 360;
		if (yaw < 45 || yaw > 315)
			yaw = 0F;
		else if (yaw < 135)
			yaw = 90F;
		else if (yaw < 225)
			yaw = 180F;
		else
			yaw = 270F;
	}

	public RyeDragon(World world) {
		super(world);
	}

	public Entity getEntity() {
		if (bukkitEntity != null)
			return bukkitEntity;
		else
			return bukkitDragon;
	}


	/**
	 * Starts a travel to the specified location
	 *
	 * @param destinationLoc Location to start a travel to
	 */
	public void startTravel(Location destinationLoc, Boolean interworld) {

		if (interworld) {
			toX = locX + 20;
			toY = locY + 10;
			toZ = locZ + 20;
			toWorld = destinationLoc.getWorld();
			destlocOtherworld = destinationLoc.clone();

			this.startX = start.getX();
			this.startY = start.getY();
			this.startZ = start.getZ();

			travelY = (int) toY;

			this.yaw = getCorrectYaw(toX, toZ);
		} else {
			toX = destinationLoc.getBlockX();
			toY = destinationLoc.getBlockY();
			toZ = destinationLoc.getBlockZ();
			toWorld = destinationLoc.getWorld();

			this.startX = start.getX();
			this.startY = start.getY();
			this.startZ = start.getZ();

			travelY = Config.config.getInt("TravelHeight");
		}

		this.bukkitDragon = this.getBukkitEntity();
		this.bukkitRider = (Player) this.passenger.getBukkitEntity();

		isTravel = true;
		move = true;

		setMoveTravel();
	}

	/**
	 * Sets the x,z move for each tick
	 */
	public void setMoveTravel() {

		this.distanceX = this.startX - toX;
		this.distanceY = this.startY - toY;
		this.distanceZ = this.startZ - toZ;

		double tick = Math.sqrt((distanceX * distanceX) + (distanceY * distanceY) + (distanceZ * distanceZ)) / DragonTravelMain.speed;
		XperTick = Math.abs(distanceX) / tick;
		ZperTick = Math.abs(distanceZ) / tick;
	}

	/**
	 * Normal Travel
	 */
	public void travel() {

		// Returns if the dragon won't move
		if (!move)
			return;

		if (bukkitDragon.getPassenger() == null)
			return;

		double myX = locX;
		double myY = locY;
		double myZ = locZ;

		if (finalmove) {

			// Flying down/up at the end
			if ((int) locY > (int) toY)
				myY -= DragonTravelMain.speed;
			else if ((int) locY < (int) toY)
				myY += DragonTravelMain.speed;

			// Removing entity
			else {

				// Interworld-travel teleport
				if (!bukkitDragon.getWorld().equals(toWorld)) {
					this.bukkitRider = (Player) bukkitDragon.getPassenger();

					spawnOtherWorld = destlocOtherworld.clone();
					spawnOtherWorld.setX(destlocOtherworld.getX() + 80);
					spawnOtherWorld.setY(destlocOtherworld.getY() + 80);
					spawnOtherWorld.setZ(destlocOtherworld.getZ() + 80);
					spawnOtherWorld.getChunk().load();

					Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("DragonTravel"), new Runnable() {
						public void run() {
							DragonManagement.dismount(bukkitRider, true);
							bukkitRider.setFlying(true);
							if (spawnOtherWorld.getZ() < destlocOtherworld.getZ())
								spawnOtherWorld.setYaw((float) (-Math.toDegrees(Math.atan((spawnOtherWorld.getX() - destlocOtherworld.getX()) / (spawnOtherWorld.getZ() - destlocOtherworld.getZ())))));
							else if (spawnOtherWorld.getZ() > destlocOtherworld.getZ())
								spawnOtherWorld.setYaw((float) (-Math.toDegrees(Math.atan((spawnOtherWorld.getX() - destlocOtherworld.getX()) / (spawnOtherWorld.getZ() - destlocOtherworld.getZ())))) + 180.0F);
							bukkitRider.teleport(spawnOtherWorld);
							if (!DragonManagement.mount(bukkitRider))
								return;
							if (!DragonTravelMain.listofDragonriders.containsKey(bukkitRider))
								return;
							bukkitRider.setFlying(false);
							RyeDragon dragon = DragonTravelMain.listofDragonriders.get(bukkitRider);
							dragon.startTravel(destlocOtherworld, false);
							bukkitDragon.remove();
						}
					}, 1L);

					// Dismount
				} else {
					DragonManagement.removeRiderandDragon(bukkitDragon, true);
					return;
				}
			}

			setPosition(myX, myY, myZ);
			return;
		}

		// Getting the correct height
		if ((int) locY < travelY)
			myY += DragonTravelMain.speed;

		if (myX < toX)
			myX += XperTick;
		else
			myX -= XperTick;

		if (myZ < toZ)
			myZ += ZperTick;
		else
			myZ -= ZperTick;

		if ((int) myZ == (int) toZ && ((int) myX == (int) toX || (int) myX == (int) toX + 1 || (int) myX == (int) toX - 1)) {
			finalmove = true;
		}

		setPosition(myX, myY, myZ);
	}


	/**
	 * Starts the specified flight
	 *
	 * @param flight Flight to start
	 */
	public void startFlight(Flight flight) {
		this.bukkitDragon = this.getBukkitEntity();
		this.bukkitRider = (Player) this.passenger.getBukkitEntity();

		this.waypoints = flight.waypoints;
		this.numberOfWaypoints = waypoints.size();
		this.nextWaypoint = waypoints.get(currentindexWaypoint);
		this.currentindexWaypoint++;

		this.startX = start.getX();
		this.startY = start.getY();
		this.startZ = start.getZ();

		this.move = true;
		this.isFlight = true;

		setMoveFlight();
	}

	/**
	 * Sets the x,y,z move for each tick
	 */
	public void setMoveFlight() {

		this.distanceX = this.startX - nextWaypoint.x;
		this.distanceY = this.startY - nextWaypoint.y;
		this.distanceZ = this.startZ - nextWaypoint.z;

		double tick = Math.sqrt((distanceX * distanceX) + (distanceY * distanceY) + (distanceZ * distanceZ)) / DragonTravelMain.speed;

		this.XperTick = Math.abs(distanceX) / tick;
		this.YperTick = Math.abs(distanceY) / tick;
		this.ZperTick = Math.abs(distanceZ) / tick;
	}

	/**
	 * Controls the dragon
	 */
	public void flight() {

		// Returns, the dragon won't move
		if (!move)
			return;

		// Initialize variables for current coordinates
		// locX/loY/locZ are variables extended by EntityEnderDragons > LivingEntity > Entity
		double currentX = locX;
		double currentY = locY;
		double currentZ = locZ;


		if ((int) currentX != nextWaypoint.x) {
			if (currentX < nextWaypoint.x)
				currentX += XperTick;
			else
				currentX -= XperTick;
		}

		if ((int) currentY != nextWaypoint.y) {
			if ((int) currentY < nextWaypoint.y) {
				currentY += YperTick;
			} else {
				currentY -= YperTick;
			}
		}

		if ((int) currentZ != nextWaypoint.z) {
			if (currentZ < nextWaypoint.z)
				currentZ += ZperTick;
			else
				currentZ -= ZperTick;
		}


		/*
		 * >> Reached the last waypoint? << Is the next waypoint the last
		 * one? If yes, did the dragon already reach it? Removing the entity
		 * and dismounting the player
		 */


		/*
		 * >> Reached the next (and not last) waypoint? << The next waypoint
		 * is loaded and the dragon moves towards it
		 */
		if ((Math.abs((int) currentX - nextWaypoint.x) == 0 && Math.abs((int) currentZ - nextWaypoint.z) <= 3) || (Math.abs((int) currentZ - nextWaypoint.z) == 0 && Math.abs((int) currentX - nextWaypoint.x) <= 3) && (Math.abs((int) currentY - nextWaypoint.y) <= 5)) {


			if (currentindexWaypoint == numberOfWaypoints) {
				try {
					DragonManagement.removeRiderandDragon(bukkitDragon, new Location(bukkitDragon.getWorld(), nextWaypoint.x, nextWaypoint.y, nextWaypoint.z, ((Player) bukkitDragon.getPassenger()).getLocation().getYaw(), ((Player) bukkitDragon.getPassenger()).getLocation().getPitch()));
					return;

				} catch (NullPointerException ex) {
					DragonManagement.removeRiderandDragon(bukkitDragon, new Location(bukkitDragon.getWorld(), nextWaypoint.x, nextWaypoint.y, nextWaypoint.z));
					return;
				}
			}


			this.nextWaypoint = waypoints.get(currentindexWaypoint);
			this.currentindexWaypoint++;

			// Get the dragons position and set it as start-location for the flight to the next waypoint.
			this.startX = locX;
			this.startY = locY;
			this.startZ = locZ;

			this.yaw = getCorrectYaw(nextWaypoint.x, nextWaypoint.z);

			setMoveFlight();

			return;
		}

		setPosition(currentX, currentY, currentZ);
	}

	public void startFreeFlight() {
		this.bukkitDragon = this.getBukkitEntity();
		this.bukkitRider = (Player) this.passenger.getBukkitEntity();

		this.isFreeFlight = true;
		this.move = true;

		this.startX = start.getX();
		this.startY = start.getY();
		this.startZ = start.getZ();
	}

	private static Field isJumping = null;
	static {
		try {
			isJumping = net.minecraft.server.v1_6_R3.EntityLiving.class.getDeclaredField("bd");
			isJumping.setAccessible(true);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private static final double SPEED_CAP = 1D;
	private static final double BACKWARDS_SPEED_CAP = 0.1D;

	public void free() {
		// Returns if the dragon won't move
		if (!move)
			return;

		// SECTION: SET UP PLAYER INPUT VARIABLES

		boolean requestedSneak = false;
		// Reattach player to the dragon
		if (passenger == null) {
			bukkitDragon.setPassenger(bukkitRider);
			requestedSneak = true;
		}

		net.minecraft.server.v1_6_R3.EntityLiving playerPassenger = (net.minecraft.server.v1_6_R3.EntityLiving) this.passenger;
		boolean requestedJump = false;
		requestedSneak = requestedSneak || playerPassenger.isSneaking() || playerPassenger.isSprinting();

		if (isJumping != null) {
			try {
				requestedJump = (Boolean) isJumping.get(playerPassenger);
			} catch (Throwable ignored) {
			}
		}
		try {
			System.out.println(String.format("be: %f bf: %f bd: %b sneaking: %b", playerPassenger.be, playerPassenger.bf, isJumping.get(playerPassenger), playerPassenger.isSneaking()));
		} catch (Throwable t) {
			t.printStackTrace();
		}

		// side movement is - for A, + for D
		// forward movement is - for S, + for W
		double requestedAD = playerPassenger.be;
		double requestedWS = playerPassenger.bf;

		// END SECTION: SET UP PLAYER INPUT VARIABLES

		double prevSpeed = Math.sqrt(motX * motX + motZ * motZ);
		double newSpeed = prevSpeed;
		this.yaw -= requestedAD * 3;
		//this.yaw = (float) ((this.yaw * 39F + (passenger.yaw > 180F ? passenger.yaw - 180F : passenger.yaw + 180F)) / 40F);

		if (requestedWS > 0) {
			newSpeed += 0.01D;
			newSpeed *= 1.05D;
			this.yaw = (float) ((this.yaw * 39F + (passenger.yaw > 180F ? passenger.yaw - 180F : passenger.yaw + 180F)) / 40F);
		} else if (requestedWS < 0) {
			newSpeed -= 0.1D;
			newSpeed *= 0.8D;
		} else {
			newSpeed *= 0.985D;
		}

		if (newSpeed > SPEED_CAP) {
			newSpeed = SPEED_CAP;
		} else if (newSpeed < -BACKWARDS_SPEED_CAP) {
			newSpeed = -BACKWARDS_SPEED_CAP;
		} else if (newSpeed > -0.0015 && newSpeed < 0.0015) {
			newSpeed = 0;
		}

		this.motX = newSpeed * -Math.sin((yaw + 180F) * Math.PI / 180F);
		this.motZ = newSpeed * Math.cos((yaw + 180F) * Math.PI / 180F);
		this.motY = (requestedJump ? 0.1D : 0) + (requestedSneak ? -0.1D : 0);

		if (this.onGround) {
			motY += 0.3D;
		}

		this.bounceMove(motX, motY, motZ);

		// ACTUALLY MOVING
		AxisAlignedBB original = this.boundingBox;
		this.boundingBox.shrink(3D, 1D, 3D);
		this.move(motX, motY, motZ);
		this.boundingBox.grow(3D, 1D, 3D);
	}

	private int private_c;
	private void bounceMove(double d0, double d1, double d2) {
		this.X *= 0.4F;
		double d3 = this.locX;
		double d4 = this.locY;
		double d5 = this.locZ;

		if (this.K) {
			this.K = false;
			d0 *= 0.25D;
			d1 *= 0.05000000074505806D;
			d2 *= 0.25D;
			this.motX = 0.0D;
			this.motY = 0.0D;
			this.motZ = 0.0D;
		}

		double d6 = d0;
		double d7 = d1;
		double d8 = d2;
		AxisAlignedBB axisalignedbb = this.boundingBox.clone();
		boolean flag = this.onGround && this.isSneaking() && false;

		if (flag) {
			double d9;

			for (d9 = 0.05D; d0 != 0.0D && this.world.getCubes(this, this.boundingBox.c(d0, -1.0D, 0.0D)).isEmpty(); d6 = d0) {
				if (d0 < d9 && d0 >= -d9) {
					d0 = 0.0D;
				} else if (d0 > 0.0D) {
					d0 -= d9;
				} else {
					d0 += d9;
				}
			}

			for (; d2 != 0.0D && this.world.getCubes(this, this.boundingBox.c(0.0D, -1.0D, d2)).isEmpty(); d8 = d2) {
				if (d2 < d9 && d2 >= -d9) {
					d2 = 0.0D;
				} else if (d2 > 0.0D) {
					d2 -= d9;
				} else {
					d2 += d9;
				}
			}

			while (d0 != 0.0D && d2 != 0.0D && this.world.getCubes(this, this.boundingBox.c(d0, -1.0D, d2)).isEmpty()) {
				if (d0 < d9 && d0 >= -d9) {
					d0 = 0.0D;
				} else if (d0 > 0.0D) {
					d0 -= d9;
				} else {
					d0 += d9;
				}

				if (d2 < d9 && d2 >= -d9) {
					d2 = 0.0D;
				} else if (d2 > 0.0D) {
					d2 -= d9;
				} else {
					d2 += d9;
				}

				d6 = d0;
				d8 = d2;
			}
		}

		List list = this.world.getCubes(this, this.boundingBox.a(d0, d1, d2));

		for (int i = 0; i < list.size(); ++i) {
			d1 = ((AxisAlignedBB) list.get(i)).b(this.boundingBox, d1);
		}

		this.boundingBox.d(0.0D, d1, 0.0D);
		if (!this.L && d7 != d1) {
			d2 = 0.0D;
			d1 = 0.0D;
			d0 = 0.0D;
		}

		boolean flag1 = this.onGround || d7 != d1 && d7 < 0.0D;

		int j;

		for (j = 0; j < list.size(); ++j) {
			d0 = ((AxisAlignedBB) list.get(j)).a(this.boundingBox, d0);
		}

		this.boundingBox.d(d0, 0.0D, 0.0D);
		if (!this.L && d6 != d0) {
			d2 = 0.0D;
			d1 = 0.0D;
			d0 = 0.0D;
		}

		for (j = 0; j < list.size(); ++j) {
			d2 = ((AxisAlignedBB) list.get(j)).c(this.boundingBox, d2);
		}

		this.boundingBox.d(0.0D, 0.0D, d2);
		if (!this.L && d8 != d2) {
			d2 = 0.0D;
			d1 = 0.0D;
			d0 = 0.0D;
		}

		double d10;
		double d11;
		double d12;
		int k;

		if (this.Y > 0.0F && flag1 && (flag || this.X < 0.05F) && (d6 != d0 || d8 != d2)) {
			d10 = d0;
			d11 = d1;
			d12 = d2;
			d0 = d6;
			d1 = (double) this.Y;
			d2 = d8;
			AxisAlignedBB axisalignedbb1 = this.boundingBox.clone();

			this.boundingBox.d(axisalignedbb);
			list = this.world.getCubes(this, this.boundingBox.a(d6, d1, d8));

			for (k = 0; k < list.size(); ++k) {
				d1 = ((AxisAlignedBB) list.get(k)).b(this.boundingBox, d1);
			}

			this.boundingBox.d(0.0D, d1, 0.0D);
			if (!this.L && d7 != d1) {
				d2 = 0.0D;
				d1 = 0.0D;
				d0 = 0.0D;
			}

			for (k = 0; k < list.size(); ++k) {
				d0 = ((AxisAlignedBB) list.get(k)).a(this.boundingBox, d0);
			}

			this.boundingBox.d(d0, 0.0D, 0.0D);
			if (!this.L && d6 != d0) {
				d2 = 0.0D;
				d1 = 0.0D;
				d0 = 0.0D;
			}

			for (k = 0; k < list.size(); ++k) {
				d2 = ((AxisAlignedBB) list.get(k)).c(this.boundingBox, d2);
			}

			this.boundingBox.d(0.0D, 0.0D, d2);
			if (!this.L && d8 != d2) {
				d2 = 0.0D;
				d1 = 0.0D;
				d0 = 0.0D;
			}

			if (!this.L && d7 != d1) {
				d2 = 0.0D;
				d1 = 0.0D;
				d0 = 0.0D;
			} else {
				d1 = (double) (-this.Y);

				for (k = 0; k < list.size(); ++k) {
					d1 = ((AxisAlignedBB) list.get(k)).b(this.boundingBox, d1);
				}

				this.boundingBox.d(0.0D, d1, 0.0D);
			}

			if (d10 * d10 + d12 * d12 >= d0 * d0 + d2 * d2) {
				d0 = d10;
				d1 = d11;
				d2 = d12;
				this.boundingBox.d(axisalignedbb1);
			}
		}

		//this.locX = (this.boundingBox.a + this.boundingBox.d) / 2.0D;
		//this.locY = this.boundingBox.b + (double) this.height - (double) this.X;
		//this.locZ = (this.boundingBox.c + this.boundingBox.f) / 2.0D;
		this.positionChanged = d6 != d0 || d8 != d2;
		this.H = d7 != d1;
		this.onGround = d7 != d1 && d7 < 0.0D;
		this.I = this.positionChanged || this.H;
		this.a(d1, this.onGround);
		// Attempt at the bounce change
		if (d6 != d0) {
			this.motX = -this.motX;
		}

		if (d7 != d1) {
			this.motY = -this.motY;
		}

		if (d8 != d2) {
			this.motZ = -this.motZ;
		}

		d10 = this.locX - d3;
		d11 = this.locY - d4;
		d12 = this.locZ - d5;

	}
	/**
	 * Gets the correct yaw for this specific path
	 */

	private float getCorrectYaw(double targetx, double targetz) {

		if (this.locZ > targetz)
			return (float) (-Math.toDegrees(Math.atan((this.locX - targetx) / (this.locZ - targetz))));
		else if (this.locZ < targetz)
			return (float) (-Math.toDegrees(Math.atan((this.locX - targetx) / (this.locZ - targetz)))) + 180.0F;
		else
			return this.yaw;
	}

	/**
	 * This method is a natural method of the Enderdragon extended by the
	 * RyeDragon. It's fired when the dragon moves and fires the
	 * travel-method again to keep the dragon flying.
	 *
	 */
	@Override
	public void c() {

		if (bukkitDragon != null && bukkitRider != null) {
			if (bukkitDragon.getPassenger() != null) {
				bukkitDragon.setPassenger(bukkitRider);
				// TODO why?
			}
		}

		// Travel
		if (isTravel) {
			travel();
			return;
		}

		// Flight
		if (isFlight) {
			flight();
			return;
		}

		// Free flight
		if (isFreeFlight) {
			free();
			return;
		}
	}

	/*
	 * public double x_() { return 3; }
	 */
}
