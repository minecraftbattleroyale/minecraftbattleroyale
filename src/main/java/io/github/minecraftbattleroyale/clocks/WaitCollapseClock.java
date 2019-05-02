package io.github.minecraftbattleroyale.clocks;

import io.github.minecraftbattleroyale.MinecraftBattleRoyale;
import io.github.minecraftbattleroyale.core.ArenaGame;
import io.github.minecraftbattleroyale.core.UserPlayer;
import net.year4000.utilities.TimeUtil;
import org.spongepowered.api.boss.BossBarColors;
import org.spongepowered.api.boss.BossBarOverlays;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.world.WorldBorder;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class WaitCollapseClock extends Clocker {
    private MinecraftBattleRoyale plugin;
    private ArenaGame game;
    private ServerBossBar bossBar;
    private CollapseClock collapse;

    public WaitCollapseClock(MinecraftBattleRoyale plugin, CollapseClock collapse) {
        super(30, TimeUnit.SECONDS);
        this.plugin = plugin;
        this.game = plugin.getCurrentGame();
        this.collapse = collapse;
    }

    @Override
    public void runFirst(long position) {
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
        bossBar.setName(Text.of(TextColors.RED, "Next Round - ", TextColors.DARK_PURPLE, new TimeUtil(getTime() - position, TimeUnit.MILLISECONDS).prettyOutput()));
        bossBar.setPercent(position / getTime());
    }

    @Override
    public void runLast(long position) {
        bossBar.setVisible(false);
        this.collapse.run(plugin.syncScheduler());
    }
}
