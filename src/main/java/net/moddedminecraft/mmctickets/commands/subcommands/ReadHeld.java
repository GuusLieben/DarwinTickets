package net.moddedminecraft.mmctickets.commands.subcommands;

import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.config.Messages;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.util.CommonUtil;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;

import java.util.ArrayList;
import java.util.List;

import static net.moddedminecraft.mmctickets.data.TicketStatus.HELD;

public class ReadHeld implements CommandExecutor {

    private final Main plugin;

    public ReadHeld(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) throws CommandException {
        final List<TicketData> tickets = new ArrayList<TicketData>(plugin.getDataStore().getTicketData());

        if (tickets.isEmpty()) {
            throw new CommandException(Messages.getErrorGen("Tickets list is empty."));
        }
        else {
            PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            List<Text> contents = new ArrayList<>();
            for (TicketData ticket : tickets) {
                if (ticket.getStatus() == HELD) {
                    String online = CommonUtil.isUserOnline(ticket.getPlayerUUID());
                    Text.Builder send = Text.builder();
                    send.append(plugin.fromLegacy("&3#" + ticket.getTicketID() + " " + CommonUtil.getTimeAgo(ticket.getTimestamp()) + " by " + online + CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()) + " &3on " + CommonUtil.checkTicketServer(ticket.getServer()) + " &3- &7" + CommonUtil.shortenMessage(ticket.getMessage())));
                    send.onClick(TextActions.runCommand("/ticket read " + ticket.getTicketID()));
                    send.onHover(TextActions.showText(plugin.fromLegacy("Click here to get more details for ticket #" + ticket.getTicketID())));
                    contents.add(send.build());

                }
            }

            if (contents.isEmpty()) {
                contents.add(Messages.getTicketReadNoneHeld());
            }
            paginationService.builder()
                    .title(plugin.fromLegacy("&3Held Tickets"))
                    .contents(contents)
                    .padding(Text.of("-"))
                    .sendTo(src);
        }
        return CommandResult.success();
    }
}
