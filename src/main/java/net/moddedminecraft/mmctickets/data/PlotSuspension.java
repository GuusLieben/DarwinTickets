package net.moddedminecraft.mmctickets.data;

import com.google.common.reflect.TypeToken;

import net.moddedminecraft.mmctickets.util.PlotSuspensionUtil;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class PlotSuspension extends PlotSuspensionUtil implements Comparable<PlotSuspension> {

    public PlotSuspension(int suspensionId, int x, int y, UUID worldId, long suspendedTo) {
        super(suspensionId, x, y, worldId, suspendedTo);
    }

    @Override
    public int compareTo(@NotNull PlotSuspension o) {
        return this.suspendedTo > o.suspendedTo ? 1 : 0;
    }

    public static class PlotSuspensionSerializer implements TypeSerializer<PlotSuspension> {

        @Override
        public PlotSuspension deserialize(@NotNull TypeToken<?> token, ConfigurationNode node) {
            return new PlotSuspension(
                    node.getNode("suspensionId").getInt(),
                    node.getNode("plotX").getInt(),
                    node.getNode("plotY").getInt(),
                    UUID.fromString(node.getNode("worldId").getString()),
                    node.getNode("suspendedTo").getLong());
        }

        @Override
        public void serialize(@NotNull TypeToken<?> token, PlotSuspension plot, ConfigurationNode node) {
            node.getNode("suspensionId").setValue(plot.suspensionId);
            node.getNode("plotX").setValue(plot.plotX);
            node.getNode("plotY").setValue(plot.plotY);
            node.getNode("worldId").setValue(plot.plotWorldId.toString());
            node.getNode("suspendedTo").setValue(plot.suspendedTo);
        }
    }


}


