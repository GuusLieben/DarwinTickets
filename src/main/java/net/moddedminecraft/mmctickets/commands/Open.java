package net.moddedminecraft.mmctickets.commands;

import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;

import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.config.Config;
import net.moddedminecraft.mmctickets.config.Messages;
import net.moddedminecraft.mmctickets.data.PlotSuspension;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.data.TicketStatus;
import net.moddedminecraft.mmctickets.util.CommonUtil;
import net.moddedminecraft.mmctickets.util.DiscordUtil;
import net.moddedminecraft.mmctickets.util.DiscordUtil.DiscordTicketStatus;

import org.jetbrains.annotations.Nullable;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Open implements CommandExecutor {

    private final Main plugin;

    public Open(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        String message = args.<String>getOne("message").get();
    /*if (!(src instanceof Player)) {
        throw new CommandException(Messages.getErrorGen("Only players can run this command"));
    }*/

        if (src instanceof Player) {
            @Nullable Plot plot = null;
            try {
                Location location =
                        new Location(
                                ((Player) src).getWorld().getName(),
                                ((Player) src).getLocation().getBlockX(),
                                ((Player) src).getLocation().getBlockY(),
                                ((Player) src).getLocation().getBlockZ());

                plot = Plot.getPlot(location);
                if (plot != null) {
                    message = plot.getWorldName() + "  |  " + plot.getId().toString();
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            Player player = (Player) src;
            UUID uuid = player.getUniqueId();

            if (plot != null && !plot.getOwners().contains(player.getUniqueId())) plot = null;

            if (null != plot) {

                Optional<PlotSuspension> suspension = this.plugin.getDataStore().getSuspension(plot);
                if (suspension.isPresent()) {
                    int[] cooldown = splitToComponentTimes(suspension.get().suspendedTo - System.currentTimeMillis());
                    player.sendMessage(Text.of(TextColors.RED, "You cannot submit this plot for the next " + cooldown[0] + " hours, " + cooldown[1] + " mins and " + cooldown[2] + " secs"));
                    return CommandResult.empty();
                }

            }

            if (Config.server.isEmpty()) {
                throw new CommandException(Messages.getErrorGen("Server name inside config is not set"));
            }
            if (plugin != null && plugin.getWaitTimer() != null && src != null && plugin.getWaitTimer().contains(src.getName())) {
                throw new CommandException(Messages.getTicketTooFast(Config.delayTimer));
            }
            final List<TicketData> tickets =
                    new ArrayList<TicketData>(plugin.getDataStore().getTicketData());
            int totalTickets = 0;
            boolean duplicate = false;
            int ticketID = tickets.size() + 1;

            if (!tickets.isEmpty()) {
                for (TicketData ticket : tickets) {
                    if (ticket.getTicketID() == ticketID) {
                        ticketID++;
                    }
                    if (ticket.getPlayerUUID().equals(uuid)
                            && ticket.getStatus() != TicketStatus.CLOSED
                            && ticket.getStatus() != TicketStatus.REJECTED
                            && ticket.getStatus() != TicketStatus.APPROVED
                    ) {
                        totalTickets++;
                    }
                    if (Config.preventDuplicates) {
                        if (ticket.getMessage().equals(message)
                                && ticket.getStatus() != TicketStatus.CLOSED
                                && ticket.getStatus() != TicketStatus.REJECTED
                                && ticket.getStatus() != TicketStatus.APPROVED
                                && ticket.getPlayerUUID().equals(uuid)) {
                            duplicate = true;
                        }
                    }
                }
            }

            if (duplicate) {
                throw new CommandException(Messages.getTicketDuplicate());
            }
            if (totalTickets >= Config.maxTickets) {
                throw new CommandException(Messages.getTicketTooMany());
            }
            if (message.split("\\s+").length < Config.minWords) {
                throw new CommandException(Messages.getTicketTooShort(Config.minWords));
            }

//      final List<PlayerData> playerData =
//          new ArrayList<PlayerData>(plugin.getDataStore().getPlayerData());
//      for (PlayerData pData : playerData) {
//        if (pData.getPlayerName().equals(src.getName()) && pData.getBannedStatus() == 1) {
//          throw new CommandException(Messages.getErrorBanned());
//        }
//      }

            if (plot != null) {
                try {
                    TicketData ticketData =
                            new TicketData(
                                    ticketID,
                                    String.valueOf(uuid),
                                    null,
                                    "",
                                    System.currentTimeMillis() / 1000,
                                    player.getWorld().getName(),
                                    player.getLocation().getBlockX(),
                                    player.getLocation().getBlockY(),
                                    player.getLocation().getBlockZ(),
                                    player.getHeadRotation().getX(),
                                    player.getHeadRotation().getY(),
                                    message,
                                    TicketStatus.OPEN,
                                    0,
                                    Config.server,
                                    null);

                    player.sendMessage(Messages.getTicketOpenUser(ticketID));
                    if (Config.staffNotification) {
                        CommonUtil.notifyOnlineStaffOpen(
                                Messages.getTicketOpen(player.getName(), ticketID), ticketID);
                    }
                    if (Config.titleNotification) {
                        CommonUtil.notifyOnlineStaffTitle(
                                Messages.getTicketTitleNotification(player.getName(), ticketID));
                    }
                    if (Config.soundNotification) {
                        CommonUtil.notifyOnlineStaffSound();
                    }

                    DiscordUtil.sendToChannel(Color.YELLOW, player.getName(), ticketData, DiscordTicketStatus.NEW, plot, (msg -> {
                        plugin.getLogger().warn("Ticket opened, Discord ID assigned : " + msg.getId());
                        ticketData.setDiscordMessage(msg.getId());
                        plugin.getDataStore().addTicketData(ticketData);
                    }));
                }
                catch (Exception e) {
                    player.sendMessage(Messages.getErrorGen("Data was not saved correctly."));
                    e.printStackTrace();
                }
            }
            else {
                src.sendMessage(
                        Text.of(
                                TextColors.DARK_GRAY,
                                "[] ",
                                TextColors.RED,
                                "You can only open a submission while standing inside your own plot!"));
            }
            plugin.getWaitTimer().add(src.getName());

            Sponge.getScheduler()
                    .createTaskBuilder()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    plugin.getWaitTimer().removeAll(Collections.singleton(src.getName()));
                                }
                            })
                    .delay(Config.delayTimer, TimeUnit.SECONDS)
                    .name("mmctickets-s-openTicketWaitTimer")
                    .submit(this.plugin);

            return CommandResult.success();
        }
        else {
            if (Config.server.isEmpty()) {
                throw new CommandException(Messages.getErrorGen("Server name inside config is not set"));
            }

            final List<TicketData> tickets =
                    new ArrayList<TicketData>(plugin.getDataStore().getTicketData());
            int ticketID = tickets.size() + 1;

            try {
                plugin
                        .getDataStore()
                        .addTicketData(
                                new TicketData(
                                        ticketID,
                                        UUID.fromString("00000000-0000-0000-0000-000000000000").toString(),
                                        null,
                                        "",
                                        System.currentTimeMillis() / 1000,
                                        Sponge.getServer().getDefaultWorldName(),
                                        0,
                                        0,
                                        0,
                                        0.0,
                                        0.0,
                                        message,
                                        TicketStatus.OPEN,
                                        0,
                                        Config.server,
                                        null));

                src.sendMessage(Messages.getTicketOpenUser(ticketID));
                if (Config.staffNotification) {
                    CommonUtil.notifyOnlineStaffOpen(Messages.getTicketOpen("Console", ticketID), ticketID);
                }
                if (Config.titleNotification) {
                    CommonUtil.notifyOnlineStaffTitle(
                            Messages.getTicketTitleNotification("Console", ticketID));
                }
                if (Config.soundNotification) {
                    CommonUtil.notifyOnlineStaffSound();
                }
            }
            catch (Exception e) {
                src.sendMessage(Messages.getErrorGen("Data was not saved correctly."));
                e.printStackTrace();
            }

            return CommandResult.success();
        }
    }

    private static int[] splitToComponentTimes(long longVal) {
        double totalSeconds = Math.floor(longVal / 1000);
        int hours = (int) totalSeconds / 3600;
        int remainder = (int) totalSeconds - hours * 3600;
        int mins = remainder / 60;
        remainder -= mins * 60;
        int secs = remainder;

        int[] ints = { hours, mins, secs };
        return ints;
    }
}
