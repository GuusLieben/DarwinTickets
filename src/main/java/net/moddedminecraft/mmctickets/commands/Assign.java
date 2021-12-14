package net.moddedminecraft.mmctickets.commands;

import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.config.Messages;
import net.moddedminecraft.mmctickets.config.Permissions;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.util.CommonUtil;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.moddedminecraft.mmctickets.data.TicketStatus.APPROVED;
import static net.moddedminecraft.mmctickets.data.TicketStatus.CLAIMED;
import static net.moddedminecraft.mmctickets.data.TicketStatus.CLOSED;
import static net.moddedminecraft.mmctickets.data.TicketStatus.REJECTED;

public class Assign implements CommandExecutor {
    private final Main plugin;

    public Assign(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull CommandResult execute(@NotNull CommandSource src, CommandContext args) throws CommandException {
        final int ticketID = args.<Integer>getOne("ticketID").get();
        final User user = args.<Player>getOne("player").get();

        final List<TicketData> tickets = new ArrayList<TicketData>(plugin.getDataStore().getTicketData());

        if (tickets.isEmpty()) {
            throw new CommandException(Messages.getErrorGen("Tickets list is empty."));
        }
        else {
            for (TicketData ticket : tickets) {
                if (ticket.getTicketID() == ticketID) {
                    if (ticket.getStatus() == CLOSED || ticket.getStatus() == REJECTED || ticket.getStatus() == APPROVED) {
                        src.sendMessage(Messages.getErrorTicketAlreadyClosed());
                    }
                    if (ticket.getStatus() == CLAIMED && !src.hasPermission(Permissions.CLAIMED_TICKET_BYPASS)) {
                        throw new CommandException(Messages.getErrorTicketClaim(ticket.getTicketID(), CommonUtil.getPlayerNameFromData(plugin, ticket.getStaffUUID())));
                    }
                    ticket.setStatus(CLAIMED);
                    ticket.setStaffUUID(user.getUniqueId().toString());

                    try {
                        plugin.getDataStore().updateTicketData(ticket);
                    }
                    catch (Exception e) {
                        src.sendMessage(Messages.getErrorGen("Unable to assign " + user.getName() + " to ticket"));
                        e.printStackTrace();
                    }

                    CommonUtil.notifyOnlineStaff(Messages.getTicketAssign(CommonUtil.getPlayerNameFromData(plugin, ticket.getStaffUUID()), ticket.getTicketID()));

                    Optional<Player> ticketPlayerOP = Sponge.getServer().getPlayer(ticket.getPlayerUUID());
                    if (ticketPlayerOP.isPresent()) {
                        Player ticketPlayer = ticketPlayerOP.get();
                        ticketPlayer.sendMessage(Messages.getTicketAssignUser(ticket.getTicketID(), CommonUtil.getPlayerNameFromData(plugin, ticket.getStaffUUID())));
                    }
                    return CommandResult.success();
                }
            }
            throw new CommandException(Messages.getTicketNotExist(ticketID));
        }
    }
}
