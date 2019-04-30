package io.github.minecraftbattleroyale.core;

import com.flowpowered.math.vector.Vector3d;
import io.github.minecraftbattleroyale.Airship;
import io.github.minecraftbattleroyale.MinecraftBattleRoyale;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldBorder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ArenaGame {
  private static final Vector3d LOBBY_SPAWN = new Vector3d(181, 60, 448);
  private static final Vector3d COLLAPSE_CENTER = new Vector3d(181, 0, 448);
  private static final int COLLAPSE_DIAMETER = 123;
  private static final Vector3d AIRSHIP_START = new Vector3d(181, 60, 448);
  private static final Vector3d AIRSHIP_STOP = new Vector3d(181, 60, 448);
  private static final int COLLAPSE_START = 60;
  private GameMode gameMode = GameMode.LOBBY;
  private World world;
  private Set<UserPlayer> players = new HashSet<>();
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
    Sponge.getEventManager().registerListeners(MinecraftBattleRoyale.get(), userPlayer);
    players.add(userPlayer);
    player.setLocationSafely(new Location<>(world, LOBBY_SPAWN));
    userPlayer.joinLobby();
  }

  /** Start the game, starts with the air ships*/
  public void startGame() {
//    if (gameMode != GameMode.LOBBY) {
//      System.out.println("cant start the game now");
//      return;
//    }
    gameMode = GameMode.RUNNING;
    players.forEach(userPlayer -> {
      Airship airship = new Airship(AIRSHIP_STOP);
      airship.spawnShip(new Location<>(userPlayer.getPlayer().getWorld(), AIRSHIP_START));
      airship.ride(userPlayer);
      userPlayer.startGame();
      userPlayer.getPlayer().sendMessage(Text.of(TextColors.GREEN, "Circle is collapsing in " + COLLAPSE_START + "..."));
    });
    scheduler.schedule(this::startCollapse, COLLAPSE_START, TimeUnit.SECONDS);
  }

  /** The start of the collapse */
  public void startCollapse() {
    players.stream().map(UserPlayer::getPlayer).forEach(player -> {
      player.sendMessage(Text.of(TextColors.GREEN, "Circle is collapsing..."));
      WorldBorder border = WorldBorder.builder()
        .center(COLLAPSE_CENTER.getX(), COLLAPSE_CENTER.getZ())
        .diameter(COLLAPSE_DIAMETER)
        .warningDistance(COLLAPSE_DIAMETER)
        .build();
      player.setWorldBorder(border, Cause.of(EventContext.builder().build(), MinecraftBattleRoyale.get()));
    });
  }

  @Listener
  public void onJoin(ClientConnectionEvent.Join event) {
    joinGame(event.getTargetEntity());
  }
}

