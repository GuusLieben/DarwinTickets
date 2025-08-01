package net.moddedminecraft.mmctickets.database;

import com.intellectualcrafters.plot.object.Plot;
import com.zaxxer.hikari.HikariDataSource;

import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.config.Config;
import net.moddedminecraft.mmctickets.data.PlayerData;
import net.moddedminecraft.mmctickets.data.PlotSuspension;
import net.moddedminecraft.mmctickets.data.TicketComment;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.data.TicketStatus;

import org.jetbrains.annotations.NonNls;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.World;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

public final class H2DataStore implements IDataStore {

    private final Main plugin;
    private final Optional<HikariDataSource> dataSource;
    private final Map<Integer, TicketData> tickets = new ConcurrentHashMap<>();
    private final Map<Integer, List<TicketComment>> comments = new ConcurrentHashMap<>();
    private final Map<Integer, PlotSuspension> suspensions = new ConcurrentHashMap<>();

    public H2DataStore(Main plugin) {
        this.plugin = plugin;
        this.dataSource = getDataSource();
    }

	@Override
    public boolean load() {
        if (!dataSource.isPresent()) {
            plugin.getLogger().error("Selected datastore: 'H2' is not avaiable please select another datastore.");
            return false;
        }
        try (Connection connection = getConnection()) {
            connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS " + Config.h2Prefix + "tickets ("
                    + " ticketid INTEGER NOT NULL PRIMARY KEY,"
                    + " playeruuid VARCHAR(60) NOT NULL,"
                    + " staffuuid VARCHAR(60) NOT NULL,"
                    + " comment VARCHAR(700) NOT NULL,"
                    + " timestamp BIGINT NOT NULL,"
                    + " world VARCHAR(100) NOT NULL,"
                    + " coordx INTEGER NOT NULL,"
                    + " coordy INTEGER NOT NULL,"
                    + " coordz INTEGER NOT NULL,"
                    + " yaw DOUBLE NOT NULL,"
                    + " pitch DOUBLE NOT NULL,"
                    + " message VARCHAR(700) NOT NULL,"
                    + " status VARCHAR(20) NOT NULL,"
                    + " notified INTEGER NOT NULL,"
                    + " server VARCHAR(100) NOT NULL,"
                    + " discord VARCHAR(100) NOT NULL,"
                    + " additional_staff VARCHAR(MAX)"
                    + ");");

            connection.createStatement().executeUpdate("ALTER TABLE " + Config.h2Prefix + "tickets ADD COLUMN IF NOT EXISTS additional_staff VARCHAR(MAX)");

            connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS " + Config.h2Prefix + "playerdata ("
                    + "uuid VARCHAR(36) NOT NULL PRIMARY KEY, "
                    + "playername VARCHAR(36) NOT NULL, "
                    + "banned INTEGER NOT NULL"
                    + ");");

            // comment, source, posted, plot, player
            connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS " + Config.h2Prefix + "comments ("
                    + " comment VARCHAR(MAX) NOT NULL,"
                    + " source VARCHAR(100) NOT NULL,"
                    + " posted TIMESTAMP NOT NULL,"
                    + " plot VARCHAR(100) NOT NULL,"
                    + " player VARCHAR(MAX) NOT NULL,"
                    + " ticketid INTEGER NOT NULL"
                    + ");"
            );

            // plot suspensions
            connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS " + Config.h2Prefix + "suspensions ("
                    + " suspensionId INTEGER NOT NULL PRIMARY KEY,"
                    + " plotX INTEGER NOT NULL,"
                    + " plotY INTEGER NOT NULL,"
                    + " worldId VARCHAR(36) NOT NULL,"
                    + " suspendedTo BIGINT NOT NULL"
                    + ");"
            );

            getConnection().commit();
        }
        catch (SQLException ex) {
            plugin.getLogger().error("Unable to create tables", ex);
            return false;
        }
        return true;
    }

    @Override
    public void addSuspension(PlotSuspension suspension) {
        plugin.getLogger().error("NEW ID : " + suspension.suspensionId);
        this.suspensions.put(suspension.suspensionId, suspension);
        try (Connection connection = this.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO " + Config.h2Prefix + "suspensions VALUES (?, ?, ?, ?, ?);");
            statement.setLong(1, suspension.suspensionId);
            statement.setInt(2, suspension.plotX);
            statement.setInt(3, suspension.plotY);
            statement.setString(4, suspension.plotWorldId.toString());
            statement.setLong(5, suspension.suspendedTo);
			statement.executeUpdate();
		}
        catch (SQLException ex) {
            this.plugin.getLogger().error("H2: Error adding suspension", ex);
        }
	}

