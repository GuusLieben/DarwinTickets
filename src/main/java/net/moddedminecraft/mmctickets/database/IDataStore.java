package net.moddedminecraft.mmctickets.database;

import com.intellectualcrafters.plot.object.Plot;

import net.moddedminecraft.mmctickets.data.PlayerData;
import net.moddedminecraft.mmctickets.data.PlotSuspension;
import net.moddedminecraft.mmctickets.data.TicketComment;
import net.moddedminecraft.mmctickets.data.TicketData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IDataStore {

	boolean load ();

	List<TicketData> getTicketData ();

	Collection<PlotSuspension> getSuspensions ();

	Optional<PlotSuspension> getSuspension (Plot plot);

	List<PlotSuspension> getSuspensionsData();

	List<PlayerData> getPlayerData ();

	ArrayList<UUID> getNotifications ();

	Optional<TicketData> getTicket(int ticketID);

	void addSuspension(PlotSuspension suspension);

	void addTicketData(TicketData ticketData);

	boolean updateTicketData(TicketData ticketData);

	void addComment(TicketData ticket, String comment, String source);

	List<TicketComment> getComments(String plotMessage, UUID player);
}
