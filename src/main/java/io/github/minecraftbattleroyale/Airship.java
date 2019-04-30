package io.github.minecraftbattleroyale;

import com.flowpowered.math.vector.Vector3d;
import io.github.minecraftbattleroyale.core.UserPlayer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.RideEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/** This is the air ship that will fly across the sky */
public class Airship implements Runnable {
  private static final double YAW = 90;
  private ArmorStand engine;
  private ArmorStand shipHolder;
  private Boat shipRider;
  private Vector3d stopLocation;
  private SpongeExecutorService scheduler;
  private Player player;
  private SpongeExecutorService.SpongeFuture future;

  public Airship(Vector3d stopLocation) {
    this.stopLocation = stopLocation;
    this.scheduler = MinecraftBattleRoyale.get().syncScheduler();
  }

  /** Spawn the ship at the given world location */
  public void spawnShip(Location<World> worldLocation) {
    engine = (ArmorStand) worldLocation.createEntity(EntityTypes.ARMOR_STAND);
    worldLocation.spawnEntity(engine);
    shipRider = (Boat) worldLocation.createEntity(EntityTypes.BOAT);
    worldLocation.spawnEntity(shipRider);
    shipHolder = (ArmorStand) worldLocation.createEntity(EntityTypes.ARMOR_STAND);
    worldLocation.spawnEntity(shipHolder);
    shipHolder.setHelmet(ItemStack.builder().itemType(ItemTypes.STRUCTURE_VOID).build());
    shipHolder.offer(Keys.INVISIBLE, true);
    engine.offer(Keys.INVISIBLE, true);
    engine.addPassenger(shipRider);
    shipRider.addPassenger(shipHolder);
  }

  /** The entites used to create this air ship */
  private Collection<Entity> entites() {
    return Arrays.asList(this.engine, this.shipRider, this.shipHolder);
  }

  /** Have the player ride the entity */
  public void ride(UserPlayer userPlayer) {
    Player player = userPlayer.getPlayer();
    shipRider.addPassenger(player);
    this.player = player;
    future = scheduler.scheduleAtFixedRate(this, 0,125, TimeUnit.MILLISECONDS);
  }



  /** Remove the airship */
  public void cleanUp() {
    System.out.println("Cleaning up the air ship");
    // player is still on board
    if (shipRider.getPassengers().size() > 1) {
      eject(player);
    }
    Sponge.getEventManager().unregisterListeners(this);
    entites().forEach(Entity::remove);
    future.cancel(false);
  }

  public Runnable eject(Player player) {
    System.out.println("Ejecting the player from the air ship");
    return () -> {
      player.setLocation(player.getLocation().add(0, -2, 0));
      player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
      player.offer(Keys.IS_ELYTRA_FLYING, true);
      player.offer(Keys.FLYING_SPEED, 0.1);
      CarriedInventory inventoryA = player.getInventory();
      inventoryA.clear();
      player.setChestplate(ItemStack.of(ItemTypes.ELYTRA, 1));
    };
  }

  @Override
  public void run() {
    // todo figure this out so the ships can remove them selves
    Vector3d looking = new Vector3d(Math.cos(YAW), 0,  Math.sin(YAW));
    engine.setVelocity(looking);
    if (engine.getLocation().getPosition().distance(stopLocation) < 5) {
      System.out.println("Near the stop location running clean up");
      cleanUp();
    }
  }

  /** They have jumped from the air ship so switch the player to this mode */
  @Listener
  public void onEject(RideEntityEvent.Dismount event, @First Player player) {
    if (this.player == null || !this.player.equals(player)) {
      return; // do nothing for this player as its not the rider
    }
    CarriedInventory inventory = player.getInventory();
    inventory.clear();
    player.setChestplate(ItemStack.of(ItemTypes.ELYTRA, 1));
    scheduler.schedule(eject(player), 250, TimeUnit.MILLISECONDS);
  }

  /** Prevents the player from falling to fast */
  @Listener
  public void onFlight(MoveEntityEvent event, @First Player player) {
    if (this.player == null || !this.player.equals(player)) {
      return; // do nothing for this player as its not the rider
    }
    // after lobby game state
    if (player.get(Keys.IS_ELYTRA_FLYING).get()) {
      player.spawnParticles(ParticleEffect.builder().type(ParticleTypes.SNOWBALL).build(), player.getPosition());
      double maxFlightVelocity = -0.45;
      if (player.getVelocity().getX() < maxFlightVelocity) {
        player.setVelocity(new Vector3d(maxFlightVelocity, player.getVelocity().getY(), player.getVelocity().getZ()));
      }
      if (player.getVelocity().getY() < maxFlightVelocity) {
        player.setVelocity(new Vector3d(player.getVelocity().getX(), maxFlightVelocity, player.getVelocity().getZ()));
      }
      if (player.getVelocity().getZ() < maxFlightVelocity) {
        player.setVelocity(new Vector3d(player.getVelocity().getX(), player.getVelocity().getY(), maxFlightVelocity));
      }
    }
  }

}
