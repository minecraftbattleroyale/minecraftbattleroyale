package io.github.minecraftbattleroyale.commands;

import io.github.minecraftbattleroyale.MinecraftBattleRoyale;
import net.year4000.utilities.sponge.Messages;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import static net.year4000.utilities.sponge.Messages.CMD_ERROR_PLAYER;
import static org.spongepowered.api.text.format.TextColors.RED;

public class CollapseCommand implements CommandExecutor {
    private static final String[] ALIAS = new String[] {"collapse"};
    private static final CommandSpec COMMAND_SPEC = CommandSpec.builder()
            .description(Text.of("Start the collapse"))
            .executor(new CollapseCommand())
            .build();

    /** Register this command with the manager */
    public static void register(Object object) {
        Sponge.getCommandManager().register(object, COMMAND_SPEC, ALIAS);
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            throw new CommandException(Text.of(Messages.ERROR, RED, CMD_ERROR_PLAYER.get(src)));
        }

        final MinecraftBattleRoyale plugin = MinecraftBattleRoyale.get();
        final Player player = (Player) src;
        player.sendMessage(Text.of(RED, "Starting the collapse"));
        plugin.getCurrentGame().startCollapse();

        return CommandResult.builder().build();
    }
}
