package net.moddedminecraft.mmctickets.util;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.intellectualcrafters.plot.object.Plot;
import com.magitechserver.magibridge.MagiBridge;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.World;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.TextChannel;
import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.data.PlotSuspension;
import net.moddedminecraft.mmctickets.data.TicketComment;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.data.TicketStatus;

public class DiscordUtil {

    private static Main plugin = null;
    public static final String channelId = "525424284731047946";

    public static void setPlugin(Main plugin) {
        DiscordUtil.plugin = plugin;
    }

    public static DiscordTicketStatus convertStatus(TicketStatus status) {
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
        HOLD("https://icon-library.net/images/stop-sign-icon-png/stop-sign-icon-png-8.jpg", "Submission on hold"),
        SUSPENDED("https://icon-library.net/images/stop-sign-icon-png/stop-sign-icon-png-8.jpg", "Plot suspended");

        String imageUrl;
        String title;

        DiscordTicketStatus(String imageUrl, String title) {
            this.imageUrl = imageUrl;
            this.title = title;
        }
    }

    public static void sendToChannel(Color color, String submitter, TicketData ticketData, DiscordTicketStatus ticketStatus, Plot plot, Consumer<Message> consumer) {
        TextChannel channel = MagiBridge.jda.getTextChannelById(channelId);

        if (channel != null)
            channel.sendMessage(getEmbed(color, submitter, ticketData, ticketStatus, plot)).queue(consumer);
    }

    public static void editMessage(String messageId, Color color, String submitter, TicketData ticketData, DiscordTicketStatus ticketStatus, Plot plot) {
        TextChannel channel = MagiBridge.jda.getTextChannelById(channelId);
        channel.retrieveMessageById(messageId).queue(msg ->
                msg.editMessage(getEmbed(color, submitter, ticketData, ticketStatus, plot)).queue()
        );
    }

    private static MessageEmbed getEmbed(Color color, String submitter, TicketData ticketData, DiscordTicketStatus ticketStatus, Plot plot) {
        final List<TicketData> tickets = new ArrayList<>(plugin.getDataStore().getTicketData());
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
            .addField("Comment(s)", comment, true)
            .addField("Handled by", String.join(", ", ticketData.getAdditionalReviewers()), false);

        return embedBuilder.isValidLength(AccountType.BOT) ? embedBuilder.build() : truncateEmbedComments(embedBuilder.build(), comments);
    }

    private static MessageEmbed truncateEmbedComments(final MessageEmbed embed, final List<TicketComment> ticketComments) {
        final String omittedText = "Comments ommited: ";
        final List<String> comments = ticketComments.stream()
            .map(comment -> "[" + comment.getSource() + " on #" + comment.getTicketId() + "]: " + comment.getComment())
            .collect(Collectors.toList());
        final int commentsOmitted = calculateOmittedComments(comments, omittedText.length());
        final List<String> truncatedComments = comments.subList(0, commentsOmitted);
        truncatedComments.add(omittedText + commentsOmitted);

        EmbedBuilder truncatedEmbedBuilder = new EmbedBuilder()
            .setTitle(embed.getTitle())
            .setDescription(embed.getDescription())
            .setColor(embed.getColor())
            .setTimestamp(embed.getTimestamp())
            .setFooter(embed.getFooter().getText(), embed.getFooter().getIconUrl())
            .setThumbnail(embed.getThumbnail().getUrl());

        for(Field field : embed.getFields()) {
            if(field.getName().equals("Comment(s)")) field = new Field(field.getName(), String.join("\n", truncatedComments), field.isInline());
            truncatedEmbedBuilder.addField(field);
        }

        return truncatedEmbedBuilder.build();
    }

    private static int calculateOmittedComments(final List<String> comments, final int additionalTextLength) {
        Collections.reverse(comments);        
        int limitDifference = comments.size() - MessageEmbed.VALUE_MAX_LENGTH;
        int omittedComments = 0;
        for(String comment : comments) {
            if(limitDifference < 0) break;
            limitDifference -= comment.length() + 1; // +1 for every next "\n" (new line) character
            omittedComments++;
        }

        return limitDifference + (additionalTextLength + omittedComments) > 0 ? omittedComments + 1 : omittedComments; //Skip one more comment if omitted comment info exceeds the limit
    }

    public static void sendSuspension(String suspendedBy, PlotSuspension suspension) {
        TextChannel channel = MagiBridge.jda.getTextChannelById(channelId);

        Optional<World> world = Sponge.getServer().getWorld(suspension.plotWorldId);
        String worldName = world.map(World::getName).orElse("Unknown");

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        Date resultDate = new Date(suspension.suspendedTo);


        MessageEmbed embeded = new EmbedBuilder()
                .setTitle(DiscordTicketStatus.SUSPENDED.title)
                .setDescription("Suspended by : " + suspendedBy)
                .setColor(Color.ORANGE)
                .setTimestamp(OffsetDateTime.now())
                .setFooter("ID #" + suspension.suspensionId, null)
                .setThumbnail(DiscordTicketStatus.SUSPENDED.imageUrl)
                .addField("World", worldName, true)
                .addField("Plot", suspension.plotX + ";" + suspension.plotY, true)
                .addField("Suspended until", sdf.format(resultDate), true)
                .build();


        if (channel != null)
            channel.sendMessage(embeded).queue();
    }

}
