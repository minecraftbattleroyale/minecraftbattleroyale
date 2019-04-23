package io.github.minecraftbattleroyale;

import com.flowpowered.math.vector.Vector3d;
import io.github.minecraftbattleroyale.core.GameManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.entity.living.player.CooldownTracker;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.projectile.arrow.TippedArrow;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.RideEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameLoadCompleteEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.resourcepack.ResourcePack;
import org.spongepowered.api.resourcepack.ResourcePacks;
import org.spongepowered.api.scheduler.SpongeExecutorService;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@Plugin(id = "mcbr", name = "Minecraft Battle Royale")
public class MinecraftBattleRoyale {
  private GameManager gameManager = new GameManager();
  private SpongeExecutorService scheduler;

  @Listener
  public void onStart(GameLoadCompleteEvent event) {
    scheduler = Sponge.getScheduler().createSyncExecutor(this);
  }

  @Listener
  public void onJoin(ClientConnectionEvent.Join event) throws MalformedURLException, URISyntaxException {
    Player player = event.getTargetEntity();
    player.sendResourcePack(ResourcePacks.fromUriUnchecked(new URL(Sponge.getServer().getDefaultResourcePack().get().getUri().toString() + "?z=" + System.currentTimeMillis()).toURI()));
    player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
    player.offer(Keys.CAN_FLY, true);
    player.offer(Keys.HEALTH, 20.0);
    player.offer(Keys.SATURATION, 20.0);
    player.offer(Keys.FOOD_LEVEL, 20);
    CarriedInventory inventory = player.getInventory();
    inventory.clear();
    inventory.offer(ItemStack.of(ItemTypes.IRON_PICKAXE, 1));
    inventory.offer(ItemStack.of(ItemTypes.IRON_AXE, 1));
    inventory.offer(ItemStack.of(ItemTypes.BLAZE_ROD, 1));
    ItemStack map = ItemStack.of(ItemTypes.FILLED_MAP, 1);
    player.setItemInHand(HandTypes.OFF_HAND, map);
    player.setChestplate(ItemStack.of(ItemTypes.ELYTRA, 1));
  }

  @Listener
  public void onRightClick(InteractItemEvent event, @First Player player) {
    ItemType item = event.getItemStack().getType();
    //event.getItemStack()
    System.out.println(item);
    // Use iron axe right now
    CooldownTracker cooldownTracker = player.getCooldownTracker();
    boolean pistol = item.matches(ItemStack.of(ItemTypes.IRON_AXE));
    boolean sniper = item.matches(ItemStack.of(ItemTypes.IRON_PICKAXE));
    if ((pistol || sniper) && !cooldownTracker.hasCooldown(item)) {
      int quantity = event.getItemStack().getQuantity();
      Vector3d position = player.getPosition().add(0, 1.5, 0);
      double yaw = Math.toRadians(player.getHeadRotation().getY() + 90);
      double pitch = Math.toRadians(player.getHeadRotation().getX() + 90);
      Vector3d velocity = new Vector3d(Math.cos(yaw), Math.cos(pitch), Math.sin(yaw)).mul(5);
      position = position.add(velocity.normalize().mul(1.5));
      Entity arrow = player.getWorld().createEntity(EntityTypes.TIPPED_ARROW, position);
      player.getWorld().spawnEntity(arrow);
      arrow.setCreator(player.getUniqueId());
      arrow.setVelocity(velocity);
      for (int i = 0 ; i < 10 ; i++) {
        position = position.add(velocity.normalize());
        player.getWorld().spawnParticles(ParticleEffect.builder().type(ParticleTypes.SNOWBALL).build(), position);
      }
      // if at one, reload gun
      if (quantity == 1) {
        cooldownTracker.setCooldown(item, 50);
        player.getItemInHand(HandTypes.MAIN_HAND).ifPresent(itemStack -> {
          int maxStackSize = itemStack.get(Keys.ITEM_DURABILITY).get();
          itemStack.offer(Keys.ITEM_DURABILITY, 1);
          long startTime = System.currentTimeMillis();
          SpongeExecutorService.SpongeFuture future = scheduler.scheduleAtFixedRate(() -> {
            itemStack.offer(Keys.ITEM_DURABILITY, (int) (maxStackSize * ((double) System.currentTimeMillis() - startTime) / 2500));
          }, 0, 5, TimeUnit.MILLISECONDS);
          scheduler.schedule(() -> {
            future.cancel(true);
            itemStack.offer(Keys.ITEM_DURABILITY, maxStackSize);
            itemStack.setQuantity(125);
          }, 2500, TimeUnit.MILLISECONDS);
        });
      } else {
        player.getItemInHand(HandTypes.MAIN_HAND).ifPresent(itemStack -> itemStack.setQuantity(quantity - 1));
        cooldownTracker.setCooldown(item, 5);
      }
    }
  }


  @Listener
  public void onAirShip(InteractItemEvent event, @First Player player) {
    ItemStackSnapshot item = event.getItemStack();
    boolean pistol = item.getType().matches(ItemStack.of(ItemTypes.BLAZE_ROD));
    if (pistol) {
      player.setLocation(player.getLocation().add(0, 150 - player.getLocation().getY(), 0));
      double yaw = Math.toRadians(player.getHeadRotation().getY() + 90);
      Vector3d looking = new Vector3d(Math.cos(yaw), 0,  Math.sin(yaw));
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
      armorStand.addPassenger(boat);
      boat.addPassenger(player);
      boat.addPassenger(armorStandP);
      scheduler.scheduleAtFixedRate(() -> {
        armorStand.setVelocity(looking);
      }, 0, 125, TimeUnit.MILLISECONDS);
    }
  }

  @Listener
  public void onEject(RideEntityEvent.Dismount event, @First Player player) {
   // event.setCancelled(true);
    CarriedInventory inventory = player.getInventory();
    inventory.clear();
    player.setChestplate(ItemStack.of(ItemTypes.ELYTRA, 1));
    scheduler.schedule(() -> {
      //event.getTargetEntity().removePassenger(player);
      player.setLocation(player.getLocation().add(0, -2, 0));
      player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
      player.offer(Keys.IS_ELYTRA_FLYING, true);
      player.offer(Keys.FLYING_SPEED, 0.1);
      CarriedInventory inventoryA = player.getInventory();
      inventoryA.clear();
      player.setChestplate(ItemStack.of(ItemTypes.ELYTRA, 1));
    }, 250, TimeUnit.MILLISECONDS);
  }

  @Listener
  public void onEject(MoveEntityEvent event, @First Player player) {
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
    } else {
      player.setChestplate(null);
      // todo switch player to playing mode
    }
  }
}
