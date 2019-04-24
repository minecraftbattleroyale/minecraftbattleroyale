package io.github.minecraftbattleroyale;

import com.flowpowered.math.vector.Vector3d;
import io.github.minecraftbattleroyale.core.UserPlayer;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class Airship {
  private ArmorStand engine;
  private ArmorStand shipHolder;
  private Boat shipRider;

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


  public void ride(UserPlayer userPlayer) {
    shipRider.addPassenger(userPlayer.getPlayer());

//    scheduler.scheduleAtFixedRate(() -> {
//      armorStand.setVelocity(looking);
//    }, 0, 125, TimeUnit.MILLISECONDS);
  }


}
