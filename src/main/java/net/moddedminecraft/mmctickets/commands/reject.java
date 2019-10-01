package net.moddedminecraft.mmctickets.commands;

import com.magitechserver.magibridge.MagiBridge;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.service.NucleusMailService;
import net.dv8tion.jda.core.EmbedBuilder;
import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.data.PlayerData;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.util.CommonUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class reject implements CommandExecutor {

  private final Main plugin;

  public reject(Main plugin) {
    this.plugin = plugin;
  }

  @Override
  public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
    args.putArg("rejected", true);
    new close(plugin).execute(src, args);
    Optional<NucleusMailService> mailServiceOptional =
        Nucleus.getNucleus().getInternalServiceManager().getService(NucleusMailService.class);
    if (mailServiceOptional.isPresent()) {
      final List<TicketData> tickets =
          new ArrayList<TicketData>(plugin.getDataStore().getTicketData());
      final Optional<Integer> ticketIDOp = args.<Integer>getOne("ticketID");
      if (ticketIDOp.isPresent()) {
        Optional<TicketData> ticketOpt =
            tickets.stream().filter(t -> t.getTicketID() == ticketIDOp.get()).findFirst();
        if (ticketOpt.isPresent()) {
          TicketData ticket = ticketOpt.get();
          String playerName = CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID());
          Sponge.getCommandManager()
              .process(
                  Sponge.getServer().getConsole(),
                  "cu execute whenonline "
                      + playerName
                      + " *plaintell "
                      + playerName
                      + " &8[] &cYour application was reviewed but was not yet approved, make sure to read the given feedback on your plot, and apply again once ready!");
          src.sendMessage(
              Text.of(
                  TextColors.GRAY,
                  "[] ",
                  TextColors.AQUA,
                  "Rejected and closed ticket #" + ticket.getTicketID()));
//          EmbedBuilder embedBuilder = new EmbedBuilder();
//          embedBuilder.setColor(Color.RED);
//          embedBuilder.setTitle("Submission rejected");
//          embedBuilder.addField(
//              "Submitted by : " + CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()),
//              "ID : #"
//                  + ticket.getTicketID()
//                  + "\nRejected by : "
//                  + src.getName()
//                  + "\nSee close message for this ticket for more details",
//              false);
//          embedBuilder.setThumbnail("https://app.buildersrefuge.com/img/rejected.png");
//          MagiBridge.jda
//              .getTextChannelById("525424284731047946")
//              .getMessageById(ticket.getDiscordMessage())
//              .queue(
//                  msg -> {
//                    msg.editMessage(embedBuilder.build()).queue();
//                  });
          //          MagiBridge.jda
          //              .getTextChannelById("525424284731047946")
          //              .sendMessage(embedBuilder.build())
          //              .queue();
        }
      }
    }
    return CommandResult.success();
  }
}
