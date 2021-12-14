package net.moddedminecraft.mmctickets.data;

import com.google.common.reflect.TypeToken;

import net.moddedminecraft.mmctickets.util.TicketDataUtil;

import org.jetbrains.annotations.NotNull;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class TicketData extends TicketDataUtil {

    public TicketData(
            int ticketID,
            String playerUUID,
            String staffUUID,
            String comment,
            long timestamp,
            String world,
            int x,
            int y,
            int z,
            Double yaw,
            Double pitch,
            String message,
            TicketStatus status,
            int notified,
            String server,
            String additionalReviewers) {
        super(
                ticketID,
                playerUUID,
                staffUUID,
                comment,
                timestamp,
                world,
                x,
                y,
                z,
                yaw,
                pitch,
                message,
                status,
                notified,
                server,
                additionalReviewers);
    }

    public static class TicketSerializer implements TypeSerializer<TicketData> {

        @Override
        public TicketData deserialize(@NotNull TypeToken<?> token, ConfigurationNode node) {
            TicketData data =
                    new TicketData(
                            node.getNode("ticketID").getInt(),
                            node.getNode("playerUUID").getString(),
                            node.getNode("staffUUID").getString(),
                            node.getNode("comment").getString(),
                            node.getNode("timestamp").getInt(),
                            node.getNode("world").getString(),
                            node.getNode("x").getInt(),
                            node.getNode("y").getInt(),
                            node.getNode("z").getInt(),
                            node.getNode("yaw").getDouble(),
                            node.getNode("pitch").getDouble(),
                            node.getNode("message").getString(),
                            TicketStatus.fromString(node.getNode("status").getString()),
                            node.getNode("notified").getInt(),
                            node.getNode("server").getString(),
                            node.getNode("additional_staff").getString());
            data.setDiscordMessage(node.getNode("discord").getString());
            return data;
        }

        @Override
        public void serialize(@NotNull TypeToken<?> token, TicketData ticket, ConfigurationNode node) {
            node.getNode("ticketID").setValue(ticket.ticketID);
            node.getNode("playerUUID").setValue(ticket.playerUUID);
            node.getNode("staffUUID").setValue(ticket.staffUUID);
            node.getNode("comment").setValue(ticket.comment);
            node.getNode("timestamp").setValue(ticket.timestamp);
            node.getNode("world").setValue(ticket.world);
            node.getNode("x").setValue(ticket.x);
            node.getNode("y").setValue(ticket.y);
            node.getNode("z").setValue(ticket.z);
            node.getNode("yaw").setValue(ticket.yaw);
            node.getNode("pitch").setValue(ticket.pitch);
            node.getNode("message").setValue(ticket.message);
            node.getNode("status").setValue(ticket.status.toString());
            node.getNode("notified").setValue(ticket.notified);
            node.getNode("server").setValue(ticket.server);
            node.getNode("discord").setValue(ticket.discordMessage);
            node.getNode("additional_staff").setValue(ticket.getAdditionalStaff());
        }
    }
}
