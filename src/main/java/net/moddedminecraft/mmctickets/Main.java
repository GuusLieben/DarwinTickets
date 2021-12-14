package net.moddedminecraft.mmctickets;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.magitechserver.magibridge.MagiBridge;

import net.moddedminecraft.mmctickets.commands.AddStaff;
import net.moddedminecraft.mmctickets.commands.Assign;
import net.moddedminecraft.mmctickets.commands.Claim;
import net.moddedminecraft.mmctickets.commands.Close;
import net.moddedminecraft.mmctickets.commands.Comment;
import net.moddedminecraft.mmctickets.commands.Hold;
import net.moddedminecraft.mmctickets.commands.Open;
import net.moddedminecraft.mmctickets.commands.Read;
import net.moddedminecraft.mmctickets.commands.Reject;
import net.moddedminecraft.mmctickets.commands.Reload;
import net.moddedminecraft.mmctickets.commands.Reopen;
import net.moddedminecraft.mmctickets.commands.Staff;
import net.moddedminecraft.mmctickets.commands.subcommands.ReadClosed;
import net.moddedminecraft.mmctickets.commands.subcommands.ReadHeld;
import net.moddedminecraft.mmctickets.commands.subcommands.ReadSelf;
import net.moddedminecraft.mmctickets.commands.Suspend;
import net.moddedminecraft.mmctickets.commands.Teleport;
import net.moddedminecraft.mmctickets.commands.Ticket;
import net.moddedminecraft.mmctickets.commands.Unclaim;
import net.moddedminecraft.mmctickets.config.Config;
import net.moddedminecraft.mmctickets.config.Messages;
import net.moddedminecraft.mmctickets.config.Permissions;
import net.moddedminecraft.mmctickets.data.PlayerData;
import net.moddedminecraft.mmctickets.data.PlayerData.PlayerDataSerializer;
import net.moddedminecraft.mmctickets.data.PlotSuspension;
import net.moddedminecraft.mmctickets.data.PlotSuspension.PlotSuspensionSerializer;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.data.TicketData.TicketSerializer;
import net.moddedminecraft.mmctickets.data.TicketStatus;
import net.moddedminecraft.mmctickets.database.DataStoreManager;
import net.moddedminecraft.mmctickets.database.IDataStore;
import net.moddedminecraft.mmctickets.util.CommonUtil;
import net.moddedminecraft.mmctickets.util.DiscordUtil;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameAboutToStartServerEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;

@Plugin(
        id = "mmctickets",
        name = "MMCTickets",
        version = "2.0.7",
        description = "A real time ticket system")
public class Main {