	@Override
    public List<TicketData> getTicketData() {
        if (!tickets.isEmpty()) return new ArrayList<>(tickets.values());

        List<TicketData> ticketList = new ArrayList<>();

        try (Connection connection = getConnection()) {
            ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM " + Config.h2Prefix + "tickets");
            while (rs.next()) {
                TicketData ticketData = new TicketData(
                        rs.getInt("ticketid"),
                        rs.getString("playeruuid"),
                        rs.getString("staffuuid"),
                        rs.getString("comment"),
                        rs.getInt("timestamp"),
                        rs.getString("world"),
                        rs.getInt("coordx"),
                        rs.getInt("coordy"),
                        rs.getInt("coordz"),
                        rs.getDouble("yaw"),
                        rs.getDouble("pitch"),
                        rs.getString("message"),
                        TicketStatus.fromString(rs.getString("status")),
                        rs.getInt("notified"),
                        rs.getString("server"),
                        rs.getString("additional_staff")
                );
                ticketData.setDiscordMessage(rs.getString("discord"));
                ticketList.add(ticketData);
            }
            ticketList.forEach(ticket -> this.tickets.put(ticket.getTicketID(), ticket));
            return ticketList;
        }
        catch (SQLException ex) {
            plugin.getLogger().info("H2: Couldn't read ticketdata from H2 database.", ex);
            return new ArrayList<>();
        }
    }

    @Override
    public Collection<PlotSuspension> getSuspensions() {
        return this.suspensions.values();
    }

    @Override
    @Nullable
    public Optional<PlotSuspension> getSuspension(Plot plot) {
        for (PlotSuspension suspensionsDatum : getSuspensionsData()) {
            Optional<World> world = Sponge.getServer().getWorld(suspensionsDatum.plotWorldId);
            if (!world.isPresent())
                continue;

            @NonNls String worldName = world.get().getName();
            if (worldName.equals(plot.getWorldName())
                    && suspensionsDatum.plotX == plot.getId().x
                    && suspensionsDatum.plotY == plot.getId().y
                    && suspensionsDatum.suspendedTo > System.currentTimeMillis())
                return Optional.of(suspensionsDatum);
        }
        return Optional.empty();
    }

    @Override
    public List<PlotSuspension> getSuspensionsData() {
        if (!this.suspensions.isEmpty()) return new ArrayList<>(this.suspensions.values());

        List<PlotSuspension> suspensionList = new ArrayList<>();

        try (Connection connection = this.getConnection()) {
            ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM " + Config.h2Prefix + "suspensions");
            while (rs.next()) {
                PlotSuspension suspensionData = new PlotSuspension(
                        rs.getInt("suspensionId"),
                        rs.getInt("plotX"),
                        rs.getInt("plotY"),
                        UUID.fromString(rs.getString("worldId")),
                        rs.getLong("suspendedTo")
                );
                suspensionList.add(suspensionData);
            }
            suspensionList.forEach(s -> this.suspensions.put(s.suspensionId, s));
            return suspensionList;
        }
        catch (SQLException ex) {
            this.plugin.getLogger().info("H2: Couldn't read suspensionData from H2 database.", ex);
            return new ArrayList<>();
        }
    }

    @Override
    public List<PlayerData> getPlayerData() {
        List<PlayerData> playerList = new ArrayList<>();

        try (Connection connection = getConnection()) {
            ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM " + Config.h2Prefix + "playerdata");
            while (rs.next()) {
                PlayerData playerData = new PlayerData(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("playername"),
                        rs.getInt("banned")
                );
                playerList.add(playerData);
            }
            return playerList;
        }
        catch (SQLException ex) {
            plugin.getLogger().info("H2: Couldn't read playerdata from H2 database.", ex);
            return new ArrayList<>();
        }
    }

    @Override
    public ArrayList<UUID> getNotifications() {
        ArrayList<UUID> notifications = new ArrayList<>();
        List<TicketData> ticketData = getTicketData();
        for (TicketData ticket : ticketData) {
            if (ticket.getNotified() == 0 && ticket.getStatus() == TicketStatus.CLOSED) {
                notifications.add(ticket.getPlayerUUID());
            }
        }
        return notifications;
    }

    @Override
    public Optional<TicketData> getTicket(int ticketID) {
        try (Connection connection = getConnection()) {
            ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM " + Config.h2Prefix + "tickets");
            rs.next();
            TicketData ticketData = new TicketData(
                    rs.getInt("ticketid"),
                    rs.getString("playeruuid"),
                    rs.getString("staffuuid"),
                    rs.getString("comment"),
                    rs.getInt("timestamp"),
                    rs.getString("world"),
                    rs.getInt("coordx"),
                    rs.getInt("coordy"),
                    rs.getInt("coordz"),
                    rs.getDouble("yaw"),
                    rs.getDouble("pitch"),
                    rs.getString("message"),
                    TicketStatus.fromString(rs.getString("status")),
                    rs.getInt("notified"),
                    rs.getString("server"),
                    rs.getString("additional_staff")
            );
            ticketData.setDiscordMessage(rs.getString("discord"));
            return Optional.of(ticketData);
        }
        catch (SQLException ex) {
            plugin.getLogger().info("H2: Couldn't read ticketdata from H2 database.", ex);
            return Optional.empty();
        }
    }

