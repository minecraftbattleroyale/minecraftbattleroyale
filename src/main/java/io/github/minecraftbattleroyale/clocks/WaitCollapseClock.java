package io.github.minecraftbattleroyale.clocks;

import io.github.minecraftbattleroyale.MinecraftBattleRoyale;
import io.github.minecraftbattleroyale.core.ArenaGame;
import io.github.minecraftbattleroyale.core.GameMode;
import io.github.minecraftbattleroyale.core.UserPlayer;
import net.year4000.utilities.TimeUtil;
import org.spongepowered.api.boss.BossBarColors;
import org.spongepowered.api.boss.BossBarOverlays;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class WaitCollapseClock extends Clocker {
    private static int[] waiting = new int[] {60, 30, 15, 15, 5};
    private MinecraftBattleRoyale plugin;
    private ArenaGame game;
    private ServerBossBar bossBar;
    private CollapseClock collapse;

    public WaitCollapseClock(MinecraftBattleRoyale plugin, CollapseClock collapse) {
        super(waiting[collapse.round - 1], TimeUnit.SECONDS);
        this.plugin = plugin;
        this.game = plugin.getCurrentGame();
        this.collapse = collapse;
    }

    @Override
    public void runFirst(long position) {
        if (game.getGameMode() == GameMode.ENDDED) {
            this.clock.task.cancel(false);
            return;
        }
        bossBar = ServerBossBar.builder()
                .name(Text.of(TextColors.RED, "Next Round: "))
                .percent(0)
                .color(BossBarColors.RED)
                .overlay(BossBarOverlays.PROGRESS)
                .build();
        bossBar.addPlayers(game.getPlayers().stream().map(UserPlayer::getPlayer).collect(Collectors.toList()));
        bossBar.setVisible(true);
    }

    @Override
    public void runTock(long position) {
        if (game.getGameMode() == GameMode.ENDDED) {
            this.clock.task.cancel(false);
            bossBar.setVisible(false);
            return;
        }
        bossBar.setName(Text.of(TextColors.RED, "Next Round - ", TextColors.DARK_PURPLE, new TimeUtil(getTime() - position, TimeUnit.MILLISECONDS).prettyOutput()));
        bossBar.setPercent((((getTime() - position) / 1000) / (float) this.time));
        for (UserPlayer userPlayer1 : game.getPlayers()) {
            userPlayer1.getPlayer().offer(Keys.EXPERIENCE_LEVEL, game.alivePlayers());
        }
    }

    @Override
    public void runLast(long position) {
        bossBar.setVisible(false);
        this.collapse.run(plugin.syncScheduler());
    }
}
