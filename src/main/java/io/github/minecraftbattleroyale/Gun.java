package io.github.minecraftbattleroyale;

import org.spongepowered.api.scheduler.SpongeExecutorService;


public class Gun {
  private SpongeExecutorService scheduler;

  public Gun() {
    this.scheduler = MinecraftBattleRoyale.get().syncScheduler();
  }

}
