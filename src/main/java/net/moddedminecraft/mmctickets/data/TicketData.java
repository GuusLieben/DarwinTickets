package net.moddedminecraft.mmctickets.data;

import com.google.common.reflect.TypeToken;
import java.util.List;
import net.moddedminecraft.mmctickets.util.TicketDataUtil;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class TicketData extends TicketDataUtil {

	public TicketData (
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
			ticketStatus status,
			int notified,
			String server ) {
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
				server);
	}

	public static class TicketSerializer implements TypeSerializer<TicketData> {
		@SuppressWarnings ("serial")
		public static final TypeToken<List<TicketData>> token = new TypeToken<List<TicketData>>() {
		};

		@Override
		public TicketData deserialize ( TypeToken<?> token, ConfigurationNode node )
				throws ObjectMappingException {
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
							ticketStatus.valueOf(node.getNode("status").getString()),
							node.getNode("notified").getInt(),
							node.getNode("server").getString());
			data.setDiscordMessage(node.getNode("discord").getString());
			return data;
		}

		@Override
		public void serialize ( TypeToken<?> token, TicketData ticket, ConfigurationNode node )
				throws ObjectMappingException {
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
		}
	}
}
