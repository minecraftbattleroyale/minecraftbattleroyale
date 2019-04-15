package io.github.minecraftbattleroyale;

import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "mcbr", name = "Minecraft Battle Royale")
public class MinecraftBattleRoyale {

  @Listener
  public void onStart(GameStartedServerEvent event) {
    System.out.println(event);
  }
}
