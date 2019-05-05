package io.github.minecraftbattleroyale;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.github.minecraftbattleroyale.commands.CollapseCommand;
import io.github.minecraftbattleroyale.commands.StartCommand;
import io.github.minecraftbattleroyale.commands.WinnerCommand;
import io.github.minecraftbattleroyale.core.ArenaGame;
import io.github.minecraftbattleroyale.core.GameMode;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
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
import org.spongepowered.api.event.block.InteractBlockEvent;
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
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.gamerule.DefaultGameRules;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.*;
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
  public final Map<ItemType, Gun> guns = new HashMap<>();
  {
    guns.put(ItemTypes.IRON_AXE, new Gun(ItemTypes.IRON_AXE, "Pistol", 6, 1000, 5, 5));
    guns.put(ItemTypes.IRON_PICKAXE, new Gun(ItemTypes.IRON_PICKAXE, "Sniper", 18, 3000, 25, 10));
    guns.put(ItemTypes.STONE_SWORD, new Gun(ItemTypes.STONE_SWORD, "Shotgun", 4, 2000, 40, 8));
  }
  public final Set<Vector3i> lootStashes = new HashSet<>();
  public final List<ItemStack> lootTable = new ArrayList<>();
  {
    lootTable.add(guns.get(ItemTypes.IRON_PICKAXE).createItem());
    lootTable.add(guns.get(ItemTypes.IRON_AXE).createItem());
    lootTable.add(guns.get(ItemTypes.STONE_SWORD).createItem());
    lootTable.add(ItemStack.of(ItemTypes.ACACIA_BOAT, 1));
    // armor
    lootTable.add(ItemStack.of(ItemTypes.LEATHER_CHESTPLATE, 1));
    lootTable.add(ItemStack.of(ItemTypes.DIAMOND_CHESTPLATE, 1));
    lootTable.add(ItemStack.of(ItemTypes.LEATHER_HELMET, 1));
    lootTable.add(ItemStack.of(ItemTypes.GOLDEN_HELMET, 1));
    lootTable.add(ItemStack.of(ItemTypes.DIAMOND_HELMET, 1));
    lootTable.add(ItemStack.of(ItemTypes.LEATHER_LEGGINGS, 1));
    lootTable.add(ItemStack.of(ItemTypes.GOLDEN_LEGGINGS, 1));
    lootTable.add(ItemStack.of(ItemTypes.IRON_LEGGINGS, 1));
    lootTable.add(ItemStack.of(ItemTypes.LEATHER_BOOTS, 1));
    lootTable.add(ItemStack.of(ItemTypes.GOLDEN_BOOTS, 1));
    // food
    lootTable.add(ItemStack.of(ItemTypes.GOLDEN_CARROT, 4));
    lootTable.add(ItemStack.of(ItemTypes.GOLDEN_APPLE, 4));
    // ammo
    ItemStack itemStack = ItemStack.of(ItemTypes.FEATHER, 16);
    itemStack.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, "Ammo"));
    lootTable.add(itemStack);
    lootTable.add(itemStack);
    lootTable.add(itemStack);
    lootTable.add(itemStack);
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
      properties.setGameRule(DefaultGameRules.KEEP_INVENTORY, "true");
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
    ItemStack ammo = ItemStack.of(ItemTypes.FEATHER);
    ammo.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, "Ammo"));
    CooldownTracker cooldownTracker = player.getCooldownTracker();
    if ((guns.containsKey(item)) && !cooldownTracker.hasCooldown(item)) {
      Gun gun = guns.get(item);
      int ammoCount = player.getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(ammo)).totalItems();
      int quantity = event.getItemStack().getQuantity();
      if (quantity >= 1 || event.getItemStack().getQuantity() >= 1) {
        Vector3d position = player.getPosition().add(0, 1.5, 0);
        double yaw = Math.toRadians(player.getHeadRotation().getY() + 90);
        double pitch = Math.toRadians(player.getHeadRotation().getX() + 90);
        Vector3d velocity = new Vector3d(Math.cos(yaw), Math.cos(pitch), Math.sin(yaw)).mul(gun.damage);
        position = position.add(velocity.normalize().mul(1.5));
        Entity arrow = player.getWorld().createEntity(EntityTypes.TIPPED_ARROW, position);
        player.getWorld().spawnEntity(arrow);
        arrow.setCreator(player.getUniqueId());
        arrow.setVelocity(velocity);
        for (int i = 0 ; i < 10 ; i++) {
          position = position.add(velocity.normalize());
          player.getWorld().spawnParticles(ParticleEffect.builder().type(ParticleTypes.SNOWBALL).build(), position);
        }
      }
      // if at one, reload gun
      if (quantity == 1 && ammoCount > 0) {
        cooldownTracker.setCooldown(item, (int) (gun.reloadTime / 1000) * 20);
        player.getItemInHand(HandTypes.MAIN_HAND).ifPresent(itemStack -> {
          itemStack.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, gun.name));
          int maxStackSize = itemStack.get(Keys.ITEM_DURABILITY).get();
          itemStack.offer(Keys.ITEM_DURABILITY, 1);
          long startTime = System.currentTimeMillis();
          SpongeExecutorService.SpongeFuture future = scheduler.scheduleAtFixedRate(() -> {
            itemStack.offer(Keys.ITEM_DURABILITY, (int) (maxStackSize * ((double) System.currentTimeMillis() - startTime) / gun.reloadTime));
          }, 0, 5, TimeUnit.MILLISECONDS);
          scheduler.schedule(() -> {
            future.cancel(true);
            itemStack.offer(Keys.ITEM_DURABILITY, maxStackSize);
            itemStack.setQuantity(ammoCount < gun.ammo ? ammoCount : gun.ammo);
            player.getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(ammo)).poll(gun.ammo);
          }, gun.reloadTime, TimeUnit.MILLISECONDS);
        });
      } else if (quantity == 1) {
          player.sendMessage(Text.of(TextColors.DARK_GREEN, "NO AMMO"));
      } else {
        player.getItemInHand(HandTypes.MAIN_HAND).ifPresent(itemStack -> itemStack.setQuantity(quantity - 1));
        cooldownTracker.setCooldown(item, gun.fireRate);
      }
    }
  }

  // todo wrong event
