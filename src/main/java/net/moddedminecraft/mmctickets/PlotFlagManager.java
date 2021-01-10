package net.moddedminecraft.mmctickets;

import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.flag.Flags;

public class PlotFlagManager {

    private final Main plugin;
    public final Flag<Boolean> SUSPENSION_FLAG = new SuspensionNotificationFlag("suspension-notifications");

    public PlotFlagManager(Main plugin) {
        this.plugin = plugin;
    }

    public void init() {
        Flags.registerFlag(this.SUSPENSION_FLAG);
    }

}
