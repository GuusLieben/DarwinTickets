package net.moddedminecraft.mmctickets.util;

import com.intellectualcrafters.plot.object.Plot;
import com.magitechserver.magibridge.MagiBridge;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.data.TicketComment;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.data.ticketStatus;

import org.spongepowered.api.command.CommandSource;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DiscordUtil {

    private static Main plugin = null;
    private static final String channelId = "525424284731047946";

    public static void setPlugin(Main plugin) {
        DiscordUtil.plugin = plugin;
    }

    public static DiscordTicketStatus convertStatus(ticketStatus status) {
        switch (status) {
            case CLOSED:
                return DiscordTicketStatus.REJECTED;
            case OPEN:
                return DiscordTicketStatus.NEW;
            case HELD:
                return DiscordTicketStatus.HOLD;
            default:
                return DiscordTicketStatus.valueOf(status.toString().toUpperCase());
        }
    }

    public enum DiscordTicketStatus {
        NEW("https://app.buildersrefuge.com/img/created.png", "New submission"),
        CLAIMED("https://webstockreview.net/images/green-clipart-magnifying-glass.png", "Submission claimed"),
        REJECTED("https://app.buildersrefuge.com/img/rejected.png", "Submission rejected"),
        APPROVED("https://app.buildersrefuge.com/img/approved.png", "Submission approved"),
        HOLD("https://icon-library.net/images/stop-sign-icon-png/stop-sign-icon-png-8.jpg", "Submission on hold");

        String imageUrl;
        String title;

        DiscordTicketStatus(String imageUrl, String title) {
            this.imageUrl = imageUrl;
            this.title = title;
        }
    }

    public static void sendToChannel(Color color, String submitter, CommandSource handler, TicketData ticketData, DiscordTicketStatus ticketStatus, Plot plot, Consumer<Message> consumer) {
        TextChannel channel = MagiBridge.jda.getTextChannelById(channelId);
        channel.sendMessage(getEmbed(color, submitter, handler, ticketData, ticketStatus, plot)).queue(consumer);
    }

    public static void editMessage(String messageId, Color color, String submitter, CommandSource handler, TicketData ticketData, DiscordTicketStatus ticketStatus, Plot plot) {
        TextChannel channel = MagiBridge.jda.getTextChannelById(channelId);
        channel.retrieveMessageById(messageId).queue(msg ->
                msg.editMessage(getEmbed(color, submitter, handler, ticketData, ticketStatus, plot)).queue()
        );
    }

    private static MessageEmbed getEmbed(Color color, String submitter, CommandSource handler, TicketData ticketData, DiscordTicketStatus ticketStatus, Plot plot) {
        final List<TicketData> tickets = new ArrayList<TicketData>(plugin.getDataStore().getTicketData());
        int ticketNum = (int) tickets.stream().filter(t -> t.getPlayerUUID().equals(ticketData.getPlayerUUID()) && t.getMessage().equals(ticketData.getMessage())).count();

        List<TicketComment> comments = plugin.getDataStore().getComments(ticketData.getMessage(), ticketData.getPlayerUUID());
        StringBuilder builder = new StringBuilder();
        for (TicketComment comment : comments) {
            if (builder.length() > 0) builder.append("\n");
            builder.append("[").append(comment.getSource()).append(" on #").append(comment.getTicketId()).append("]: ").append(comment.getComment());
        }

        String comment = builder.toString();
        if (comments.isEmpty()) comment = "None";

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(ticketStatus.title)
                .setDescription("Submitted by : " + submitter)
                .setColor(color)
                .setTimestamp(OffsetDateTime.now())
                .setFooter("ID #" + ticketData.getTicketID() + " | Submission #" + (ticketNum + 1), null)
                .setThumbnail(ticketStatus.imageUrl)
                .addField("World", plot.getWorldName(), true)
                .addField("Plot", plot.getId().toString(), true)
                .addField("Player UUID", ticketData.getPlayerUUID().toString(), true)
                .addField("Comment(s)", comment, true);

        if (handler != null)
            embedBuilder.addField("Handled by", ticketData.getAdditionalStaff().replaceAll(",", ", "), false);

        return embedBuilder.build();
    }

}
