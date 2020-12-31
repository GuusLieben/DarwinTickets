package net.moddedminecraft.mmctickets.commands;

import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.data.PlotSuspension;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.util.DiscordUtil;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class suspend implements CommandExecutor {
    private final Main plugin;

    public suspend ( Main plugin ) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute ( CommandSource src, CommandContext args ) throws CommandException {
        if(!(src instanceof Player)) {
            src.sendMessage(Text.of(TextColors.RED, "You must be a player to execute this command"));
            return CommandResult.empty();
        }

        // Get player and its Intellectual Location
        Player player = (Player) src;
        Location playerLocation = new Location(
                player.getWorld().getName(),
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ()
        );

        // Get the plot that the player is on
        Plot plot = Plot.getPlot(playerLocation);
        if(null == plot) {
            player.sendMessage(Text.of(TextColors.RED, "You must be on a plot to execute this command"));
            return CommandResult.empty();
        }

        // Get the suspension time
        final Optional<Integer> time = args.getOne("time");
        if(!time.isPresent()) {
            player.sendMessage(Text.of(TextColors.RED, "A time must be specified"));
            return CommandResult.empty();
        }

        long suspensionTime = time.get();

        final Collection<PlotSuspension> suspensions = new ArrayList<>(this.plugin.getDataStore().getSuspensionsData());
        int id = suspensions.size() + 1;
        PlotSuspension suspension = new PlotSuspension(id, plot.getId().x, plot.getId().y, player.getWorld().getUniqueId(), System.currentTimeMillis() + (suspensionTime * 1000));
        this.plugin.getDataStore().addSuspension(suspension);

        player.sendMessage(Text.of(TextColors.YELLOW, "This plot has been suspended from submitting for the next " + suspensionTime + " seconds"));
        DiscordUtil.sendSuspension(player.getName(), suspension);

        return CommandResult.success();
    }
}
