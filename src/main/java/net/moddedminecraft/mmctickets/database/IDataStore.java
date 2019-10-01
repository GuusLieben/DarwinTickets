package net.moddedminecraft.mmctickets.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.moddedminecraft.mmctickets.data.PlayerData;
import net.moddedminecraft.mmctickets.data.TicketData;

public interface IDataStore {

	String getDatabaseName ();

	boolean load ();

	List<TicketData> getTicketData ();

	List<PlayerData> getPlayerData ();

	ArrayList<UUID> getNotifications ();

	Optional<TicketData> getTicket ( int ticketID );

	boolean addTicketData ( TicketData ticketData );

	boolean addPlayerData ( PlayerData playerData );

	boolean updateTicketData ( TicketData ticketData );

	boolean updatePlayerData ( PlayerData playerData );
}
