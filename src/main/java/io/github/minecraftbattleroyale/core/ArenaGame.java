package io.github.minecraftbattleroyale.core;

import com.flowpowered.math.vector.Vector3d;
import io.github.minecraftbattleroyale.Airship;
import io.github.minecraftbattleroyale.MinecraftBattleRoyale;
import io.github.minecraftbattleroyale.clocks.CollapseClock;
import io.github.minecraftbattleroyale.clocks.StartGameClock;
import net.year4000.utilities.TimeUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
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
    event.setToTransform(new Transform<>(world, COLLAPSE_CENTER.add(0, 100, 0)));
  }

  /** Disable item drops */
  @Listener
  public void onDropItems(DropItemEvent.Pre event) {
    event.getDroppedItems().clear();
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
  public void onJoin(ClientConnectionEvent.Disconnect event) {
    leaveGame(event.getTargetEntity());
  }
}

