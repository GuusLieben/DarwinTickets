package net.moddedminecraft.mmctickets.commands;

import com.magitechserver.magibridge.MagiBridge;
import net.dv8tion.jda.core.EmbedBuilder;
import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.config.Messages;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.util.CommonUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.moddedminecraft.mmctickets.data.ticketStatus.Claimed;
import static net.moddedminecraft.mmctickets.data.ticketStatus.Open;

public class reopen implements CommandExecutor {
  private final Main plugin;

  public reopen(Main plugin) {
    this.plugin = plugin;
  }

  @Override
  public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
    final int ticketID = args.<Integer>getOne("ticketID").get();

    final List<TicketData> tickets =
        new ArrayList<TicketData>(plugin.getDataStore().getTicketData());

    if (tickets.isEmpty()) {
      throw new CommandException(Messages.getErrorGen("Tickets list is empty."));
    } else {
      for (TicketData ticket : tickets) {
        if (ticket.getTicketID() == ticketID) {
          if (ticket.getStatus() == Claimed || ticket.getStatus() == Open) {
            throw new CommandException(Messages.getErrorTicketNotClosed(ticketID));
          }
          if (ticket.getStatus() == Claimed) {
            throw new CommandException(
                Messages.getErrorTicketClaim(
                    ticket.getTicketID(),
                    CommonUtil.getPlayerNameFromData(plugin, ticket.getStaffUUID())));
          }
          ticket.setStatus(Open);
          ticket.setStaffUUID(UUID.fromString("00000000-0000-0000-0000-000000000000").toString());
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
          EmbedBuilder embedBuilder = new EmbedBuilder();
          embedBuilder.setColor(Color.YELLOW);
          embedBuilder.setTitle("New submission");
          embedBuilder.addField(
              "Submitted by : " + CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()),
              "ID assigned : " + ticketID + "\nPlot : " + ticket.getMessage(),
              false);
          embedBuilder.setThumbnail("https://app.buildersrefuge.com/img/created.png");

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
