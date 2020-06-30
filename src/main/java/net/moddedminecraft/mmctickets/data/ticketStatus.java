package net.moddedminecraft.mmctickets.data;


import java.awt.Color;

public enum ticketStatus {
	OPEN("Open", Color.YELLOW),
	CLAIMED("Claimed", Color.GREEN),
	HELD("Held", Color.PINK),
	CLOSED("Legacy Closed", Color.BLACK),
	REJECTED("Rejected", Color.RED),
	APPROVED("Approved", Color.GREEN);

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
}
