package net.moddedminecraft.mmctickets;

import com.intellectualcrafters.plot.object.Plot;
import com.magitechserver.magibridge.MagiBridge;
import com.plotsquared.sponge.events.PlayerEnterPlotEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.moddedminecraft.mmctickets.config.Messages;
import net.moddedminecraft.mmctickets.config.Permissions;
import net.moddedminecraft.mmctickets.data.PlotSuspension;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.data.TicketStatus;
import net.moddedminecraft.mmctickets.util.CommonUtil;
import net.moddedminecraft.mmctickets.util.DiscordUtil;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.awt.Color;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

public class EventListener extends ListenerAdapter {

    private Main plugin;

    public EventListener(Main instance) {
        plugin = instance;
    }

    @Listener
    public void onPlayerLogin(ClientConnectionEvent.Join event, @Root Player player) {
        // Notify a player if a ticket they created was closed while they were offline
        if (plugin.getDataStore().getNotifications().contains(player.getUniqueId())) {
            final List<TicketData> tickets =
                    new ArrayList<>(plugin.getDataStore().getTicketData());
            int totalTickets = 0;
            for (TicketData ticket : tickets) {
                if (ticket.getPlayerUUID().equals(player.getUniqueId()) && ticket.getNotified() == 0) {
                    totalTickets++;
                    ticket.setNotified(1);
                    try {
                        plugin.getDataStore().updateTicketData(ticket);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            plugin
                    .getDataStore()
                    .getNotifications()
                    .removeAll(Collections.singleton(player.getUniqueId()));
            final int finalTotalTickets = totalTickets;
            Sponge.getScheduler()
                    .createTaskBuilder()
                    .execute(() -> {
                        if (finalTotalTickets < 2) {
                            player.sendMessage(Messages.getTicketCloseOffline());
                        }
                        else {
                            player.sendMessage(
                                    Messages.getTicketCloseOfflineMulti(finalTotalTickets, "check self"));
                        }
                    })
                    .delay(5, TimeUnit.SECONDS)
                    .name("mmctickets-s-sendUserNotifications")
                    .submit(this.plugin);
        }

        // Notify staff of the current open tickets when they login
        if (player.hasPermission(Permissions.STAFF)) {
            final List<TicketData> tickets = new ArrayList<>(plugin.getDataStore().getTicketData());
            int openTickets = 0;
            int heldTickets = 0;
            for (TicketData ticket : tickets) {
                if (ticket.getStatus() == TicketStatus.OPEN) openTickets++;
                if (ticket.getStatus() == TicketStatus.HELD) heldTickets++;
            }
            final int finalOpen = openTickets;
            final int finalHeld = heldTickets;
            Sponge.getScheduler()
                    .createTaskBuilder()
                    .execute(() -> {
                        if (finalOpen == 0) {
                            player.sendMessage(Messages.getTicketReadNone());
                        }
                        if (finalOpen > 0 && finalHeld == 0) {
                            player.sendMessage(Messages.getTicketUnresolved(finalOpen, "check"));
                        }
                        if (finalOpen > 0 && finalHeld > 0) {
                            player.sendMessage(
                                    Messages.getTicketUnresolvedHeld(finalOpen, finalHeld, "check"));
                        }
                    })
                    .delay(3, TimeUnit.SECONDS)
                    .name("mmctickets-s-sendStaffNotifications")
                    .submit(this.plugin);
        }
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.getChannel().getId().equals(DiscordUtil.channelId)) {
            if (event.getMessage().getContentRaw().startsWith(".check")) {
                String command = event.getMessage().getContentRaw();
                final List<TicketData> tickets = new ArrayList<>(plugin.getDataStore().getTicketData());
                if (command.split(" ").length == 2) {
                    String id = command.split(" ")[1];
                    int ticketId = Integer.parseInt(id);
                    Optional<TicketData> optionalTicket =
                            tickets.stream().filter(ticket -> ticket.getTicketID() == ticketId).findFirst();
                    if (optionalTicket.isPresent()) {
                        TicketData ticketData = optionalTicket.get();
                        MessageBuilder message = new MessageBuilder();
                        EmbedBuilder embed = new EmbedBuilder();

                        String body = "";
                        String playerName =
                                CommonUtil.getPlayerNameFromData(plugin, ticketData.getPlayerUUID());
                        String age = CommonUtil.getTimeAgo(ticketData.getTimestamp());
                        String location =
                                ticketData.getWorld()
                                        + " | x: "
                                        + ticketData.getX()
                                        + ", y: "
                                        + ticketData.getY()
                                        + ", z: "
                                        + ticketData.getZ();

                        int ticketNum = (int) tickets.stream().filter(t -> t.getPlayerUUID()
                                .toString()
                                .equals(ticketData.getPlayerUUID().toString())
                                && t.getMessage().equals(ticketData.getMessage())
                        ).count();

                        for (String regex : new String[]{ "(&)([a-f])+", "(&)([0-9])+", "&l", "&n", "&o", "&k", "&m", "&r" })
                            age = age.replaceAll(regex, "");

                        body += "Player : " + playerName;
                        body += "\nSubmitted : " + age;
                        body += "\nLocation : " + location;
                        body += "\nPlot : " + ticketData.getMessage();
                        body += "\nSubmission : #" + ticketNum;
                        body += "\nComments/score : " + (ticketData.getComment().length() == 0 ? "None" : ticketData.getComment());

                        embed.setColor(Color.CYAN);

                        embed.setTitle("Ticket : #" + ticketData.getTicketID());
                        embed.addField("", body, true);

                        message.setEmbed(embed.build());
                        MagiBridge.jda.getTextChannelById(DiscordUtil.channelId).sendMessage(message.build()).queue();
                    }
                    else {
                        MagiBridge.jda.getTextChannelById(DiscordUtil.channelId).sendMessage(":no_entry: **Unable to find ticket**").queue();
                    }
                }
                else {
                    MessageBuilder message = new MessageBuilder();
                    EmbedBuilder embed = new EmbedBuilder();
                    AtomicInteger amount = new AtomicInteger();
                    tickets.stream()
                            .filter(t -> t.getStatus().equals(TicketStatus.OPEN))
                            .forEach(ticket -> {
                                amount.getAndIncrement();
                                String playerName =
                                        CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID());
                                String age = CommonUtil.getTimeAgo(ticket.getTimestamp());

                                String title =
                                        MessageFormat.format(
                                                "#{0} | {1}", ticket.getTicketID(), ticket.getMessage());

                                for (String regex :
                                        new String[]{
                                                "(&)([a-f])+", "(&)([0-9])+", "&l", "&n", "&o", "&k", "&m", "&r"
                                        })
                                    age = age.replaceAll(regex, "");

                                String body =
                                        MessageFormat.format("By : {0}\nSubmitted : {1}", playerName, age);
                                embed.addField(title, body, true);
                            });

                    if (amount.get() <= 3) embed.setColor(Color.CYAN);
                    else if (amount.get() >= 7) embed.setColor(Color.PINK);
                    else embed.setColor(Color.YELLOW);

                    embed.setTitle("Open tickets (" + amount.get() + ")");
                    message.setEmbed(embed.build());
                    MagiBridge.jda.getTextChannelById(DiscordUtil.channelId).sendMessage(message.build()).queue();
                }
            }
        }
    }

    @Listener
    public void onPlotJoin(PlayerEnterPlotEvent event) {
        Player player = event.getPlayer();
        Plot plot = event.getPlot();

        // Check if the plot disabled suspension notifications
        if (!plot.getFlag(this.plugin.getPlotFlagManager().SUSPENSION_FLAG, true))
            return;

        // Trigger only for the owners of the plot
        if (!plot.getOwners().contains(player.getUniqueId()))
            return;

        // Check if the player has a world id
        Optional<UUID> worldId = player.getWorldUniqueId();
        if (!worldId.isPresent())
            return;

        List<PlotSuspension> suspensions = new ArrayList<>(this.plugin.getDataStore().getSuspensionsData());
        Collections.sort(suspensions);
        Collections.reverse(suspensions);

        Optional<PlotSuspension> suspension = suspensions
                .stream()
                .filter(x -> x.plotWorldId == worldId.get()
                        && x.plotX == plot.getId().x
                        && x.plotY == plot.getId().y
                        && x.suspendedTo > System.currentTimeMillis())
                .findFirst();

        if (!suspension.isPresent()) return;

        int[] cooldown = splitToComponentTimes(suspension.get().suspendedTo - System.currentTimeMillis());
        player.sendMessage(Text.of(TextColors.YELLOW, "You cannot submit this plot for the next : " + cooldown[0] + " hours, " + cooldown[1] + " mins and " + cooldown[2] + " secs"));
    }

    private static int[] splitToComponentTimes(long longVal) {
        double totalSeconds = Math.floor(longVal / 1000);
        int hours = (int) totalSeconds / 3600;
        int remainder = (int) totalSeconds - hours * 3600;
        int mins = remainder / 60;
        remainder -= mins * 60;
        int secs = remainder;

        return new int[]{ hours, mins, secs };
    }

}
