package net.moddedminecraft.mmctickets.commands;

import com.magitechserver.magibridge.MagiBridge;
import java.awt.Color;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.dv8tion.jda.core.EmbedBuilder;
import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.config.Messages;
import net.moddedminecraft.mmctickets.data.TicketData;
import static net.moddedminecraft.mmctickets.data.ticketStatus.Claimed;
import static net.moddedminecraft.mmctickets.data.ticketStatus.Closed;
import static net.moddedminecraft.mmctickets.data.ticketStatus.Held;
import net.moddedminecraft.mmctickets.util.CommonUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

public class hold implements CommandExecutor {
	private final Main plugin;

	public hold ( Main plugin ) {
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
					if (ticket.getStatus() == Closed) {
						src.sendMessage(Messages.getErrorTicketAlreadyClosed());
					}
					if (ticket.getStatus() == Held) {
						src.sendMessage(Messages.getErrorTicketlreadyHold());
					}
					if (ticket.getStatus() == Claimed && !ticket.getStaffUUID().equals(uuid)) {
						src.sendMessage(
								Messages.getErrorTicketClaim(
										ticket.getTicketID(),
										CommonUtil.getPlayerNameFromData(plugin, ticket.getStaffUUID())));
					}
					ticket.setStatus(Held);
					ticket.setStaffUUID(UUID.fromString("00000000-0000-0000-0000-000000000000").toString());

					try {
						plugin.getDataStore().updateTicketData(ticket);
					} catch (Exception e) {
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

					EmbedBuilder embedBuilder = new EmbedBuilder();
					embedBuilder.setColor(Color.PINK);
					embedBuilder.setTitle("Submission on hold");
					embedBuilder.addField(
							"Submitted by : " + CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()),
							MessageFormat.format(
									"ID : #{0}\nPlot : {1}\nClosed by : {2}\nScore : {3}\n",
									ticketID,
									ticket.getMessage(),
									src.getName(),
									ticket.getComment().length() == 0 ? "None" : ticket.getComment()),
							false);
					embedBuilder.setThumbnail(
							"https://icon-library.net/images/stop-sign-icon-png/stop-sign-icon-png-8.jpg");

					MagiBridge.jda
							.getTextChannelById("525424284731047946")
							.getMessageById(ticket.getDiscordMessage())
							.queue(msg -> msg.editMessage(embedBuilder.build()).queue());
					return CommandResult.success();
				}
			}
			throw new CommandException(Messages.getTicketNotExist(ticketID));
		}
	}
}
