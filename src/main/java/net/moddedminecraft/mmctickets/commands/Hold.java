package net.moddedminecraft.mmctickets.commands;

import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;

import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.config.Messages;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.util.CommonUtil;
import net.moddedminecraft.mmctickets.util.DiscordUtil;
import net.moddedminecraft.mmctickets.util.DiscordUtil.DiscordTicketStatus;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.moddedminecraft.mmctickets.data.TicketStatus.APPROVED;
import static net.moddedminecraft.mmctickets.data.TicketStatus.CLAIMED;
import static net.moddedminecraft.mmctickets.data.TicketStatus.CLOSED;
import static net.moddedminecraft.mmctickets.data.TicketStatus.HELD;
import static net.moddedminecraft.mmctickets.data.TicketStatus.REJECTED;

public class Hold implements CommandExecutor {
    private final Main plugin;

    public Hold(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        final int ticketID = args.<Integer>getOne("ticketID").get();

        final List<TicketData> tickets =
                new ArrayList<TicketData>(plugin.getDataStore().getTicketData());

        UUID uuid = null;
        if (src instanceof Player) {
            Player player = (Player) src;
            uuid = player.getUniqueId();
        }

        if (tickets.isEmpty()) {
            throw new CommandException(Messages.getErrorGen("Tickets list is empty."));
        }
        else {
            for (TicketData ticket : tickets) {
                if (ticket.getTicketID() == ticketID) {
                    if (ticket.getStatus() == CLOSED || ticket.getStatus() == REJECTED || ticket.getStatus() == APPROVED) {
                        src.sendMessage(Messages.getErrorTicketAlreadyClosed());
                    }
                    if (ticket.getStatus() == HELD) {
                        src.sendMessage(Messages.getErrorTicketlreadyHold());
                    }
                    if (ticket.getStatus() == CLAIMED && !ticket.getStaffUUID().equals(uuid)) {
                        src.sendMessage(
                                Messages.getErrorTicketClaim(
                                        ticket.getTicketID(),
                                        CommonUtil.getPlayerNameFromData(plugin, ticket.getStaffUUID())));
                    }
                    ticket.setStatus(HELD);
                    if (src instanceof Player) {
                        ticket.setStaffUUID(((Player) src).getUniqueId().toString());
                    }
                    else {
                        ticket.setStaffUUID(null);
                    }

                    try {
                        plugin.getDataStore().updateTicketData(ticket);
                    }
                    catch (Exception e) {
                        src.sendMessage(Messages.getErrorGen("Unable to put ticket on hold"));
                        e.printStackTrace();
                    }

                    CommonUtil.notifyOnlineStaff(Messages.getTicketHold(ticket.getTicketID(), src.getName()));

                    Optional<Player> ticketPlayerOP = Sponge.getServer().getPlayer(ticket.getPlayerUUID());
                    if (ticketPlayerOP.isPresent()) {
                        Player ticketPlayer = ticketPlayerOP.get();
                        ticketPlayer.sendMessage(
                                Messages.getTicketHoldUser(ticket.getTicketID(), src.getName()));
                    }

                    Location location = new Location(ticket.getWorld(), ticket.getX(), ticket.getY(), ticket.getZ());
                    Plot plot = Plot.getPlot(location);
                    DiscordUtil.editMessage(ticket.getDiscordMessage(), Color.PINK, CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()), ticket, DiscordTicketStatus.HOLD, plot);

                    return CommandResult.success();
                }
            }
            throw new CommandException(Messages.getTicketNotExist(ticketID));
        }
    }
}