    @Override
    public void addTicketData(TicketData ticketData) {
        this.tickets.put(ticketData.getTicketID(), ticketData);
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO " + Config.h2Prefix + "tickets VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
            statement.setInt(1, ticketData.getTicketID());
            statement.setString(2, ticketData.getPlayerUUID().toString());
            statement.setString(3, ticketData.getStaffUUID().toString());
            statement.setString(4, ticketData.getComment());
            statement.setLong(5, ticketData.getTimestamp());
            statement.setString(6, ticketData.getWorld());
            statement.setInt(7, ticketData.getX());
            statement.setInt(8, ticketData.getY());
            statement.setInt(9, ticketData.getZ());
            statement.setDouble(10, ticketData.getYaw());
            statement.setDouble(11, ticketData.getPitch());
            statement.setString(12, ticketData.getMessage());
            statement.setString(13, ticketData.getStatus().toString());
            statement.setInt(14, ticketData.getNotified());
            statement.setString(15, ticketData.getServer());
            statement.setString(16, ticketData.getDiscordMessage());
            statement.setString(17, ticketData.getAdditionalStaff());
			statement.executeUpdate();
		}
        catch (SQLException ex) {
            plugin.getLogger().error("H2: Error adding ticketdata", ex);
        }
	}

	@Override
    public boolean updateTicketData(TicketData ticketData) {
        this.tickets.put(ticketData.getTicketID(), ticketData);
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("MERGE INTO " + Config.h2Prefix + "tickets VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
            statement.setInt(1, ticketData.getTicketID());
            statement.setString(2, ticketData.getPlayerUUID().toString());
            statement.setString(3, ticketData.getStaffUUID().toString());
            statement.setString(4, ticketData.getComment());
            statement.setLong(5, ticketData.getTimestamp());
            statement.setString(6, ticketData.getWorld());
            statement.setInt(7, ticketData.getX());
            statement.setInt(8, ticketData.getY());
            statement.setInt(9, ticketData.getZ());
            statement.setDouble(10, ticketData.getYaw());
            statement.setDouble(11, ticketData.getPitch());
            statement.setString(12, ticketData.getMessage());
            statement.setString(13, ticketData.getStatus().toString());
            statement.setInt(14, ticketData.getNotified());
            statement.setString(15, ticketData.getServer());
            statement.setString(16, ticketData.getDiscordMessage());
            statement.setString(17, ticketData.getAdditionalStaff());
            return statement.executeUpdate() > 0;
        }
        catch (SQLException ex) {
            plugin.getLogger().error("H2: Error updating ticketdata", ex);
        }
        return false;
    }

	@Override
    public void addComment(TicketData ticket, String comment, String source) {
        if (!this.comments.containsKey(ticket.getTicketID())) {
            this.comments.put(ticket.getTicketID(), new CopyOnWriteArrayList<>());
        }
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO " + Config.h2Prefix + "comments VALUES (?, ?, ?, ?, ?, ?);");
            statement.setString(1, comment);
            statement.setString(2, source);
            statement.setTimestamp(3, Timestamp.from(Instant.now()));
            statement.setString(4, ticket.getMessage());
            statement.setString(5, ticket.getPlayerUUID().toString());
            statement.setInt(6, ticket.getTicketID());
			statement.executeUpdate();
		}
        catch (SQLException ex) {
            plugin.getLogger().error("H2: Error adding ticket comment", ex);
        }
	}

    @Override
    public List<TicketComment> getComments(String plotMessage, UUID player) {
        List<TicketComment> comments = new ArrayList<>();
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT comment, source, posted, ticketid FROM " + Config.h2Prefix + "comments WHERE plot = ? AND player = ?");
            statement.setString(1, plotMessage);
            statement.setString(2, player.toString());
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                TicketComment comment = new TicketComment(
                        rs.getString("source"),
                        rs.getTimestamp("posted"),
                        rs.getString("comment"),
                        rs.getInt("ticketid")
                );
                comments.add(comment);
            }
            return comments;
        }
        catch (SQLException ex) {
            plugin.getLogger().warn("H2: Couldn't read comments from H@ database.", ex);
            return new ArrayList<>();
        }
    }

	public Optional<HikariDataSource> getDataSource() {
        try {
            HikariDataSource ds = new HikariDataSource();
            ds.setDriverClassName("org.h2.Driver");
            ds.setJdbcUrl("jdbc:h2://" + new File(plugin.ConfigDir.toFile(), Config.databaseFile).getAbsolutePath());
            ds.setConnectionTimeout(1000);
            ds.setLoginTimeout(5);
            ds.setAutoCommit(true);
            return Optional.ofNullable(ds);
        }
        catch (SQLException ex) {
            plugin.getLogger().error("H2: Failed to get datastore.", ex);
            return Optional.empty();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.get().getConnection();
    }

}
