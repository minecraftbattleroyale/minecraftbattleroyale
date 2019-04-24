package io.github.minecraftbattleroyale;

import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.github.minecraftbattleroyale.commands.StartCommand;
import io.github.minecraftbattleroyale.core.ArenaGame;
import org.spongepowered.api.Game;
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
import org.spongepowered.api.event.EventManager;
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
import org.spongepowered.api.resourcepack.ResourcePacks;
import org.spongepowered.api.scheduler.SpongeExecutorService;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@Plugin(id = "mcbr", name = "Minecraft Battle Royale")
public class MinecraftBattleRoyale {
  @Inject
  private Injector injector;
  @Inject
  private EventManager eventManager;
  @Inject
  private Game game;
  private SpongeExecutorService scheduler;
  private ArenaGame arenaGame = new ArenaGame();

  /** This will get the current game */
  public ArenaGame getCurrentGame() {
    return arenaGame;
  }

  /** Handle logic that register's stuff */
  @Listener
  public void onStart(GameLoadCompleteEvent event) {
    scheduler = Sponge.getScheduler().createSyncExecutor(this);
    StartCommand.register(this);
    getCurrentGame().setWorld(Sponge.getServer().getWorld(Sponge.getServer().getDefaultWorldName()).get());
  }

  @Listener
  public void onJoin(ClientConnectionEvent.Join event) {
    getCurrentGame().joinGame(event.getTargetEntity());
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
