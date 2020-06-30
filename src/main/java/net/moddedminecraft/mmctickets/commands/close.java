package net.moddedminecraft.mmctickets.commands;

import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.config.Messages;
import net.moddedminecraft.mmctickets.config.Permissions;
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
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.moddedminecraft.mmctickets.data.ticketStatus.APPROVED;
import static net.moddedminecraft.mmctickets.data.ticketStatus.CLAIMED;
import static net.moddedminecraft.mmctickets.data.ticketStatus.CLOSED;
import static net.moddedminecraft.mmctickets.data.ticketStatus.REJECTED;

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
          if ( ticket.getStatus() == CLOSED || ticket.getStatus() == REJECTED || ticket.getStatus() == APPROVED) {
            throw new CommandException(Messages.getErrorTicketAlreadyClosed());
          }
          if (ticket.getStatus() == CLAIMED
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

          if (rejected.isPresent() && rejected.get()) ticket.setStatus(REJECTED);
          else ticket.setStatus(APPROVED);
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

          String playerName = CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID());
          Location location = new Location(ticket.getWorld(), ticket.getX(), ticket.getY(), ticket.getZ());
          Plot plot = Plot.getPlot(location);

          if (rejected.isPresent() && rejected.get()) {
            DiscordUtil.editMessage(ticket.getDiscordMessage(), Color.RED, playerName, src, ticket, DiscordTicketStatus.REJECTED, plot);
          } else {
            DiscordUtil.editMessage(ticket.getDiscordMessage(), Color.GREEN, playerName, src, ticket, DiscordTicketStatus.APPROVED, plot);
          }

          try {
            plugin.getDataStore().updateTicketData(ticket);
          } catch (Exception e) {
            src.sendMessage(Messages.getErrorGen("Unable to close ticket"));
            e.printStackTrace();
          }

          for (String staff : ticket.getAdditionalReviewers()) {
            Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "lp user " + staff + " permission unset plots.admin.build.other");
            Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "lp user " + staff + " permission unset plots.admin.destroy.other");
            Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "lp user " + staff + " permission unset plots.admin.interact.other");
          }

          src.sendMessage(Text.of(TextColors.GRAY, "[] ", TextColors.AQUA, "Deactivated reviewer plot bypass."));
          return CommandResult.success();
        }
      }
      throw new CommandException(Messages.getTicketNotExist(ticketID));
    }
  }
}
