package net.moddedminecraft.mmctickets.util;

import net.moddedminecraft.mmctickets.data.ticketStatus;

import java.util.UUID;

public class TicketDataUtil {

  protected String playerUUID, world, staffUUID, comment, message, server;
  protected int ticketID, x, y, z, notified;
  protected Double yaw, pitch;
  protected long timestamp;
  protected ticketStatus status;
  protected String discordMessage;
  private String rank;

  public String getRank() {
    return rank;
  }

  public void setRank(String rank) {
    this.rank = rank;
  }

  public TicketDataUtil(
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
      String server) {
    this.ticketID = ticketID;
    this.playerUUID = playerUUID;
    this.staffUUID = staffUUID;
    this.comment = comment;
    this.timestamp = timestamp;
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.message = message;
    this.status = status;
    this.notified = notified;
    this.server = server;
  }

  public String getDiscordMessage() {
    return discordMessage;
  }

  public void setDiscordMessage(String discordMessage) {
    this.discordMessage = discordMessage;
  }

  public int getTicketID() {
    return ticketID;
  }

  public UUID getPlayerUUID() {
    return UUID.fromString(playerUUID);
  }

  public UUID getStaffUUID() {
    return UUID.fromString(staffUUID);
  }

  public String getOldPlayer() {
    return playerUUID;
  }

  public String getOldStaffname() {
    return staffUUID;
  }

  public String getComment() {
    return comment.replaceAll("(\\[)(.*)(\\])", "$2");
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getWorld() {
    return world;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getZ() {
    return z;
  }

  public Double getYaw() {
    return yaw;
  }

  public Double getPitch() {
    return pitch;
  }

  public String getMessage() {
    return message.replaceAll("(\\[)(.*)(\\])", "$2");
  }

  public ticketStatus getStatus() {
    return status;
  }

  public int getNotified() {
    return notified;
  }

  public void setStatus(ticketStatus status) {
    this.status = status;
  }

  public void setNotified(int notified) {
    this.notified = notified;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public void setStaffUUID(String uuid) {
    this.staffUUID = uuid;
  }

  public void setPlayerUUID(UUID uuid) {
    this.playerUUID = String.valueOf(uuid);
  }

  public String getServer() {
    return server;
  }

  public void setServer(String server) {
    this.server = server;
  }
}
