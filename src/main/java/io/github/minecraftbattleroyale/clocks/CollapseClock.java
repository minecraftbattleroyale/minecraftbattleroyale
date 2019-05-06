package io.github.minecraftbattleroyale.clocks;

import io.github.minecraftbattleroyale.MinecraftBattleRoyale;
import io.github.minecraftbattleroyale.core.ArenaGame;
import io.github.minecraftbattleroyale.core.GameMode;
import io.github.minecraftbattleroyale.core.UserPlayer;
import io.github.minecraftbattleroyale.core.UserPlayerMode;
import net.year4000.utilities.TimeUtil;
import org.spongepowered.api.boss.BossBarColors;
import org.spongepowered.api.boss.BossBarOverlays;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.world.WorldBorder;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CollapseClock extends Clocker {
    private MinecraftBattleRoyale plugin;
    private ArenaGame game;
    private ServerBossBar bossBar;
    public int round;
    private static final int MAX_ROUNDS = 5;
    private static int[] diamaters = new int[] {800, 500 , 300, 100, 50, 0};
    private static int[] closing = new int[] {120, 60, 30, 15, 5};
    private long showCollapse;

    public CollapseClock(MinecraftBattleRoyale plugin) {
        this(plugin, 1);
    }

    public CollapseClock(MinecraftBattleRoyale plugin, int round) {
        super(closing[round - 1], TimeUnit.SECONDS);
        this.plugin = plugin;
        this.game = plugin.getCurrentGame();
        this.round = round;
    }

    @Override
    public void runFirst(long position) {
        if (game.getGameMode() == GameMode.ENDDED) {
            this.clock.task.cancel(false);
            return;
        }
        game.getPlayers().stream().map(UserPlayer::getPlayer).forEach(player -> {
            player.playSound(SoundTypes.ENTITY_ENDERDRAGON_GROWL, player.getPosition(), 0.5);
            WorldBorder worldBorder = game.getWorld().getWorldBorder();
            worldBorder.setCenter(ArenaGame.COLLAPSE_CENTER.getX(), ArenaGame.COLLAPSE_CENTER.getZ());
            worldBorder.setDamageAmount(round * 0.0125);
            worldBorder.setDamageThreshold(0);
            worldBorder.setWarningDistance(0);
            worldBorder.setWarningTime(0);
            worldBorder.setDiameter(diamaters[round - 1]);
            worldBorder.setDiameter(diamaters[round], timeUnit.toMillis(time));
            player.sendTitle(Title.builder()
                    .title(Text.of(TextColors.AQUA, "Round " + round))
                    .subtitle(Text.of(TextColors.DARK_AQUA, "Started    "))
                    .fadeIn(1)
                    .fadeOut(20 * 2)
                    .build());
        });
        bossBar = ServerBossBar.builder()
                .name(Text.of(TextColors.RED, "ZONE"))
                .percent(0)
                .color(BossBarColors.RED)
                .overlay(BossBarOverlays.PROGRESS)
                .build();
        bossBar.addPlayers(game.getPlayers().stream().map(UserPlayer::getPlayer).collect(Collectors.toList()));
        bossBar.setVisible(true);
        showCollapse = position + 2000;
    }

    @Override
    public void runTock(long position) {
        if (game.getGameMode() == GameMode.ENDDED) {
            bossBar.setVisible(false);
            this.clock.task.cancel(false);
            return;
        }
        bossBar.setName(Text.of(TextColors.RED, "Round ", TextColors.DARK_RED, round, TextColors.RED, " - ", TextColors.DARK_PURPLE, new TimeUtil(getTime() - position, TimeUnit.MILLISECONDS).prettyOutput()));
        bossBar.setPercent(((getTime() - position) / (float)1000.0) / (float)100.0);
        if (position > showCollapse) {
            game.getPlayers().stream().map(UserPlayer::getPlayer).forEach(player -> {
                player.clearTitle();
                player.sendTitle(Title.builder().subtitle(Text.EMPTY).title(Text.EMPTY).actionBar(Text.of(TextColors.RED, TextStyles.BOLD, "Square is collapsing...")).build());
            });
        }
        for (UserPlayer userPlayer1 : game.getPlayers()) {
            userPlayer1.getPlayer().offer(Keys.EXPERIENCE_LEVEL, game.alivePlayers());
        }
    }

    @Override
    public void runLast(long position) {
        if (round + 1 <= MAX_ROUNDS) {
            bossBar.setVisible(false);
            game.getPlayers().stream().map(UserPlayer::getPlayer).forEach(player -> {
                player.sendTitle(Title.builder().actionBar(Text.of(TextColors.GREEN, "NEXT COLLAPSE...")).build());
            });
            new WaitCollapseClock(plugin, new CollapseClock(plugin, round + 1)).run(plugin.syncScheduler());
        }
    }
}
