package io.github.minecraftbattleroyale.core;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;

import java.util.HashSet;
import java.util.Set;

public class ArenaGame {
  private static final Vector3d LOBBY_SPAWN = new Vector3d(181, 60, 448);
  private World world;
  private Set<UserPlayer> players = new HashSet<>();

  /** Set the world for this player should be done asap */
  public void setWorld(World world) {
    this.world = world;
  }

  /** Have the player join the game, have them then join the lobby */
  public void joinGame(Player player) {
    UserPlayer userPlayer = new UserPlayer(this, player);
    players.add(userPlayer);
    player.setLocation(LOBBY_SPAWN, world.getUniqueId());
    userPlayer.joinLobby();
  }

  /** Start the game, starts with the air ships*/
  public void startGame() {
    players.forEach(UserPlayer::startGame);
  }
}

