package net.moddedminecraft.mmctickets.database;

import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.config.Config;
import net.moddedminecraft.mmctickets.data.PlayerData;
import net.moddedminecraft.mmctickets.data.TicketComment;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.data.ticketStatus;

public final class H2DataStore implements IDataStore {

	private final Main plugin;
	private final Optional<HikariDataSource> dataSource;

	public H2DataStore ( Main plugin ) {
		this.plugin = plugin;
		this.dataSource = getDataSource();
	}

	@Override
	public String getDatabaseName () {
		return "H2";
	}

	@Override
	public boolean load () {
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

			getConnection().commit();
		} catch (SQLException ex) {
			plugin.getLogger().error("Unable to create tables", ex);
			return false;
		}
		return true;
	}

	@Override
	public List<TicketData> getTicketData () {
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
						ticketStatus.valueOf(rs.getString("status").toUpperCase()),
						rs.getInt("notified"),
						rs.getString("server"),
						rs.getString("additional_staff")
				);
				ticketData.setDiscordMessage(rs.getString("discord"));
				ticketList.add(ticketData);
			}
			return ticketList;
		} catch (SQLException ex) {
			plugin.getLogger().info("H2: Couldn't read ticketdata from H2 database.", ex);
			return new ArrayList<>();
		}
	}

	@Override
	public List<PlayerData> getPlayerData () {
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
		} catch (SQLException ex) {
			plugin.getLogger().info("H2: Couldn't read playerdata from H2 database.", ex);
			return new ArrayList<>();
		}
	}

	@Override
	public ArrayList<UUID> getNotifications () {
		ArrayList<UUID> notifications = new ArrayList<>();
		List<TicketData> ticketData = getTicketData();
		for (TicketData ticket : ticketData) {
			if (ticket.getNotified() == 0 && ticket.getStatus() == ticketStatus.CLOSED) {
				notifications.add(ticket.getPlayerUUID());
			}
		}
		return notifications;
	}

	@Override
	public Optional<TicketData> getTicket ( int ticketID ) {
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
	public boolean addTicketData ( TicketData ticketData ) {
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
			return statement.executeUpdate() > 0;
		} catch (SQLException ex) {
			plugin.getLogger().error("H2: Error adding ticketdata", ex);
		}
		return false;
	}

	@Override
	public boolean addPlayerData ( PlayerData playerData ) {
		try (Connection connection = getConnection()) {
			PreparedStatement statement = connection.prepareStatement("INSERT INTO " + Config.h2Prefix + "playerdata VALUES (?, ?, ?);");
			statement.setString(1, playerData.getPlayerUUID().toString());
			statement.setString(2, playerData.getPlayerName());
			statement.setInt(3, playerData.getBannedStatus());
			return statement.executeUpdate() > 0;
		} catch (SQLException ex) {
			plugin.getLogger().error("H2: Error adding playerdata", ex);
		}
		return false;
	}

	@Override
	public boolean updateTicketData ( TicketData ticketData ) {
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
		} catch (SQLException ex) {
			plugin.getLogger().error("H2: Error updating ticketdata", ex);
		}
		return false;
	}

	@Override
	public boolean updatePlayerData ( PlayerData playerData ) {
		try (Connection connection = getConnection()) {
			PreparedStatement statement = connection.prepareStatement("MERGE INTO " + Config.h2Prefix + "playerdata VALUES (?, ?, ?);");
			statement.setString(1, playerData.getPlayerUUID().toString());
			statement.setString(2, playerData.getPlayerName());
			statement.setInt(3, playerData.getBannedStatus());
			return statement.executeUpdate() > 0;
		} catch (SQLException ex) {
			plugin.getLogger().error("H2: Error updating playerdata", ex);
		}
		return false;
	}

	@Override
	public boolean addComment(TicketData ticket, String comment, String source) {
		try (Connection connection = getConnection()) {
			PreparedStatement statement = connection.prepareStatement("INSERT INTO " + Config.h2Prefix + "comments VALUES (?, ?, ?, ?, ?, ?);");
			statement.setString(1, comment);
			statement.setString(2, source);
			statement.setTimestamp(3, Timestamp.from(Instant.now()));
			statement.setString(4, ticket.getMessage());
			statement.setString(5, ticket.getPlayerUUID().toString());
			statement.setInt(6, ticket.getTicketID());
			return statement.executeUpdate() > 0;
		} catch (SQLException ex) {
			plugin.getLogger().error("H2: Error adding ticket comment", ex);
		}
		return false;
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
		} catch (SQLException ex) {
			plugin.getLogger().warn("H2: Couldn't read comments from H@ database.", ex);
			return new ArrayList<>();
		}
	}

	public boolean hasColumn ( String tableName, String columnName ) {
		try (Connection connection = getConnection()) {
			DatabaseMetaData md = connection.getMetaData();
			ResultSet rs = md.getColumns(null, null, tableName, columnName);
			return rs.next();
		} catch (SQLException ex) {
			plugin.getLogger().error("H2: Error checking if column exists.", ex);
		}
		return false;
	}

	public Optional<HikariDataSource> getDataSource () {
		try {
			HikariDataSource ds = new HikariDataSource();
			ds.setDriverClassName("org.h2.Driver");
			ds.setJdbcUrl("jdbc:h2://" + new File(plugin.ConfigDir.toFile(), Config.databaseFile).getAbsolutePath());
			ds.setConnectionTimeout(1000);
			ds.setLoginTimeout(5);
			ds.setAutoCommit(true);
			return Optional.ofNullable(ds);
		} catch (SQLException ex) {
			plugin.getLogger().error("H2: Failed to get datastore.", ex);
			return Optional.empty();
		}
	}

	public Connection getConnection () throws SQLException {
		return dataSource.get().getConnection();
	}

}
