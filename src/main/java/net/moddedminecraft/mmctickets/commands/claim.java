package net.moddedminecraft.mmctickets.commands;

import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.config.Messages;
import net.moddedminecraft.mmctickets.config.Permissions;
import net.moddedminecraft.mmctickets.data.TicketData;
import static net.moddedminecraft.mmctickets.data.ticketStatus.Claimed;
import static net.moddedminecraft.mmctickets.data.ticketStatus.Closed;
import static net.moddedminecraft.mmctickets.data.ticketStatus.Held;
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
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class claim implements CommandExecutor {

	private final Main plugin;

	public claim ( Main plugin ) {
		this.plugin = plugin;
	}

	@Override
	public CommandResult execute ( CommandSource src, CommandContext args ) throws CommandException {
		final int ticketID = args.<Integer>getOne("ticketID").get();
		final List<TicketData> tickets =
				new ArrayList<TicketData>(plugin.getDataStore().getTicketData());

		UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
		if (src instanceof Player) {
			Player player = (Player) src;
			uuid = player.getUniqueId();
		}

		if (tickets.isEmpty()) {
			throw new CommandException(Messages.getErrorGen("Tickets list is empty."));
		} else {
			for (TicketData ticket : tickets) {
				if (ticket.getTicketID() == ticketID) {
					if (!ticket.getStaffUUID().equals(uuid)
							&& ticket.getStatus() == Claimed
							&& !src.hasPermission(Permissions.CLAIMED_TICKET_BYPASS)) {
						throw new CommandException(
								Messages.getErrorTicketClaim(
										ticket.getTicketID(),
										CommonUtil.getPlayerNameFromData(plugin, ticket.getStaffUUID())));
					}
					if (ticket.getStaffUUID().equals(uuid) && ticket.getStatus() == Claimed) {
						throw new CommandException(Messages.getErrorTicketClaim(ticket.getTicketID(), "you"));
					}
					if (ticket.getStatus() == Closed || ticket.getStatus() == Held) {
						throw new CommandException(Messages.getTicketNotOpen(ticketID));
					}

					ticket.setStaffUUID(uuid.toString());
					ticket.setStatus(Claimed);

					try {
						plugin.getDataStore().updateTicketData(ticket);
					} catch (Exception e) {
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
					DiscordUtil.editMessage(ticket.getDiscordMessage(), Color.GREEN, CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()), src, ticket, DiscordTicketStatus.CLAIMED, plot);

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
}
