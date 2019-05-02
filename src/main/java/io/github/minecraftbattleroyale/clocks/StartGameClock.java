package io.github.minecraftbattleroyale.clocks;

import io.github.minecraftbattleroyale.MinecraftBattleRoyale;
import io.github.minecraftbattleroyale.core.ArenaGame;
import io.github.minecraftbattleroyale.core.UserPlayer;
import net.year4000.utilities.TimeUtil;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.title.Title;

import java.util.concurrent.TimeUnit;

public class StartGameClock extends Clocker {
    private MinecraftBattleRoyale plugin;
    private ArenaGame game;

    public StartGameClock(MinecraftBattleRoyale plugin) {
        super(10, TimeUnit.SECONDS);
        this.plugin = plugin;
        this.game = plugin.getCurrentGame();
    }

    @Override
    public void runFirst(long position) {
        // todo
    }

    @Override
    public void runTock(long position) {
        game.getPlayers().stream().map(UserPlayer::getPlayer).forEach(player -> {
            if (position % 4 == 0) {
                player.playSound(SoundTypes.BLOCK_NOTE_PLING, player.getPosition(), 0.25);
            }
            Title title = Title.builder()
                    .title(Text.of(TextColors.AQUA, "Starting..."))
                    //.subtitle(MathUtil.countTitle(position + "", MathUtil.percent((int) getTime(), (int) position)))
                    .subtitle(Text.of(TextColors.DARK_AQUA, new TimeUtil(getTime() - position, TimeUnit.MILLISECONDS).prettyOutput()))
                    .fadeIn(0)
                    .fadeOut(0)
                    .build();
            player.sendTitle(title);
        });
    }

    @Override
    public void runLast(long position) {
        game.getPlayers().stream().map(UserPlayer::getPlayer).forEach(Viewer::clearTitle);
        game.startGame();
    }


}
