package net.moddedminecraft.mmctickets.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.config.Messages;
import net.moddedminecraft.mmctickets.config.Permissions;
import net.moddedminecraft.mmctickets.data.TicketData;
import static net.moddedminecraft.mmctickets.data.ticketStatus.Claimed;
import net.moddedminecraft.mmctickets.util.CommonUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;

public class comment implements CommandExecutor {

	private final Main plugin;

	public comment ( Main plugin ) {
		this.plugin = plugin;
	}

	@Override
	public CommandResult execute ( CommandSource src, CommandContext args ) throws CommandException {
		final int ticketID = args.<Integer>getOne("ticketID").get();
		final String comment = args.<String>getOne("comment").get();

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
					if (!ticket.getComment().isEmpty()) {
						if (src.hasPermission(Permissions.COMMAND_TICKET_EDIT_COMMENT)) {
							Text.Builder action = Text.builder();
							action.append(
									Text.builder()
											.append(plugin.fromLegacy(Messages.getYesButton()))
											.onHover(
													TextActions.showText(plugin.fromLegacy(Messages.getYesButtonHover())))
											.onClick(
													TextActions.executeCallback(
															changeTicketComment(ticketID, comment, src.getName())))
											.build());
							src.sendMessage(Messages.getTicketCommentedit(ticketID));
							src.sendMessage(action.build());
							return CommandResult.success();
						} else {
							throw new CommandException(
									Messages.getErrorGen("There is already a comment on this ticket."));
						}
					}
					ticket.setComment(comment);

					try {
						plugin.getDataStore().updateTicketData(ticket);
					} catch (Exception e) {
						src.sendMessage(Messages.getErrorGen("Unable to comment on ticket"));
						e.printStackTrace();
					}

					Optional<Player> ticketPlayerOP = Sponge.getServer().getPlayer(ticket.getPlayerUUID());
					if (ticketPlayerOP.isPresent()) {
						Player ticketPlayer = ticketPlayerOP.get();
						ticketPlayer.sendMessage(
								Messages.getTicketComment(ticket.getTicketID(), src.getName()));
					}

					src.sendMessage(Messages.getTicketCommentUser(ticket.getTicketID()));
					return CommandResult.success();
				}
			}
			throw new CommandException(Messages.getTicketNotExist(ticketID));
		}
	}

	private Consumer<CommandSource> changeTicketComment ( int ticketID, String comment, String name ) {
		return consumer -> {
			final List<TicketData> tickets =
					new ArrayList<TicketData>(plugin.getDataStore().getTicketData());
			for (TicketData ticket : tickets) {
				if (ticket.getTicketID() == ticketID) {
					ticket.setComment(comment);

					try {
						plugin.getDataStore().updateTicketData(ticket);
					} catch (Exception e) {
						consumer.sendMessage(Messages.getErrorGen("Unable to comment on ticket"));
						e.printStackTrace();
					}

					Optional<Player> ticketPlayerOP = Sponge.getServer().getPlayer(ticket.getPlayerUUID());
					if (ticketPlayerOP.isPresent()) {
						Player ticketPlayer = ticketPlayerOP.get();
						ticketPlayer.sendMessage(Messages.getTicketComment(ticket.getTicketID(), name));
					}

					consumer.sendMessage(Messages.getTicketCommentUser(ticket.getTicketID()));
				}
			}
		};
	}
}
