package net.moddedminecraft.mmctickets.commands;

import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;

import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.config.Messages;
import net.moddedminecraft.mmctickets.config.Permissions;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.util.CommonUtil;
import net.moddedminecraft.mmctickets.util.DiscordUtil;
import net.moddedminecraft.mmctickets.util.DiscordUtil.DiscordTicketStatus;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

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

public class Claim implements CommandExecutor {

    private final Main plugin;

    public Claim(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull CommandResult execute(@NotNull CommandSource src, CommandContext args) throws CommandException {
        final int ticketID = args.<Integer>getOne("ticketID").get();
        final List<TicketData> tickets =
                new ArrayList<TicketData>(plugin.getDataStore().getTicketData());

        if (src instanceof Player) {
            Player player = (Player) src;
            UUID uuid = player.getUniqueId();

            if (tickets.isEmpty()) {
                throw new CommandException(Messages.getErrorGen("Tickets list is empty."));
            }
            else {
                for (TicketData ticket : tickets) {
                    if (ticket.getTicketID() == ticketID) {
                        if (!ticket.getStaffUUID().equals(uuid)
                                && ticket.getStatus() == CLAIMED
                                && !src.hasPermission(Permissions.CLAIMED_TICKET_BYPASS)) {
                            throw new CommandException(
                                    Messages.getErrorTicketClaim(
                                            ticket.getTicketID(),
                                            CommonUtil.getPlayerNameFromData(plugin, ticket.getStaffUUID())));
                        }
                        if (ticket.getStaffUUID().equals(uuid) && ticket.getStatus() == CLAIMED) {
                            throw new CommandException(Messages.getErrorTicketClaim(ticket.getTicketID(), "you"));
                        }
                        if (ticket.getStatus() == CLOSED || ticket.getStatus() == REJECTED || ticket.getStatus() == APPROVED || ticket.getStatus() == HELD) {
                            throw new CommandException(Messages.getTicketNotOpen(ticketID));
                        }

                        ticket.setStaffUUID(uuid.toString());
                        ticket.setStatus(CLAIMED);

                        try {
                            plugin.getDataStore().updateTicketData(ticket);
                        }
                        catch (Exception e) {
                            src.sendMessage(Messages.getErrorGen("Unable to claim ticket"));
                            e.printStackTrace();
                        }

                        Optional<Player> ticketPlayerOP = Sponge.getServer().getPlayer(ticket.getPlayerUUID());
                        if (ticketPlayerOP.isPresent()) {
                            Player ticketPlayer = ticketPlayerOP.get();
                            ticketPlayer.sendMessage(
                                    Messages.getTicketClaimUser(src.getName(), ticket.getTicketID()));
                        }

                        CommonUtil.notifyOnlineStaff(
                                Messages.getTicketClaim(src.getName(), ticket.getTicketID()));

                        Location location = new Location(ticket.getWorld(), ticket.getX(), ticket.getY(), ticket.getZ());
                        Plot plot = Plot.getPlot(location);
                        DiscordUtil.editMessage(ticket.getDiscordMessage(), Color.GREEN, CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()), ticket, DiscordTicketStatus.CLAIMED, plot);

                        Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "lp user " + src.getName() + " permission set plots.admin.build.other true");
                        Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "lp user " + src.getName() + " permission set plots.admin.destroy.other true");
                        Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "lp user " + src.getName() + " permission set plots.admin.interact.other true");
                        src.sendMessage(Text.of(TextColors.GRAY, "[] ", TextColors.AQUA, "Activated reviewer plot bypass, note that you will have to relog for this bypass to take effect"));

                        return CommandResult.success();
                    }
                }
                throw new CommandException(Messages.getTicketNotExist(ticketID));
            }
        }
        throw new CommandException(Text.of("Only a player can claim a ticket"));
    }
}
