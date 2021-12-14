package net.moddedminecraft.mmctickets.util;

import java.util.UUID;

public class PlotSuspensionUtil {

    public int suspensionId;
    public int plotX;
    public int plotY;
    public UUID plotWorldId;
    public long suspendedTo;

    public PlotSuspensionUtil(int suspensionId, int x, int y, UUID worldId, long suspendedTo) {
        this.suspensionId = suspensionId;
        this.plotX = x;
        this.plotY = y;
        this.plotWorldId = worldId;
        this.suspendedTo = suspendedTo;
    }
}
