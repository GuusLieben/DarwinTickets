package net.moddedminecraft.mmctickets.commands;

import com.magitechserver.magibridge.MagiBridge;
import net.dv8tion.jda.core.EmbedBuilder;
import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.config.Messages;
import net.moddedminecraft.mmctickets.config.Permissions;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.util.CommonUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

import java.awt.*;
import java.sql.Date;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.moddedminecraft.mmctickets.data.ticketStatus.Claimed;
import static net.moddedminecraft.mmctickets.data.ticketStatus.Closed;

public class close implements CommandExecutor {

  private final Main plugin;

  public close(Main plugin) {
    this.plugin = plugin;
  }

  @Override
  public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

    final int ticketID = args.<Integer>getOne("ticketID").get();
    final Optional<Boolean> rejected = args.<Boolean>getOne("rejected");
    final Optional<String> commentOP = args.<String>getOne("comment");

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
          if (ticket.getPlayerUUID().equals(uuid)
              && !src.hasPermission(Permissions.COMMAND_TICKET_CLOSE_SELF)) {
            throw new CommandException(
                Messages.getErrorPermission(Permissions.COMMAND_TICKET_CLOSE_SELF));
          }
          if (!ticket.getPlayerUUID().equals(uuid)
              && !src.hasPermission(Permissions.COMMAND_TICKET_CLOSE_ALL)) {
            throw new CommandException(Messages.getErrorTicketOwner());
          }
          if (ticket.getStatus() == Closed) {
            throw new CommandException(Messages.getErrorTicketAlreadyClosed());
          }
          if (ticket.getStatus() == Claimed
              && !ticket.getStaffUUID().equals(uuid)
              && !src.hasPermission(Permissions.CLAIMED_TICKET_BYPASS)) {
            throw new CommandException(
                Messages.getErrorTicketClaim(
                    ticket.getTicketID(),
                    CommonUtil.getPlayerNameFromData(plugin, ticket.getStaffUUID())));
          }
          if (commentOP.isPresent()) {
            String comment = commentOP.get();
            ticket.setComment(comment);
          }
          ticket.setStatus(Closed);
          ticket.setStaffUUID(uuid.toString());

          CommonUtil.notifyOnlineStaff(Messages.getTicketClose(ticketID, src.getName()));
          Optional<Player> ticketPlayerOP = Sponge.getServer().getPlayer(ticket.getPlayerUUID());
          if (ticketPlayerOP.isPresent()) {
            Player ticketPlayer = ticketPlayerOP.get();
            ticketPlayer.sendMessage(
                Messages.getTicketCloseUser(ticket.getTicketID(), src.getName()));
            ticket.setNotified(1);
          } else {
            plugin.getDataStore().getNotifications().add(ticket.getPlayerUUID());
          }
          EmbedBuilder embedBuilder = new EmbedBuilder();
          embedBuilder.setColor(Color.PINK);

          String rank;
          switch (ticket.getWorld()) {
            case "Plots1":
              rank = "Member";
              break;
            case "Plots2":
              rank = "Expert";
              break;
            case "MasterPlots":
              rank = "Mastered Skill";
              break;
            default:
              rank = "Unknown";
          }

          if (rejected.isPresent()) {
            embedBuilder.setTitle("Submission rejected");
          } else {
            embedBuilder.setTitle("Submission approved");
          }

          embedBuilder.addField(
              "Submitted by : " + CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()),
              MessageFormat.format(
                      "ID : #{0}\nPlot : {1}\nClosed by : {2}\nComments : {3}\nTime closed : {4}",
                      ticketID,
                      ticket.getMessage(),
                      src.getName(),
                      ticket.getComment().length() == 0 ? "None" : ticket.getComment(),
                      LocalDateTime.now().toString())
                  + (rejected.isPresent() ? "" : "\nPromoted to : " + rank),
              false);
          if (rejected.isPresent()) {
            embedBuilder.setColor(Color.RED);
            embedBuilder.setThumbnail("https://app.buildersrefuge.com/img/rejected.png");
          } else {
            embedBuilder.setColor(Color.GREEN);
            embedBuilder.setThumbnail("https://app.buildersrefuge.com/img/approved.png");
          }

          MagiBridge.jda
              .getTextChannelById("525424284731047946")
              .getMessageById(ticket.getDiscordMessage())
              .queue(msg -> msg.editMessage(embedBuilder.build()).queue());

          try {
            plugin.getDataStore().updateTicketData(ticket);
          } catch (Exception e) {
            src.sendMessage(Messages.getErrorGen("Unable to close ticket"));
            e.printStackTrace();
          }
          return CommandResult.success();
        }
      }
      throw new CommandException(Messages.getTicketNotExist(ticketID));
    }
  }
}
