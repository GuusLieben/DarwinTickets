package net.moddedminecraft.mmctickets.commands;

import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;

import net.moddedminecraft.mmctickets.Main;
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
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddStaff implements CommandExecutor {

    private final Main plugin;

    public AddStaff(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        final int ticketID = args.<Integer>getOne("ticketID").get();
        final User user = args.<Player>getOne("player").get();

        if (user.hasPermission(Permissions.COMMAND_TICKET_CLAIM)) {
            final List<TicketData> tickets =
                    new ArrayList<TicketData>(plugin.getDataStore().getTicketData());

            for (TicketData ticket : tickets) {
                if (ticket.getTicketID() == ticketID) {
                    if (Arrays.asList(ticket.getAdditionalReviewers()).contains(user.getName())) {
                        src.sendMessage(Text.of(TextColors.GRAY, "[] ", TextColors.RED, "That reviewer is already assigned to this ticket"));
                        return CommandResult.success();
                    }
                    ticket.addAdditionalReviewer(user.getName());
                    plugin.getDataStore().updateTicketData(ticket);
                    Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "lp user " + user.getName() + " permission set plots.admin.build.other true");
                    Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "lp user " + user.getName() + " permission set plots.admin.destroy.other true");
                    Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "lp user " + user.getName() + " permission set plots.admin.interact.other true");
                    CommonUtil.notifyOnlineStaff(Text.of(TextColors.GRAY, "[] ", TextColors.DARK_AQUA, src.getName(), TextColors.AQUA, " added ", TextColors.DARK_AQUA, user.getName(), TextColors.AQUA, " to ticket #" + ticketID));

                    Location location = new Location(ticket.getWorld(), ticket.getX(), ticket.getY(), ticket.getZ());
                    Plot plot = Plot.getPlot(location);
                    DiscordUtil.editMessage(ticket.getDiscordMessage(), Color.GREEN, CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()), src, ticket, DiscordTicketStatus.CLAIMED, plot);
                }
            }
        } else {
            src.sendMessage(Text.of(TextColors.GRAY, "[] ", TextColors.RED, "That player is not a reviewer!"));
        }

        return CommandResult.success();
    }
}
