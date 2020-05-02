package net.moddedminecraft.mmctickets.util;

import com.intellectualcrafters.plot.object.Plot;
import com.magitechserver.magibridge.MagiBridge;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.data.TicketData;

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
        channel.getMessageById(messageId).queue(msg ->
                msg.editMessage(getEmbed(color, submitter, handler, ticketData, ticketStatus, plot)).queue()
        );
    }

    private static MessageEmbed getEmbed(Color color, String submitter, CommandSource handler, TicketData ticketData, DiscordTicketStatus ticketStatus, Plot plot) {
        String comment = ticketData.getComment() == null || ticketData.getComment().equals("") ? "None" : ticketData.getComment();
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(ticketStatus.title)
                .setDescription("Submitted by : " + submitter)
                .setColor(color)
                .setTimestamp(OffsetDateTime.now())
                .setFooter("ID #" + ticketData.getTicketID(), null)
                .setThumbnail(ticketStatus.imageUrl)
                .addField("World", plot.getWorldName(), true)
                .addField("Plot", plot.getId().toString(), true)
                .addField("Player UUID", ticketData.getPlayerUUID().toString(), true)
                .addField("Comment(s)", comment, true);

        if (handler != null) embedBuilder.addField("Handled by", handler.getName(), false);

        return embedBuilder.build();
    }

}
