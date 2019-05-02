package io.github.minecraftbattleroyale;

import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.github.minecraftbattleroyale.commands.CollapseCommand;
import io.github.minecraftbattleroyale.commands.StartCommand;
import io.github.minecraftbattleroyale.commands.WinnerCommand;
import io.github.minecraftbattleroyale.core.ArenaGame;
import io.github.minecraftbattleroyale.core.GameMode;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.CooldownTracker;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameLoadCompleteEvent;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.chunk.UnloadChunkEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.gamerule.DefaultGameRules;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Plugin(id = MinecraftBattleRoyale.ID, name = "Minecraft Battle Royale")
public class MinecraftBattleRoyale {
  public static final String ID = "mcbr";
  @Inject
  private Injector injector;
  @Inject
  private EventManager eventManager;
  @Inject
  private Game game;
  private SpongeExecutorService scheduler;
  private ArenaGame arenaGame = new ArenaGame();
  private final Map<ItemType, Gun> guns = new HashMap<>();
  {
    // todo create the map of the guns
  }

  /** Get the instance of the plugin */
  public static MinecraftBattleRoyale get() {
    return (MinecraftBattleRoyale) Sponge.getPluginManager().getPlugin(MinecraftBattleRoyale.ID).get().getInstance().get();
  }

  /** This will get the current game */
  public ArenaGame getCurrentGame() {
    return this.arenaGame;
  }

  public SpongeExecutorService syncScheduler() {
    return this.scheduler;
  }

  /** Handle logic that register's stuff */
  @Listener
  public void onStart(GameLoadCompleteEvent event) {
    scheduler = Sponge.getScheduler().createSyncExecutor(this);
    StartCommand.register(this);
    CollapseCommand.register(this);
    WinnerCommand.register(this);
    Sponge.getEventManager().registerListeners(this, arenaGame);
    getCurrentGame().setScheduler(scheduler);
  }

  @Listener
  public void onFall(DamageEntityEvent event, @Root DamageSource source, @Getter("getTargetEntity") Player subject) {
    event.setCancelled(source.getType().equals(DamageTypes.FALL) || this.arenaGame.getGameMode() == GameMode.LOBBY);
  }

  /** Set the world for the game when it loads */
  @Listener
  public void onWorldLoad(LoadWorldEvent event) {
    // todo better checks but just use the overworld right now
    final World world = event.getTargetWorld();
    if (world.getDimension().getType().equals(DimensionTypes.OVERWORLD)) {
      WorldProperties properties = world.getProperties();
      properties.setWorldTime(4283);
      properties.setGameRule(DefaultGameRules.DO_DAYLIGHT_CYCLE, "false");
      properties.setGameRule(DefaultGameRules.DO_MOB_SPAWNING, "false");
      properties.setGameRule(DefaultGameRules.DO_WEATHER_CYCLE, "false");
      properties.setGameRule(DefaultGameRules.SPAWN_RADIUS, "1");
      properties.setSpawnPosition(ArenaGame.LOBBY_SPAWN.toInt());
      getCurrentGame().setWorld(world);
      int chunksRadius = 45;
      for (int i = 0 ; i < chunksRadius ; i++) {
        for (int j = 0 ; j < chunksRadius ; j++) {
          Chunk chunk = world.getChunk(i, 0, j).orElse(null);
          if (chunk != null) {
            System.out.println("Loading chunk: " + chunk);
            chunk.loadChunk(false);
          }
        }
      }
    }
  }

  /** Set the world for the game when it loads */
  @Listener
  public void onWorldLoad(UnloadChunkEvent event) {
    event.getTargetChunk().loadChunk(false);
  }

  // The code that makes the guns shoot, must register an gun registry
  @Listener
  public void onRightClick(InteractItemEvent event, @First Player player) {
    ItemType item = event.getItemStack().getType();
    //event.getItemStack()
    //System.out.println(item);
    // Use iron axe right now
    CooldownTracker cooldownTracker = player.getCooldownTracker();
    boolean pistol = item.matches(ItemStack.of(ItemTypes.IRON_AXE));
    boolean sniper = item.matches(ItemStack.of(ItemTypes.IRON_PICKAXE));
    boolean shotgun = item.matches(ItemStack.of(ItemTypes.STONE_SWORD));
    if ((pistol || sniper || shotgun) && !cooldownTracker.hasCooldown(item)) {
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
            itemStack.setQuantity(28);
          }, 2500, TimeUnit.MILLISECONDS);
        });
      } else {
        player.getItemInHand(HandTypes.MAIN_HAND).ifPresent(itemStack -> itemStack.setQuantity(quantity - 1));
        cooldownTracker.setCooldown(item, 5);
      }
    }
  }
}
