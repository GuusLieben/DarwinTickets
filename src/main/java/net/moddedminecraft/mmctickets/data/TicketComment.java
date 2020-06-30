package net.moddedminecraft.mmctickets.data;

import java.sql.Timestamp;

public class TicketComment {

    private final String source;
    private final Timestamp timestamp;
    private final String comment;
    private final int ticketId;

    public TicketComment(String source, Timestamp timestamp, String comment, int ticketId) {
        this.source = source;
        this.timestamp = timestamp;
        this.comment = comment;
        this.ticketId = ticketId;
    }

    public int getTicketId() {
        return ticketId;
    }

    public String getSource() {
        return source;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getComment() {
        return comment;
    }
}