    @Inject
    private Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = false)
    public Path defaultConf;

    @Inject
    @ConfigDir(sharedRoot = false)
    public Path ConfigDir;

    public Config config;
    public Messages messages;

    private final CommandManager cmdManager = Sponge.getCommandManager();

    private ArrayList<String> waitTimer;
    private DataStoreManager dataStoreManager;
    private PlotFlagManager plotFlagManager;

    public static Main INSTANCE;

    public Main() {
        Main.INSTANCE = this;
    }

    @Listener
    public void Init(GameInitializationEvent event) throws IOException, ObjectMappingException {
        DiscordUtil.setPlugin(this);
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(TicketData.class), new TicketSerializer());
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(PlayerData.class), new PlayerDataSerializer());
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(PlotSuspension.class), new PlotSuspensionSerializer());

        config = new Config(this);
        messages = new Messages(this);
        loadCommands();
    }

    @Listener
    public void onServerAboutStart(GameAboutToStartServerEvent event) {
        dataStoreManager = new DataStoreManager(this);
        if (dataStoreManager.load()) {
            getLogger().info("MMCTickets datastore Loaded");
        }
        else {
            getLogger().error("Unable to load a datastore please check your Console/Config!");
        }
    }

    public PlotFlagManager getPlotFlagManager() {
        return this.plotFlagManager;
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        this.plotFlagManager = new PlotFlagManager();
        this.plotFlagManager.init();

        getLogger().info("MMCTickets Loaded");
        getLogger().info("Tickets loaded: " + getDataStore().getTicketData().size());
        getLogger().info("Notifications loaded: " + getDataStore().getNotifications().size());
        getLogger().info("PlayerData loaded: " + getDataStore().getPlayerData().size());
        getLogger().info("PlotSuspensions loaded: " + getDataStore().getSuspensions().size());

        EventListener el = new EventListener(this);
        Sponge.getEventManager().registerListeners(this, el);
        MagiBridge.jda.addEventListener(el);
        this.waitTimer = new ArrayList<>();

        nagTimer();
    }

    @Listener
    public void onPluginReload(GameReloadEvent event) throws IOException, ObjectMappingException {
        this.config = new Config(this);
        this.messages = new Messages(this);
        dataStoreManager = new DataStoreManager(this);
        loadDataStore();
    }

    public void loadDataStore() {
        if (dataStoreManager.load()) {
            getLogger().info("MMCTickets datastore Loaded");
        }
        else {
            getLogger().error("Unable to load a datastore please check your Console/Config!");
        }
    }

    public void setDataStoreManager(DataStoreManager dataStoreManager) {
        this.dataStoreManager = dataStoreManager;
    }

    private void loadCommands() {
        // /stafflist
        CommandSpec staffList = CommandSpec.builder()
                .description(Text.of("List online staff members"))
                .executor(new Staff(this))
                .build();

        // /ticket read self
        CommandSpec readSelf = CommandSpec.builder()
                .description(Text.of("Display a list of all tickets the player owns"))
                .executor(new ReadSelf(this))
                .build();

        // /ticket read closed
        CommandSpec readClosed = CommandSpec.builder()
                .description(Text.of("Display a list of all closed tickets"))
                .executor(new ReadClosed(this))
                .permission(Permissions.COMMAND_TICKET_READ_ALL)
                .build();

        // /ticket read held
        CommandSpec readHeld = CommandSpec.builder()
                .description(Text.of("Display a list of all held tickets"))
                .executor(new ReadHeld(this))
                .permission(Permissions.COMMAND_TICKET_READ_ALL)
                .build();

        // /ticket read (ticketID)
        CommandSpec ticketRead = CommandSpec.builder()
                .description(Text.of("Read all ticket or give more detail of a specific ticket"))
                .executor(new Read(this))
                .child(readClosed, "closed")
                .child(readHeld, "held")
                .child(readSelf, "self")
                .arguments(GenericArguments.optional(GenericArguments.integer(Text.of("ticketID"))))
                .build();

        // /ticket close (ticketID) (comment)
        CommandSpec ticketClose = CommandSpec.builder()
                .description(Text.of("Close a ticket"))
                .executor(new Close(this))
                .arguments(
                        GenericArguments.integer(Text.of("ticketID")),
                        GenericArguments.optional(
                                GenericArguments.remainingJoinedStrings(Text.of("comment"))))
                .build();

        // /ticket open
        CommandSpec ticketOpen = CommandSpec.builder()
                .description(Text.of("Open a ticket"))
                .executor(new Open(this))
                .arguments(GenericArguments.remainingJoinedStrings(Text.of("message")))
                .permission(Permissions.COMMAND_TICKET_OPEN)
                .build();

        CommandSpec addStaff = CommandSpec.builder()
                .description(Text.of("Adds additional reviewers to a ticket"))
                .executor(new AddStaff(this))
                .arguments(GenericArguments.integer(Text.of("ticketID")), GenericArguments.user(Text.of("player")))
                .permission(Permissions.COMMAND_ADD_STAFF)
                .build();

        // /ticket reload
        CommandSpec ticketReload = CommandSpec.builder()
                .description(Text.of("Reload ticket and player data."))
                .executor(new Reload(this))
                .permission(Permissions.COMMAND_RELOAD)
                .build();

        // /ticket claim (ticketID)
        CommandSpec ticketClaim = CommandSpec.builder()
                .description(Text.of("Claim a ticket"))
                .executor(new Claim(this))
                .arguments(GenericArguments.integer(Text.of("ticketID")))
                .permission(Permissions.COMMAND_TICKET_CLAIM)
                .build();

        // /ticket unclaim (ticketID)
        CommandSpec ticketUnclaim = CommandSpec.builder()
                .description(Text.of("Unclaim a ticket"))
                .executor(new Unclaim(this))
                .arguments(GenericArguments.integer(Text.of("ticketID")))
                .permission(Permissions.COMMAND_TICKET_UNCLAIM)
                .build();

        // /ticket reopen (ticketID)
        CommandSpec ticketReopen = CommandSpec.builder()
                .description(Text.of("Reopen a ticket"))
                .executor(new Reopen(this))
                .arguments(GenericArguments.integer(Text.of("ticketID")))
                .permission(Permissions.COMMAND_TICKET_REOPEN)
                .build();

        // /ticket assign (ticketID) (player)
        CommandSpec ticketAssign = CommandSpec.builder()
                .description(Text.of("Unclaim a ticket"))
                .executor(new Assign(this))
                .arguments(
                        GenericArguments.integer(Text.of("ticketID")),
                        GenericArguments.user(Text.of("player")))
                .permission(Permissions.COMMAND_TICKET_ASSIGN)
                .build();

        // /ticket hold (ticketID)
        CommandSpec ticketHold = CommandSpec.builder()
                .description(Text.of("Put a ticket on hold"))
                .executor(new Hold(this))
                .arguments(GenericArguments.integer(Text.of("ticketID")))
                .permission(Permissions.COMMAND_TICKET_HOLD)
                .build();

        // /ticket comment (ticketID) (comment)
        CommandSpec ticketComment = CommandSpec.builder()
                .description(Text.of("Open a ticket"))
                .executor(new Comment(this))
                .arguments(
                        GenericArguments.integer(Text.of("ticketID")),
                        GenericArguments.remainingJoinedStrings(Text.of("comment")))
                .permission(Permissions.COMMAND_TICKET_COMMENT)
                .build();

        // /ticket teleport (ticketID)
        CommandSpec ticketTeleport = CommandSpec.builder()
                .description(Text.of("Teleport to a ticket"))
                .executor(new Teleport(this))
                .arguments(GenericArguments.integer(Text.of("ticketID")))
                .permission(Permissions.COMMAND_TICKET_TELEPORT)
                .build();

        // /ticket reject (ticketID)
        CommandSpec ticketReject = CommandSpec.builder()
                .description(Text.of("Reject a ticket"))
                .executor(new Reject(this))
                .arguments(
                        GenericArguments.integer(Text.of("ticketID")),
                        GenericArguments.optional(
                                GenericArguments.remainingJoinedStrings(Text.of("comment"))))
                .permission(Permissions.COMMAND_TICKET_CLOSE_ALL)
                .build();

        // /ticket
        CommandSpec ticketBase = CommandSpec.builder()
                .description(Text.of("Ticket base command, Displays help"))
                .executor(new Ticket(this))
                .child(ticketOpen, "open")
                .child(ticketRead, "read", "check")
                .child(ticketClose, "close", "complete")
                .child(ticketReload, "reload")
                .child(ticketClaim, "claim")
                .child(ticketUnclaim, "unclaim")
                .child(ticketReopen, "reopen")
                .child(ticketAssign, "assign")
                .child(ticketHold, "hold")
                .child(ticketComment, "comment")
                .child(ticketTeleport, "teleport", "tp")
                .child(ticketReject, "reject")
                .child(addStaff, "add")
                .build();

        // plot suspend
        CommandSpec plotSuspend = CommandSpec.builder()
                .description(Text.of("Suspend a plot from submission"))
                .executor(new Suspend(this))
                .arguments(GenericArguments.string(Text.of("time")))
                .permission(Permissions.COMMAND_PLOT_SUSPEND)
                .build();

        cmdManager.register(this, ticketOpen, "modreq");
        cmdManager.register(this, ticketRead, "check");
        cmdManager.register(this, ticketBase, "ticket");
        cmdManager.register(this, staffList, "stafflist");
        cmdManager.register(this, plotSuspend, "suspend");
    }

    public Logger getLogger() {
        return logger;
    }

    public IDataStore getDataStore() {
        return dataStoreManager.getDataStore();
    }

    public void nagTimer() {
        if (Config.nagTimer > 0) {
            Sponge.getScheduler().createSyncExecutor(this).scheduleWithFixedDelay(() -> {
                        final List<TicketData> tickets =
                                new ArrayList<>(getDataStore().getTicketData());
                        int openTickets = 0;
                        int heldTickets = 0;
                        for (TicketData ticket : tickets) {
                            if (ticket.getStatus() == TicketStatus.OPEN || ticket.getStatus() == TicketStatus.CLAIMED) {
                                openTickets++;
                            }
                            if (ticket.getStatus() == TicketStatus.HELD) {
                                heldTickets++;
                            }
                        }
                        if (Config.nagHeld) {
                            if (heldTickets > 0) {
                                if (openTickets > 0) {
                                    CommonUtil.notifyOnlineStaff(
                                            Messages.getTicketUnresolvedHeld(openTickets, heldTickets, "check"));
                                }
                            }
                            else {
                                if (openTickets > 0) {
                                    CommonUtil.notifyOnlineStaff(
                                            Messages.getTicketUnresolved(openTickets, "check"));
                                }
                            }
                        }
                        else {
                            if (openTickets > 0) {
                                CommonUtil.notifyOnlineStaff(
                                        Messages.getTicketUnresolved(openTickets, "check"));
                            }
                        }
                    },
                    Config.nagTimer,
                    Config.nagTimer,
                    TimeUnit.MINUTES);
        }
    }

    public ArrayList<String> getWaitTimer() {
        return this.waitTimer;
    }

    public Text fromLegacy(String legacy) {
        return TextSerializers.FORMATTING_CODE.deserializeUnchecked(legacy);
    }

    @Deprecated
    public List<TicketData> getTickets() {
        return getDataStore().getTicketData();
    }

    @Deprecated
    public @Nullable TicketData getTicket(int ticketID) {
        if (getDataStore().getTicket(ticketID).isPresent()) {
            return getDataStore().getTicket(ticketID).get();
        }
        return null;
    }
}
