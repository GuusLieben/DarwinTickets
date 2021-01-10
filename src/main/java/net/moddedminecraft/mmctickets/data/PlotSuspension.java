package net.moddedminecraft.mmctickets.data;

import com.google.common.reflect.TypeToken;
import net.moddedminecraft.mmctickets.util.PlayerDataUtil;
import net.moddedminecraft.mmctickets.util.PlotSuspensionUtil;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class PlotSuspension extends PlotSuspensionUtil implements Comparable<PlotSuspension> {

    public PlotSuspension(int suspensionId, int x, int y, UUID worldId, long suspendedTo) {
        super(suspensionId, x, y, worldId, suspendedTo);
    }

    @Override
    public int compareTo(@NotNull PlotSuspension o) {
        return this.suspendedTo > o.suspendedTo ? 1 : 0;
    }

    public static class PlotSuspensionSerializer implements TypeSerializer<PlotSuspension> {
        @SuppressWarnings ("serial")
        final public static TypeToken<List<PlotSuspension>> token = new TypeToken<List<PlotSuspension>>() {
        };

        @Override
        public PlotSuspension deserialize ( TypeToken<?> token, ConfigurationNode node ) throws ObjectMappingException {
            return new PlotSuspension(
                    node.getNode("suspensionId").getInt(),
                    node.getNode("plotX").getInt(),
                    node.getNode("plotY").getInt(),
                    UUID.fromString(node.getNode("worldId").getString()),
                    node.getNode("suspendedTo").getLong());
        }

        @Override
        public void serialize (TypeToken<?> token, PlotSuspension plot, ConfigurationNode node ) throws ObjectMappingException {
            node.getNode("suspensionId").setValue(plot.suspensionId);
            node.getNode("plotX").setValue(plot.plotX);
            node.getNode("plotY").setValue(plot.plotY);
            node.getNode("worldId").setValue(plot.plotWorldId.toString());
            node.getNode("suspendedTo").setValue(plot.suspendedTo);
        }
    }



}


