package io.github.minecraftbattleroyale;

import com.flowpowered.math.vector.Vector3d;
import io.github.minecraftbattleroyale.core.GameManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.FallingBlock;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.RideEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.plugin.Plugin;

import java.util.concurrent.TimeUnit;

@Plugin(id = "mcbr", name = "Minecraft Battle Royale")
public class MinecraftBattleRoyale {
  private GameManager gameManager = new GameManager();

  @Listener
  public void onStart(GameStartedServerEvent event) {
    System.out.println(event);

  }

  @Listener
  public void onJoin(ClientConnectionEvent.Join event) {
    Player player = event.getTargetEntity();
    player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
    player.offer(Keys.CAN_FLY, true);
    player.offer(Keys.HEALTH, 20.0);
    player.offer(Keys.SATURATION, 20.0);
    player.offer(Keys.FOOD_LEVEL, 20);
    CarriedInventory inventory = player.getInventory();
    inventory.clear();
    inventory.offer(ItemStack.of(ItemTypes.IRON_PICKAXE, 1));
    inventory.offer(ItemStack.of(ItemTypes.IRON_AXE, 1));
    ItemStack map = ItemStack.of(ItemTypes.FILLED_MAP, 1);
    player.setItemInHand(HandTypes.OFF_HAND, map);
    player.setChestplate(ItemStack.of(ItemTypes.ELYTRA, 1));
  }

  @Listener
  public void onRightClick(InteractItemEvent event, @First Player player) {
    ItemStackSnapshot item = event.getItemStack();
    System.out.println(item);
    // Use iron axe right now
    boolean pistol = item.getType().matches(ItemStack.of(ItemTypes.IRON_AXE));
    boolean sniper = item.getType().matches(ItemStack.of(ItemTypes.IRON_PICKAXE));
    if (pistol || sniper) {
      Vector3d positon = player.getPosition();
      double yaw = Math.toRadians(player.getHeadRotation().getY() + 90);
      double pitch = Math.toRadians(player.getHeadRotation().getX() + 90);
      double x = 1.25 * Math.cos(yaw);
      double y = 1.25 * Math.sin(yaw);
      Entity arrow = player.getWorld().createEntity(EntityTypes.TIPPED_ARROW, positon.add(x, 1.62, y));
      System.out.println(arrow);
      player.getWorld().spawnEntity(arrow);
      Vector3d velocity = new Vector3d(x, Math.cos(pitch), y).mul(5);
      arrow.setVelocity(velocity);
    }
  }


  @Listener
  public void onAirShip(InteractItemEvent event, @First Player player) {
    ItemStackSnapshot item = event.getItemStack();
    System.out.println(item);
    boolean pistol = item.getType().matches(ItemStack.of(ItemTypes.BLAZE_ROD));
    if (pistol) {
      ArmorStand armorStand = (ArmorStand) player.getWorld().createEntity(EntityTypes.ARMOR_STAND, player.getPosition());
      player.getWorld().spawnEntity(armorStand);
      Boat boat = (Boat) player.getWorld().createEntity(EntityTypes.BOAT, player.getPosition());
      boat.offer(Keys.INVISIBLE, true);
      player.getWorld().spawnEntity(boat);
      ArmorStand armorStandP = (ArmorStand) player.getWorld().createEntity(EntityTypes.ARMOR_STAND, player.getPosition());
      player.getWorld().spawnEntity(armorStandP);
      armorStandP.setHelmet(ItemStack.builder().itemType(ItemTypes.HAY_BLOCK).build());
      armorStandP.offer(Keys.INVISIBLE, true);
      armorStand.offer(Keys.INVISIBLE, true);
      //armorStand.offer(Keys.HAS_GRAVITY, false);
      armorStand.addPassenger(boat);
      boat.addPassenger(player);
      boat.addPassenger(armorStandP);
      double yaw = Math.toRadians(player.getHeadRotation().getY() + 90);
      double x = 1.25 * Math.cos(yaw);
      double y = 1.25 * Math.sin(yaw);
      Sponge.getScheduler().createSyncExecutor(this).scheduleAtFixedRate(() -> {
        armorStand.setVelocity(new Vector3d(x, 0, y));

      }, 0, 125, TimeUnit.MILLISECONDS);
      //armorStand.setVelocity(new Vector3d(x, 0, y).mul(20));
    }
  }

  @Listener
  public void onEject(RideEntityEvent.Dismount event, @First Player player) {
    player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
    player.offer(Keys.CAN_FLY, false);
    player.offer(Keys.HEALTH, 40.0);
    player.offer(Keys.IS_ELYTRA_FLYING, true);
    CarriedInventory inventory = player.getInventory();
    inventory.clear();
    player.setChestplate(ItemStack.of(ItemTypes.ELYTRA, 1));
  }

  @Listener
  public void onEject(MoveEntityEvent event, @First Player player) {
    // after lobby game state
//    if (!player.get(Keys.IS_ELYTRA_FLYING).get()) {
//
//    }

  }
}
