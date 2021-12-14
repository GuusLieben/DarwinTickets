package net.moddedminecraft.mmctickets.data;


import java.awt.Color;

public enum TicketStatus {
    OPEN("Open", Color.YELLOW),
    CLAIMED("Claimed", Color.GREEN),
    HELD("Held", Color.PINK),
    CLOSED("Legacy Closed", Color.BLACK),
    REJECTED("Rejected", Color.RED),
    APPROVED("Approved", Color.GREEN),
    SUSPENDED("SUSPENDED", Color.ORANGE);

    private final String status;
    private final Color color;

    TicketStatus(String stat, Color color) {
        this.status = stat;
        this.color = color;
    }

    @Override
    public String toString() {
        return status;
    }

    public Color getAssociatedColor() {
        return this.color;
    }

    public static TicketStatus fromString(String value) {
        if (CLOSED.status.equalsIgnoreCase(value)) return CLOSED;
        else return TicketStatus.valueOf(value.toUpperCase());
    }
}
