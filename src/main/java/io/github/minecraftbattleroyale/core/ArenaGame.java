package io.github.minecraftbattleroyale.core;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import io.github.minecraftbattleroyale.Airship;
import io.github.minecraftbattleroyale.MinecraftBattleRoyale;
import io.github.minecraftbattleroyale.clocks.CollapseClock;
import io.github.minecraftbattleroyale.clocks.StartGameClock;
import net.year4000.utilities.TimeUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.boss.BossBarColors;
import org.spongepowered.api.boss.BossBarOverlays;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldBorder;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ArenaGame {
  public static final Vector3d LOBBY_SPAWN = new Vector3d(181, 60, 448);
  public static final Vector3d COLLAPSE_CENTER = new Vector3d(75, 0, 40);
  public static final int COLLAPSE_DIAMETER = 700;
  public static final Vector3d AIRSHIP_START = new Vector3d(300, 150, 305);
  public static final Vector3d AIRSHIP_STOP = new Vector3d(-160, 150, -177);
  public static final int COLLAPSE_START = 60;
  private GameMode gameMode = GameMode.LOBBY;
  private World world;
  private List<UserPlayer> players = new ArrayList<>();
  private SpongeExecutorService scheduler;

  /** Set the world for this player should be done asap */
  public void setWorld(World world) {
    this.world = world;
  }

  public void setScheduler(SpongeExecutorService scheduler) {
    this.scheduler = scheduler;
  }

  /** Have the player join the game, have them then join the lobby */
  public void joinGame(Player player) {
    UserPlayer userPlayer = new UserPlayer(this, player);
    //Sponge.getEventManager().registerListeners(MinecraftBattleRoyale.get(), userPlayer);
    players.add(userPlayer);
    player.setLocationSafely(new Location<>(world, LOBBY_SPAWN));
    userPlayer.joinLobby();
  }

  /** Have the player join the game, have them then join the lobby */
  public void spectateGame(Player player) {
    UserPlayer userPlayer = new UserPlayer(this, player);
    //Sponge.getEventManager().registerListeners(MinecraftBattleRoyale.get(), userPlayer);
    player.setLocationSafely(new Location<>(world, COLLAPSE_CENTER.add(0, 100, 0)));
    player.offer(Keys.GAME_MODE, GameModes.SPECTATOR);
    ServerBossBar bossBar = ServerBossBar.builder()
            .name(Text.of(TextColors.GOLD, "Spectator Mode"))
            .percent(1)
            .color(BossBarColors.WHITE)
            .overlay(BossBarOverlays.PROGRESS)
            .build();
    bossBar.addPlayer(player);
    bossBar.setVisible(true);
  }

  /** Get the player from the array list O(n) bad */
  public UserPlayer getPlayer(Player player) {
    for (UserPlayer userPlayer : players) {
      if (userPlayer.getPlayer().getUniqueId().equals(player.getUniqueId())) {
        return userPlayer;
      }
    }
    return null;
  }

  public List<UserPlayer> getPlayers() {
    return this.players;
  }

  public GameMode getGameMode() {
    return this.gameMode;
  }

  public World getWorld() {
    return this.world;
  }

  /** Get the number of alive players */
  public int alivePlayers() {
    return (int) getPlayers().stream().filter(UserPlayer::isAlive).count();
  }

  /** Have the player join the game, have them then join the lobby */
  public void leaveGame(Player player) {
    UserPlayer userPlayer = getPlayer(player);
    if (userPlayer != null) {
      players.remove(userPlayer);
      //Sponge.getEventManager().unregisterListeners(userPlayer);
    }
  }

  public void startGameCountdown() {
    new StartGameClock(MinecraftBattleRoyale.get()).run(scheduler);
  }

  /** Declare the winner of the match */
  public void declareWinner(UserPlayer winnerPlayer) {
    System.out.println("declating winner: " + winnerPlayer);
    gameMode = GameMode.ENDDED;
    getPlayers().stream().map(UserPlayer::getPlayer).forEach(player -> {
      // convert players to spectators
      player.offer(Keys.GAME_MODE, GameModes.SPECTATOR);
      // display title
      player.playSound(SoundTypes.ENTITY_ENDERDRAGON_DEATH, player.getPosition(), 1);
      Title title = Title.builder()
              .title(Text.of(TextColors.AQUA, "Winner"))
              //.subtitle(MathUtil.countTitle(position + "", MathUtil.percent((int) getTime(), (int) position)))
              .subtitle(Text.of(TextColors.DARK_AQUA, winnerPlayer.getPlayer().getName()))
              .fadeIn(5)
              .fadeOut(20 * 20)
              .build();
      player.sendTitle(title);
    });
    scheduler.schedule(() -> Sponge.getServer().shutdown(Text.of(TextColors.GOLD, "Server Restarting")), 20, TimeUnit.SECONDS);
  }

  /** Start the game, starts with the air ships*/
  public void startGame() {
    if (gameMode != GameMode.LOBBY) {
      System.out.println("cant start the game now");
      return;
    }
    gameMode = GameMode.RUNNING;
    Random rand = new Random();
    players.forEach(userPlayer -> {
      Location<World> start = new Location<>(userPlayer.getPlayer().getWorld(), AIRSHIP_START.add(rand.nextInt(10), 0, rand.nextInt(10)));
      userPlayer.getPlayer().setLocation(start);
      Airship airship = new Airship(AIRSHIP_STOP);
      airship.spawnShip(start);
      airship.ride(userPlayer);
      Sponge.getEventManager().registerListeners(MinecraftBattleRoyale.get(), airship);
      userPlayer.startGame();
      userPlayer.getPlayer().sendMessage(Text.of(TextColors.GREEN, "Circle is collapsing in " + COLLAPSE_START + "..."));
    });
    scheduler.schedule(this::startCollapse, COLLAPSE_START, TimeUnit.SECONDS);
  }

  /** The start of the collapse */
  public void startCollapse() {
    new CollapseClock(MinecraftBattleRoyale.get()).run(scheduler);
  }

  @Listener
  public void onRespawn(RespawnPlayerEvent event) {
    event.setToTransform(new Transform<>(world, MinecraftBattleRoyale.get().getCurrentGame().getPlayer(event.getOriginalPlayer()).deathLocation));
  }

  @Listener
  public void onDeath(DestructEntityEvent.Death event, @First Player player) {
    event.setKeepInventory(true);
    MinecraftBattleRoyale mcbr = MinecraftBattleRoyale.get();
    ArenaGame game = mcbr.getCurrentGame();
    UserPlayer userPlayer = game.getPlayer(player);
    userPlayer.deathLocation = player.getPosition();
    // set the player stats after they have died
    System.out.println("Player has died: " + userPlayer);
    System.out.println(player.getInventory());
    Vector3i position = player.getPosition().toInt();
    player.getWorld().setBlock(position, BlockState.builder().blockType(BlockTypes.CHEST).build());
    mcbr.lootStashes.add(position);
    System.out.println("right click block");
    if (player.getLocation().getTileEntity().isPresent()) {
      TileEntity titleEntity = player.getLocation().getTileEntity().get();
      System.out.println(titleEntity);
      if (titleEntity instanceof TileEntityCarrier) {
        TileEntityCarrier tileEntityCarrier = (TileEntityCarrier) titleEntity;
        Iterator<Inventory> slots = tileEntityCarrier.getInventory().slots().iterator();

        for (Inventory inv : player.getInventory().slots()) {
          inv.poll().ifPresent(item -> slots.next().set(item));
        }
      }
    }
    // todo create chest of items
    userPlayer.death();
    int playingPlayers  = 0;
    for (UserPlayer userPlayer1 : game.getPlayers()) {
      player.offer(Keys.EXPERIENCE_LEVEL, game.alivePlayers());
      if (userPlayer1.getMode() == UserPlayerMode.IN_GAME || userPlayer1.getMode() == UserPlayerMode.START_GAME) {
        playingPlayers++;
      }
    }
    // when finding if the game should end use this right now.
    if (playingPlayers == 1) {
      for (UserPlayer userPlayer1 : game.getPlayers()) {
        if (userPlayer1.getMode() == UserPlayerMode.IN_GAME || userPlayer1.getMode() == UserPlayerMode.START_GAME) {
          game.declareWinner(userPlayer1);
          return;
        }
      }
    }
    event.setCancelled(true);
  }

  /** Disable item drops */
  @Listener
  public void onDropItems(DropItemEvent.Pre event) {
    if (gameMode != GameMode.RUNNING) {
      event.getDroppedItems().clear();
    } else {
      // remove dropped guns... may be BUGGY
      event.getDroppedItems().removeIf(itemStackSnapshot -> MinecraftBattleRoyale.get().guns.containsKey(itemStackSnapshot.getType()));
      System.out.println(event.getDroppedItems());
    }
  }

  @Listener
  public void onJoin(ClientConnectionEvent.Join event) {
    if (gameMode != GameMode.LOBBY) {
      spectateGame(event.getTargetEntity());
    } else {
      joinGame(event.getTargetEntity());
    }
  }

  @Listener
  public void onInteract(ClickInventoryEvent event) {
    event.setCancelled(gameMode == GameMode.LOBBY);
  }

  @Listener
  public void onInteract(ClickInventoryEvent.Secondary event) {
    event.setCancelled(MinecraftBattleRoyale.get().guns.containsKey(event.getCursorTransaction().getOriginal().getType()));
  }

  @Listener
  public void onInteract(ClickInventoryEvent.Drag event) {
    event.setCancelled(MinecraftBattleRoyale.get().guns.containsKey(event.getCursorTransaction().getOriginal().getType()));
  }

  @Listener
  public void onJoin(ClientConnectionEvent.Disconnect event) {
    leaveGame(event.getTargetEntity());
  }
}

