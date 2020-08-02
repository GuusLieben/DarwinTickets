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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.moddedminecraft.mmctickets.data.ticketStatus.CLAIMED;
import static net.moddedminecraft.mmctickets.data.ticketStatus.OPEN;

public class reopen implements CommandExecutor {
	private final Main plugin;

	public reopen ( Main plugin ) {
		this.plugin = plugin;
	}

	@Override
	public CommandResult execute ( CommandSource src, CommandContext args ) throws CommandException {
		final int ticketID = args.<Integer>getOne("ticketID").get();

		final List<TicketData> tickets =
				new ArrayList<TicketData>(plugin.getDataStore().getTicketData());

		if (tickets.isEmpty()) {
			throw new CommandException(Messages.getErrorGen("Tickets list is empty."));
		} else {
			for (TicketData ticket : tickets) {
				if (ticket.getTicketID() == ticketID) {
					if (ticket.getStatus() == CLAIMED || ticket.getStatus() == OPEN) {
						throw new CommandException(Messages.getErrorTicketNotClosed(ticketID));
					}
					if (ticket.getStatus() == CLAIMED) {
						throw new CommandException(
								Messages.getErrorTicketClaim(
										ticket.getTicketID(),
										CommonUtil.getPlayerNameFromData(plugin, ticket.getStaffUUID())));
					}
					ticket.setStatus(OPEN);
					if (src instanceof Player) {
						ticket.setStaffUUID(((Player) src).getUniqueId().toString());
					} else {
						ticket.setStaffUUID(null);
					}
					ticket.setComment("");
					ticket.setNotified(0);

					try {
						plugin.getDataStore().updateTicketData(ticket);
					} catch (Exception e) {
						src.sendMessage(Messages.getErrorGen("Unable to reopen ticket"));
						e.printStackTrace();
					}

					CommonUtil.notifyOnlineStaff(
							Messages.getTicketReopen(src.getName(), ticket.getTicketID()));

					Optional<Player> ticketPlayerOP = Sponge.getServer().getPlayer(ticket.getPlayerUUID());
					if (ticketPlayerOP.isPresent()) {
						Player ticketPlayer = ticketPlayerOP.get();
						ticketPlayer.sendMessage(
								Messages.getTicketReopenUser(src.getName(), ticket.getTicketID()));
					}

					Location location = new Location(ticket.getWorld(), ticket.getX(), ticket.getY(), ticket.getZ());
					Plot plot = Plot.getPlot(location);
					DiscordUtil.editMessage(ticket.getDiscordMessage(), ticket.getStatus().getAssociatedColor(), CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()), src, ticket, DiscordTicketStatus.NEW, plot);

					return CommandResult.success();
				}
			}
			throw new CommandException(Messages.getTicketNotExist(ticketID));
		}
	}
}
