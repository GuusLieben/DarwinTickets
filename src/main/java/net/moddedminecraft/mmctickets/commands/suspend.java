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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        final Optional<String> time = args.getOne("time");
        if(!time.isPresent()) {
            player.sendMessage(Text.of(TextColors.RED, "A time must be specified"));
            return CommandResult.empty();
        }

        Optional<Long> timeFromPattern = this.getSecondsFromPattern(time.get());
        if(!timeFromPattern.isPresent()) {
            player.sendMessage(Text.of(TextColors.RED, "Cannot read given time"));
            return CommandResult.empty();
        }

        long suspensionTime = timeFromPattern.get();

        final Collection<PlotSuspension> suspensions = new ArrayList<>(this.plugin.getDataStore().getSuspensionsData());
        int id = suspensions.size() + 1;
        PlotSuspension suspension = new PlotSuspension(id, plot.getId().x, plot.getId().y, player.getWorld().getUniqueId(), System.currentTimeMillis() + (suspensionTime * 1000));
        this.plugin.getDataStore().addSuspension(suspension);

        player.sendMessage(Text.of(TextColors.YELLOW, "This plot has been suspended from submitting for the next " + suspensionTime + " seconds"));
        DiscordUtil.sendSuspension(player.getName(), suspension);

        return CommandResult.success();
    }

    private Optional<Long> getSecondsFromPattern(String input) {
        Pattern pattern = Pattern.compile(TimeUnit.getPattern());
        Matcher matcher = pattern.matcher(input.toLowerCase());

        boolean isValidFormat = matcher.find();
        long seconds = 0;

        if (isValidFormat) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String match = matcher.group(i);

                if (null != match) {
                    //The number is everything except the last character, which is the unit.
                    int num = Integer.parseInt(match.substring(0, match.length() - 1));
                    TimeUnit unit = TimeUnit.unitFromIndex(i - 1);
                    //Apply the multiplier to the unit, to convert it to seconds.
                    seconds += num * unit.getToSecondsMultiplier();
                }
            }
        } else return Optional.empty();

        return Optional.of(seconds);
    }


    private enum TimeUnit {
        DAYS("d", 24 * 60 * 60),
        HOURS("h", 60 * 60),
        MINUTES("m", 60),
        SECONDS("s", 1);

        String character;
        int toSecondsMultiplier;

        TimeUnit(String character, int toSecondsMultiplier) {
            this.character = character;
            this.toSecondsMultiplier= toSecondsMultiplier;
        }

        public int getToSecondsMultiplier() {
            return this.toSecondsMultiplier;
        }

        public String getCharacter() {
            return this.character;
        }

        public static TimeUnit unitFromIndex(int index) {
            return TimeUnit.values()[index];
        }

        public static String getPattern() {
            StringBuilder pattern = new StringBuilder();
            for (TimeUnit unit : TimeUnit.values()) {
                pattern.append(String.format("(\\d*%s)?", unit.getCharacter()));
            }

            return pattern.toString();
        }
    }

}
