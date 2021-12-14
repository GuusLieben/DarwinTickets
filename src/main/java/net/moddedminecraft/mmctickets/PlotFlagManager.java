package net.moddedminecraft.mmctickets;

import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.flag.Flags;

public class PlotFlagManager {

    public final Flag<Boolean> SUSPENSION_FLAG = new SuspensionNotificationFlag("suspension-notifications");

    public void init() {
        Flags.registerFlag(this.SUSPENSION_FLAG);
    }

}
