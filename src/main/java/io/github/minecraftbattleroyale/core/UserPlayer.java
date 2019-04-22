package io.github.minecraftbattleroyale.core;

import org.spongepowered.api.entity.living.player.Player;

public class UserPlayer {
  private Player player;
  private UserPlayerMode mode = UserPlayerMode.LOBBY;
  private int kills;
}
