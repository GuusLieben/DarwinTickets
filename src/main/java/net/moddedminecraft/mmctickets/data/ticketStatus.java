package net.moddedminecraft.mmctickets.data;


import net.moddedminecraft.mmctickets.util.DiscordUtil.DiscordTicketStatus;

import java.awt.Color;

public enum ticketStatus {
	OPEN("Open", Color.YELLOW),
	CLAIMED("Claimed", Color.GREEN),
	HELD("Held", Color.PINK),
	CLOSED("Legacy Closed", Color.BLACK),
	REJECTED("Rejected", Color.RED),
	APPROVED("Approved", Color.GREEN),
	SUSPENDED("SUSPENDED", Color.ORANGE);

	private String status;
	private Color color;

	ticketStatus(String stat, Color color) {
		this.status = stat;
		this.color = color;
	}

	@Override
	public String toString () {
		return status;
	}

	public Color getAssociatedColor() {
		return this.color;
	}

	public static ticketStatus fromString(String value) {
		if ("LEGACY CLOSED".equals(value.toUpperCase())) return CLOSED;
		else return ticketStatus.valueOf(value.toUpperCase());
	}
}