//  /** Reload the gun... */
//  @Listener(order = Order.FIRST)
//  public void onInteract(ClickInventoryEvent.Drop event, @First Player player) {
//    ItemType item = event.getCursorTransaction().getOriginal().getType();
//    //event.getItemStack()
//    System.out.println(item);
//    // Use iron axe right now
//    CooldownTracker cooldownTracker = player.getCooldownTracker();
//    if ((guns.containsKey(item)) && !cooldownTracker.hasCooldown(item)) {
//      Gun gun = guns.get(item);
//      int ammoCount = 100;
//      int quantity = event.getCursorTransaction().getOriginal().getQuantity();
//      // if at one, reload gun
//      if (ammoCount >= gun.ammo) {
//        cooldownTracker.setCooldown(item, (int) (gun.reloadTime / 1000) * 20);
//        player.getItemInHand(HandTypes.MAIN_HAND).ifPresent(itemStack -> {
//          itemStack.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, gun.name));
//          int maxStackSize = itemStack.get(Keys.ITEM_DURABILITY).get();
//          itemStack.offer(Keys.ITEM_DURABILITY, 1);
//          long startTime = System.currentTimeMillis();
//          SpongeExecutorService.SpongeFuture future = scheduler.scheduleAtFixedRate(() -> {
//            itemStack.offer(Keys.ITEM_DURABILITY, (int) (maxStackSize * ((double) System.currentTimeMillis() - startTime) / gun.reloadTime));
//          }, 0, 5, TimeUnit.MILLISECONDS);
//          scheduler.schedule(() -> {
//            future.cancel(true);
//            itemStack.offer(Keys.ITEM_DURABILITY, maxStackSize);
//            itemStack.setQuantity(gun.ammo);
//            //player.getInventory().
//          }, gun.reloadTime, TimeUnit.MILLISECONDS);
//        });
//      } else if (quantity == 1) {
//        player.sendMessage(Text.of(TextColors.DARK_GREEN, "NO AMMO"));
//      }
//    }
//  }

  @Listener
  public void onChests(InteractBlockEvent.Secondary event) {
    BlockSnapshot block = event.getTargetBlock();
    Vector3i location = block.getPosition();
    System.out.println("right click block");
    if (!lootStashes.contains(location)) {
      if (block.getLocation().get().getTileEntity().isPresent()) {
        TileEntity titleEntity = block.getLocation().get().getTileEntity().get();
        System.out.println(titleEntity);
        if (titleEntity instanceof TileEntityCarrier) {
          TileEntityCarrier tileEntityCarrier = (TileEntityCarrier) titleEntity;
          // todo randomize the loot amount and items
          Random rand = new Random();
          int ammount = rand.nextInt(8) + 4;
          Iterator<Inventory> slots = tileEntityCarrier.getInventory().slots().iterator();
          for (int i = 0 ; i < ammount ; i++) {
            ItemStack itemStack = lootTable.get(rand.nextInt(lootTable.size()));
            slots.next().set(itemStack);
          }
          lootStashes.add(location);
        }
      }
    }
  }
}
