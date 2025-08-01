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

import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @deprecated For removal, use {@link H2DataStore} instead.
 */
@Deprecated
public final class MYSQLDataStore implements IDataStore {

    private final Main plugin;
    private final Optional<HikariDataSource> dataSource;

    public MYSQLDataStore(Main plugin) {
        this.plugin = plugin;
        this.dataSource = getDataSource();
    }

    @Override
    public boolean load() {
        if (!dataSource.isPresent()) {
            plugin.getLogger().error("Selected datastore: 'MySQL' is not avaiable please select another datastore.");
            return false;
        }
        try (Connection connection = getConnection()) {
            connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS " + Config.mysqlPrefix + "tickets ("
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
                    + " server VARCHAR(100) NOT NULL"
                    + ");");

            connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS " + Config.mysqlPrefix + "playerdata ("
                    + "uuid VARCHAR(36) NOT NULL PRIMARY KEY, "
                    + "playername VARCHAR(36) NOT NULL, "
                    + "banned INTEGER NOT NULL"
                    + ");");

            getConnection().commit();
        }
        catch (SQLException ex) {
            plugin.getLogger().error("MySQL: Unable to create tables", ex);
            return false;
        }
        return true;
    }

    @Override
    public List<TicketData> getTicketData() {
        List<TicketData> ticketList = new ArrayList<>();

        try (Connection connection = getConnection()) {
            ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM " + Config.mysqlPrefix + "tickets");
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
                ticketList.add(ticketData);
            }
            return ticketList;
        }
        catch (SQLException ex) {
            plugin.getLogger().info("MySQL: Couldn't read ticketdata from MySQL database.", ex);
            return new ArrayList<>();
        }
    }

    @Override
    public Collection<PlotSuspension> getSuspensions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<PlotSuspension> getSuspension(Plot plot) {
        return Optional.empty();
    }

    @Override
    public @Nullable List<PlotSuspension> getSuspensionsData() {
        return null;
    }

    @Override
    public List<PlayerData> getPlayerData() {
        List<PlayerData> playerList = new ArrayList<>();

        try (Connection connection = getConnection()) {
            ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM " + Config.mysqlPrefix + "playerdata");
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
            plugin.getLogger().info("MySQL: Couldn't read playerdata from MySQL database.", ex);
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
        List<TicketData> ticketList = getTicketData();
        if (ticketList == null || ticketList.isEmpty()) {
            return Optional.empty();
        }
        for (TicketData ticket : ticketList) {
            if (ticket.getTicketID() == ticketID) {
                return Optional.of(ticket);
            }
        }
        return Optional.empty();
    }

    @Override
    public void addSuspension(PlotSuspension suspension) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addTicketData(TicketData ticketData) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO " + Config.mysqlPrefix + "tickets VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
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
            statement.executeUpdate();
        }
        catch (SQLException ex) {
            plugin.getLogger().error("MySQL: Error adding ticketdata", ex);
        }
    }

    @Override
    public boolean updateTicketData(TicketData ticketData) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("REPLACE INTO " + Config.mysqlPrefix + "tickets VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
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
            return statement.executeUpdate() > 0;
        }
        catch (SQLException ex) {
            plugin.getLogger().error("MySQL: Error updating ticketdata", ex);
        }
        return false;
    }

    @Override
    public void addComment(TicketData ticket, String comment, String source) {
    }

    @Override
    public @Nullable List<TicketComment> getComments(String plotMessage, UUID player) {
        return null;
    }

    public boolean hasColumn(String tableName, String columnName) {
        try (Connection connection = getConnection()) {
            DatabaseMetaData md = connection.getMetaData();
            ResultSet rs = md.getColumns(null, null, tableName, columnName);
            return rs.next();
        }
        catch (SQLException ex) {
            plugin.getLogger().error("MySQL: Error checking if column exists.", ex);
        }
        return false;
    }

    public Optional<HikariDataSource> getDataSource() {
        try {
            HikariDataSource ds = new HikariDataSource();
            ds.setDriverClassName("org.mariadb.jdbc.Driver");
            ds.setJdbcUrl("jdbc:mariadb://"
                    + Config.mysqlHost
                    + ":" + Config.mysqlPort
                    + "/" + Config.mysqlDatabase);
            ds.addDataSourceProperty("user", Config.mysqlUser);
            ds.addDataSourceProperty("password", Config.mysqlPass);
            ds.setConnectionTimeout(1000);
            ds.setLoginTimeout(5);
            ds.setAutoCommit(true);
            return Optional.ofNullable(ds);
        }
        catch (SQLException ex) {
            plugin.getLogger().error("MySQL: Failed to get datastore.", ex);
            return Optional.empty();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.get().getConnection();
    }

}

