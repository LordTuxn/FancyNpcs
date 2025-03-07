package de.oliver.fancynpcs.commands.npc;

import de.oliver.fancylib.LanguageConfig;
import de.oliver.fancylib.MessageHelper;
import de.oliver.fancynpcs.FancyNpcs;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.events.NpcModifyEvent;
import de.oliver.fancynpcs.commands.Subcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

public class PlayerCommandCMD implements Subcommand {

    private final LanguageConfig lang = FancyNpcs.getInstance().getLanguageConfig();

    @Override
    public List<String> tabcompletion(@NotNull Player playedr, @Nullable Npc npc, @NotNull String[] args) {
        if (args.length == 3) {
            return Stream.of("none")
                    .filter(input -> input.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }
        
        return null;
    }

    @Override
    public boolean run(@NotNull CommandSender receiver, @Nullable Npc npc, @NotNull String[] args) {
        if (args.length < 3) {
            MessageHelper.error(receiver, lang.get("wrong-usage"));
            return false;
        }


        if (npc == null) {
            MessageHelper.error(receiver, lang.get("npc-not-found"));
            return false;
        }

        String cmd = "";
        for (int i = 2; i < args.length; i++) {
            cmd += args[i] + " ";
        }
        cmd = cmd.substring(0, cmd.length() - 1);

        if (cmd.equalsIgnoreCase("none")) {
            cmd = "";
        }

        for (String blockedCommand : FancyNpcs.getInstance().getFancyNpcConfig().getBlockedCommands()) {
            if (cmd.toLowerCase().startsWith(blockedCommand.toLowerCase())) {
                MessageHelper.error(receiver, lang.get("illegal-command"));
                return false;
            }
        }

        NpcModifyEvent npcModifyEvent = new NpcModifyEvent(npc, NpcModifyEvent.NpcModification.PLAYER_COMMAND, cmd, receiver);
        npcModifyEvent.callEvent();

        if (!npcModifyEvent.isCancelled()) {
            npc.getData().setPlayerCommand(cmd);
            MessageHelper.success(receiver, lang.get("npc-command-playerCommand-updated"));
        } else {
            MessageHelper.error(receiver, lang.get("npc-command-modification-cancelled"));
        }

        return true;
    }
}
