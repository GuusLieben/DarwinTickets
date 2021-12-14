package net.moddedminecraft.mmctickets.util;

import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.data.TicketStatus;

import java.util.UUID;

public class TicketDataUtil {

    protected String playerUUID, world, staffUUID, comment, message, server;
    protected int ticketID, x, y, z, notified;
    protected Double yaw, pitch;
    protected long timestamp;
    protected TicketStatus status;
    protected String discordMessage;
    protected String additionalReviewers;

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
            TicketStatus status,
            int notified,
            String server,
            String additionalReviewers) {
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
        if (additionalReviewers == null || "".equals(additionalReviewers)) {
            if (staffUUID != null) {
                String firstReviewer = CommonUtil.getPlayerNameFromData(Main.INSTANCE, UUID.fromString(staffUUID));
                if (!(firstReviewer.equalsIgnoreCase("Console") || firstReviewer.equalsIgnoreCase(CommonUtil.getPlayerNameFromData(Main.INSTANCE, UUID.fromString(playerUUID)))))
                    this.additionalReviewers = firstReviewer;
            }
        }
        else this.additionalReviewers = additionalReviewers;
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
        if (staffUUID == null) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        else {
            return UUID.fromString(staffUUID);
        }
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

    public TicketStatus getStatus() {
        return status;
    }

    public int getNotified() {
        return notified;
    }

    public void setStatus(TicketStatus status) {
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
        if (uuid != null) {
            String name = CommonUtil.getPlayerNameFromData(Main.INSTANCE, UUID.fromString(uuid));
            if (this.additionalReviewers == null)
                this.additionalReviewers = name;

            if (!this.additionalReviewers.contains(name))
                this.additionalReviewers += "," + name;
        }
    }

    public String getServer() {
        return server;
    }

    public void addAdditionalReviewer(String reviewer) {
        if (this.additionalReviewers == null || "".equals(this.additionalReviewers)) {
            this.additionalReviewers = reviewer;
        }
        else {
            this.additionalReviewers += "," + reviewer;
        }
    }

    public String getAdditionalStaff() {
        if (additionalReviewers == null || "".equals(additionalReviewers)) {
            if (!"00000000-0000-0000-0000-000000000000".equals(staffUUID) && staffUUID != null)
                return CommonUtil.getPlayerNameFromData(Main.INSTANCE, UUID.fromString(staffUUID));
            else return "Unknown";
        }
        return additionalReviewers;
    }

    public String[] getAdditionalReviewers() {
        if (additionalReviewers == null || "".equals(additionalReviewers))
            if (!"00000000-0000-0000-0000-000000000000".equals(staffUUID) && staffUUID != null)
                return new String[]{ CommonUtil.getPlayerNameFromData(Main.INSTANCE, UUID.fromString(staffUUID)) };
            else return new String[]{ "Unknown" };

        return additionalReviewers.split(",");
    }

}
