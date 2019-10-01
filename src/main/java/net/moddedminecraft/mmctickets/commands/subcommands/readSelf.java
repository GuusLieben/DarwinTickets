package net.moddedminecraft.mmctickets.commands.subcommands;

import com.google.common.collect.Lists;
import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.config.Messages;
import net.moddedminecraft.mmctickets.config.Permissions;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.util.CommonUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;

import java.util.ArrayList;
import java.util.List;

import static net.moddedminecraft.mmctickets.data.ticketStatus.*;

public class readSelf implements CommandExecutor {

    private final Main plugin;

    public readSelf(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        final List<TicketData> tickets = new ArrayList<TicketData>(plugin.getDataStore().getTicketData());

        if (!src.hasPermission(Permissions.COMMAND_TICKET_READ_SELF)) {
            throw new CommandException(Messages.getErrorPermission(Permissions.COMMAND_TICKET_READ_SELF));
        }
        if (!(src instanceof Player)) {
            throw new CommandException(Messages.getErrorGen("Console users cannot use this command."));
        }
        Player player = (Player) src;

        if (tickets.isEmpty()) {
            throw new CommandException(Messages.getErrorGen("Tickets list is empty."));
        } else {
            PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            List<Text> contents = new ArrayList<>();
            for (TicketData ticket : tickets) {
                if (ticket.getPlayerUUID().equals(player.getUniqueId())) {
                    String online = CommonUtil.isUserOnline(ticket.getPlayerUUID());
                    Text.Builder send = Text.builder();
                    String status = "";
                    if (ticket.getStatus() == Open) status = "&bOpen &b- ";
                    if (ticket.getStatus() == Held) status = "&3Held &b- ";
                    if (ticket.getStatus() == Closed) status = "&bClosed &b- ";
                    send.append(plugin.fromLegacy(status + "&3#" + ticket.getTicketID() + " " + CommonUtil.getTimeAgo(ticket.getTimestamp()) + " by " + online + CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()) + " &3on " + CommonUtil.checkTicketServer(ticket.getServer()) + " &3- &7" + CommonUtil.shortenMessage(ticket.getMessage())));
                    send.onClick(TextActions.runCommand("/ticket read " + ticket.getTicketID()));
                    send.onHover(TextActions.showText(plugin.fromLegacy("Click here to get more details for ticket #" + ticket.getTicketID())));
                    contents.add(send.build());

                }
            }

            if (contents.isEmpty()) {
                contents.add(Messages.getTicketReadNoneSelf());
            }
            paginationService.builder()
                    .title(plugin.fromLegacy("&3Your Tickets"))
                    .contents(Lists.reverse(contents))
                    .padding(Text.of("-"))
                    .sendTo(src);
        }
        return CommandResult.success();
    }
}
