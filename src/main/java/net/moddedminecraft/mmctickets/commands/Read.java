package net.moddedminecraft.mmctickets.commands;

import com.flowpowered.math.vector.Vector3d;

import net.moddedminecraft.mmctickets.Main;
import net.moddedminecraft.mmctickets.config.Config;
import net.moddedminecraft.mmctickets.config.Messages;
import net.moddedminecraft.mmctickets.config.Permissions;
import net.moddedminecraft.mmctickets.data.TicketComment;
import net.moddedminecraft.mmctickets.data.TicketData;
import net.moddedminecraft.mmctickets.util.CommonUtil;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static net.moddedminecraft.mmctickets.config.Permissions.COMMAND_TICKET_COMMENT;
import static net.moddedminecraft.mmctickets.data.TicketStatus.APPROVED;
import static net.moddedminecraft.mmctickets.data.TicketStatus.CLAIMED;
import static net.moddedminecraft.mmctickets.data.TicketStatus.CLOSED;
import static net.moddedminecraft.mmctickets.data.TicketStatus.HELD;
import static net.moddedminecraft.mmctickets.data.TicketStatus.OPEN;
import static net.moddedminecraft.mmctickets.data.TicketStatus.REJECTED;

public class Read implements CommandExecutor {

    private final Main plugin;

    public Read(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        final Optional<Integer> ticketIDOp = args.<Integer>getOne("ticketID");

        final List<TicketData> tickets =
                new ArrayList<TicketData>(plugin.getDataStore().getTicketData());

        UUID uuid = null;
        if (src instanceof Player) {
            Player player = (Player) src;
            uuid = player.getUniqueId();
        }

        if (tickets.isEmpty()) {
            throw new CommandException(Messages.getErrorGen("Tickets list is empty."));
        }
        else {
            if (!ticketIDOp.isPresent()) {
                if (src.hasPermission(Permissions.COMMAND_TICKET_READ_ALL)) {
                    PaginationService paginationService =
                            Sponge.getServiceManager().provide(PaginationService.class).get();
                    List<Text> contents = new ArrayList<>();
                    int totalTickets = 0;
                    for (TicketData ticket : tickets) {
                        if (ticket.getStatus() == CLAIMED || ticket.getStatus() == OPEN) {
                            String online = CommonUtil.isUserOnline(ticket.getPlayerUUID());
                            totalTickets++;
                            Text.Builder send = Text.builder();
                            String status = "";
                            if (ticket.getStatus() == CLAIMED) status = "&bClaimed - ";
                            send.append(
                                    plugin.fromLegacy(
                                            status
                                                    + "&3#"
                                                    + ticket.getTicketID()
                                                    + " "
                                    ));

                            if (src.hasPermission(Permissions.COMMAND_TICKET_CLAIM) && ticket.getStatus().equals(OPEN)) {
                                send.append(Text.builder()
                                        .append(plugin.fromLegacy(Messages.getClaimButton()))
                                        .onHover(TextActions.showText(plugin.fromLegacy(Messages.getClaimButtonHover())))
                                        .onClick(TextActions.runCommand("/ticket claim " + ticket.getTicketID()))
                                        .build());
                            }

                            send.append(Text.builder()
                                    .append(plugin.fromLegacy(
                                            " &3- &b"
                                                    + CommonUtil.getTimeAgo(ticket.getTimestamp())
                                                    + " by "
                                                    + online
                                                    + CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID())
                                                    + " &3- &b"
                                                    + CommonUtil.shortenMessage(ticket.getMessage()).replaceAll(" {2}\\| {2}", ", ")))
                                    .onClick(TextActions.runCommand("/ticket read " + ticket.getTicketID()))
                                    .onHover(
                                            TextActions.showText(
                                                    plugin.fromLegacy(
                                                            "Click here to get more details for ticket #" + ticket.getTicketID())))
                                    .build()
                            );

                            contents.add(send.build());
                        }
                    }

                    if (contents.isEmpty()) {
                        contents.add(Messages.getTicketReadNone());
                    }
                    int ticketsPer = 5;
                    if (Config.ticketsPerPage > 0) {
                        ticketsPer = Config.ticketsPerPage;
                    }
                    paginationService
                            .builder()
                            .title(plugin.fromLegacy("&3" + totalTickets + " Open Tickets"))
                            .contents(contents)
                            .padding(Text.of("-"))
                            .linesPerPage(ticketsPer + 2)
                            .sendTo(src);
                    return CommandResult.success();
                }
                else {
                    if (src.hasPermission(Permissions.COMMAND_TICKET_READ_SELF)) {
                        throw new CommandException(Messages.getErrorIncorrectUsage("/check self or /check #"));
                    }
                    else {
                        throw new CommandException(
                                Messages.getErrorPermission(Permissions.COMMAND_TICKET_READ_ALL));
                    }
                }
            }
            else {
                if (src.hasPermission(Permissions.COMMAND_TICKET_READ_ALL)
                        || (src.hasPermission(Permissions.COMMAND_TICKET_READ_SELF))) {
                    PaginationService paginationService =
                            Sponge.getServiceManager().provide(PaginationService.class).get();
                    List<Text> contents = new ArrayList<>();
                    int ticketID = ticketIDOp.get();
                    String ticketStatus = "";
                    for (TicketData ticket : tickets) {
                        if (ticket.getTicketID() == ticketID) {
                            if (!ticket.getPlayerUUID().equals(uuid)
                                    && !src.hasPermission(Permissions.COMMAND_TICKET_READ_ALL)) {
                                throw new CommandException(Messages.getErrorTicketOwner());
                            }
                            ticketStatus = CommonUtil.getTicketStatusColour(ticket.getStatus());
                            String online = CommonUtil.isUserOnline(ticket.getPlayerUUID());
                            Optional<World> worldOptional = Sponge.getServer().loadWorld(ticket.getWorld());

                            Text.Builder action = Text.builder();

                            if (ticket.getStatus() == OPEN || ticket.getStatus() == CLAIMED) {
                                if (ticket.getStatus() == OPEN
                                        && src.hasPermission(Permissions.COMMAND_TICKET_CLAIM)) {

                                    boolean showClaim = false;

                                    if (ticket.getWorld().equals("Plots2") || ticket.getWorld().equals("Plots2B")) {
                                        if (src.hasPermission(Permissions.COMMAND_TICKET_CLAIM_EXPERT))
                                            showClaim = true;
                                    }
                                    else if (ticket.getWorld().equals("MasterPlots")) {
                                        if (src.hasPermission(Permissions.COMMAND_TICKET_CLAIM_MASTER))
                                            showClaim = true;
                                    }
                                    else showClaim = true;

                                    if (showClaim) {
                                        action.append(
                                                Text.builder()
                                                        .append(plugin.fromLegacy(Messages.getClaimButton()))
                                                        .onHover(TextActions.showText(plugin.fromLegacy(Messages.getClaimButtonHover())))
                                                        .onClick(TextActions.runCommand("/ticket claim " + ticket.getTicketID()))
                                                        .build());

                                        action.append(plugin.fromLegacy(" "));
                                    }
                                }

                                if (ticket.getStatus() == CLAIMED) {
                                    if (ticket.getStaffUUID().equals(uuid)
                                            && src.hasPermission(Permissions.COMMAND_TICKET_UNCLAIM)) {
                                        action.append(Text.builder()
                                                .append(plugin.fromLegacy(Messages.getUnclaimButton()))
                                                .onHover(TextActions.showText(plugin.fromLegacy(Messages.getUnclaimButtonHover())))
                                                .onClick(TextActions.runCommand("/ticket unclaim " + ticket.getTicketID()))
                                                .build());
                                        action.append(plugin.fromLegacy(" "));
                                    }
                                }
                                if ((ticket.getStatus() == OPEN
                                        || ticket.getStatus() == CLAIMED && ticket.getStaffUUID().equals(uuid))
                                        && src.hasPermission(Permissions.COMMAND_TICKET_HOLD)) {
                                    action.append(Text.builder()
                                            .append(plugin.fromLegacy(Messages.getHoldButton()))
                                            .onHover(TextActions.showText(plugin.fromLegacy(Messages.getHoldButtonHover())))
                                            .onClick(TextActions.runCommand("/ticket hold " + ticket.getTicketID()))
                                            .build());
                                    action.append(plugin.fromLegacy(" "));
                                }
                                if ((ticket.getStatus() == CLAIMED) && ticket.getStaffUUID().equals(uuid)) {
                                    action.append(Text.builder()
                                            .append(plugin.fromLegacy(Messages.getAddReviewerButton()))
                                            .onHover(TextActions.showText(plugin.fromLegacy(Messages.getAddReviewerButtonHover())))
                                            .onClick(TextActions.suggestCommand("/ticket add " + ticket.getTicketID() + " "))
                                            .build());
                                }

                            }
                            if (ticket.getStatus() == HELD || ticket.getStatus() == CLOSED || ticket.getStatus() == REJECTED || ticket.getStatus() == APPROVED) {
                                if (src.hasPermission(Permissions.COMMAND_TICKET_REOPEN)) {
                                    action.append(Text.builder()
                                            .append(plugin.fromLegacy(Messages.getReopenButton()))
                                            .onHover(TextActions.showText(plugin.fromLegacy(Messages.getReopenButtonHover())))
                                            .onClick(TextActions.runCommand("/ticket reopen " + ticket.getTicketID()))
                                            .build());
                                    action.append(plugin.fromLegacy(" "));
                                }
                            }
                            if (src.hasPermission(COMMAND_TICKET_COMMENT)) {
                                if (ticket.getStatus() != CLAIMED
                                        || ticket.getStatus() == CLAIMED && ticket.getStaffUUID().equals(uuid)) {
                                    action.append(Text.builder()
                                            .append(plugin.fromLegacy(Messages.getCommentButton()))
                                            .onHover(TextActions.showText(plugin.fromLegacy(Messages.getCommentButtonHover())))
                                            .onClick(TextActions.suggestCommand("/ticket comment " + ticket.getTicketID() + " "))
                                            .build());
                                }
                            }


                            Text.Builder promotionActions = Text.builder();

                            if (src.hasPermission(Permissions.TICKET_PROMOTE)) {
                                promotionActions.append(Text.NEW_LINE);
                                if (ticket.getStatus() == HELD
                                        || ticket.getStatus() == CLAIMED
                                        || ticket.getStatus() == OPEN) {
                                    if ((ticket.getStatus() == CLAIMED && ticket.getStaffUUID().equals(uuid))
                                            || ticket.getStatus() == OPEN
                                            || ticket.getStatus() == HELD) {
                                        if (src.hasPermission(Permissions.COMMAND_TICKET_CLOSE_ALL)
                                                || src.hasPermission(Permissions.COMMAND_TICKET_CLOSE_SELF)) {

                                            if (ticket.getWorld().equals("Plots1")) {
                                                promotionActions.append(Text.builder()
                                                        .append(Text.of(TextColors.AQUA, "[", TextColors.GREEN, "Promote - Member", TextColors.AQUA, "]"))
                                                        .onHover(TextActions.showText(Text.of(TextColors.AQUA, "Promote to Member and close ticket")))
                                                        .onClick(TextActions.runCommand("/multi ticket complete " + ticket.getTicketID() + "|promote " + CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()) + " Member"))
                                                        .build());
                                            }
                                            else if (ticket.getWorld().equals("Plots2") || ticket.getWorld().equals("Plots2B")) {
                                                promotionActions.append(Text.builder()
                                                        .append(Text.of(TextColors.AQUA, "[", TextColors.YELLOW, "Promote - Expert", TextColors.AQUA, "]"))
                                                        .onHover(TextActions.showText(Text.of(TextColors.AQUA, "Promote to Expert and close ticket")))
                                                        .onClick(TextActions.runCommand("/multi ticket complete " + ticket.getTicketID() + "|promote " + CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()) + " Expert"))
                                                        .build());
                                            }
                                            else if (ticket.getWorld().equals("MasterPlots")) {
                                                promotionActions.append(Text.NEW_LINE);
                                                promotionActions.append(Text.builder()
                                                        .append(Text.of(TextColors.AQUA, "[", TextColors.AQUA, "Promote - MS:Nature", TextColors.AQUA, "]"))
                                                        .onHover(TextActions.showText(Text.of(TextColors.AQUA, "Promote to Mastered Skill Nature and close ticket")))
                                                        .onClick(TextActions.runCommand("/multi ticket complete " + ticket.getTicketID() + "|master " + CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()) + " nat"))
                                                        .build());
                                                promotionActions.append(plugin.fromLegacy(" "));
                                                promotionActions.append(Text.builder()
                                                        .append(Text.of(TextColors.AQUA, "[", TextColors.AQUA, "Promote - MS:Architecture", TextColors.AQUA, "]"))
                                                        .onHover(TextActions.showText(Text.of(TextColors.AQUA, "Promote to Mastered Skill Architecture and close ticket")))
                                                        .onClick(TextActions.runCommand("/multi ticket complete " + ticket.getTicketID() + "|master " + CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()) + " arch"))
                                                        .build());
                                                promotionActions.append(plugin.fromLegacy(" "));
                                                promotionActions.append(Text.builder()
                                                        .append(Text.of(TextColors.AQUA, "[", TextColors.AQUA, "Promote - MS:Both", TextColors.AQUA, "]"))
                                                        .onHover(TextActions.showText(Text.of(TextColors.AQUA, "Promote to both Mastered Skills and close ticket")))
                                                        .onClick(TextActions.runCommand("/multi ticket complete " + ticket.getTicketID() + "|master " + CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()) + " all"))
                                                        .build());
                                                promotionActions.append(Text.NEW_LINE);
                                            }
                                            else {

                                            }
                                            promotionActions.append(plugin.fromLegacy(" "));
                                        }
                                    }
                                }
                            }
                            if (ticket.getStatus() == HELD
                                    || ticket.getStatus() == CLAIMED
                                    || ticket.getStatus() == OPEN) {
                                if (ticket.getStatus() == HELD
                                        || ticket.getStatus() == CLAIMED
                                        || ticket.getStatus() == OPEN) {
                                    if ((ticket.getStatus() == CLAIMED && ticket.getStaffUUID().equals(uuid))
                                            || ticket.getStatus() == OPEN
                                            || ticket.getStatus() == HELD) {
                                        if (src.hasPermission(Permissions.COMMAND_TICKET_CLOSE_ALL)
                                                || src.hasPermission(Permissions.COMMAND_TICKET_CLOSE_SELF)) {
                                            promotionActions.append(
                                                    Text.builder()
                                                            .append(plugin.fromLegacy(Messages.getRejectButton()))
                                                            .onHover(TextActions.showText(plugin.fromLegacy(Messages.getRejectButtonHover())))
                                                            .onClick(TextActions.runCommand("/ticket reject " + ticket.getTicketID()))
                                                            .build());
                                            promotionActions.append(plugin.fromLegacy(" "));
                                        }
                                    }
                                }
                            }

                            Text.Builder teleportButton = Text.builder();
                            teleportButton.append(Text.of(TextColors.AQUA, "[", TextColors.DARK_AQUA, "Teleport to " + ticket.getMessage(), TextColors.AQUA, "]"));
                            if (src.hasPermission(Permissions.COMMAND_TICKET_TELEPORT) && ticket.getServer().equalsIgnoreCase(Config.server)) {
                                teleportButton.onHover(TextActions.showText(Messages.getTicketOnHoverTeleportTo()));
                                worldOptional.ifPresent(world -> teleportButton.onClick(
                                        TextActions.executeCallback(
                                                teleportTo(world, ticket.getX(), ticket.getY(), ticket.getZ(),
                                                        ticket.getPitch(), ticket.getYaw(), ticketID))));
                            }

                            if (!action.build().isEmpty()) {
                                contents.add(action.build());
                            }

                            if (!ticket
                                    .getPlayerUUID()
                                    .toString()
                                    .equals("00000000-0000-0000-0000-000000000000")) {
                                contents.add(teleportButton.build());
                            }

                            if (!ticket
                                    .getStaffUUID()
                                    .toString()
                                    .equals("00000000-0000-0000-0000-000000000000")) {
                                if (ticket.getStatus() == CLAIMED)
                                    contents.add(
                                            plugin.fromLegacy(
                                                    "&bClaimed by: &7"
                                                            + String.join(", ", ticket.getAdditionalReviewers())));
                                else if (ticket.getStatus() == CLOSED || ticket.getStatus() == REJECTED || ticket.getStatus() == APPROVED)
                                    contents.add(
                                            plugin.fromLegacy(
                                                    "&bHandled by: &7"
                                                            + String.join(", ", ticket.getAdditionalReviewers())));
                            }

                            if (src.hasPermission(COMMAND_TICKET_COMMENT)) {
                                List<TicketComment> comments = plugin.getDataStore().getComments(ticket.getMessage(), ticket.getPlayerUUID());
                                if (!comments.isEmpty()) {
                                    contents.add(plugin.fromLegacy("&bComments: "));
                                    for (TicketComment comment : comments) {
                                        contents.add(plugin.fromLegacy(" &3[&b#" + comment.getTicketId() + ": " + comment.getSource() + " &3- &b" + CommonUtil.getTimeAgo(comment.getTimestamp().getTime()) + "&3] &7:"));
                                        contents.add(plugin.fromLegacy("&7 - " + comment.getComment()));
                                    }
                                }
                            }

                            int ticketNum =
                                    (int)
                                            tickets.stream()
                                                    .filter(
                                                            t ->
                                                                    t.getPlayerUUID()
                                                                            .toString()
                                                                            .equals(ticket.getPlayerUUID().toString())
                                                                            && t.getMessage().equals(ticket.getMessage()))
                                                    .count();

                            AtomicBoolean isOnline = new AtomicBoolean(false);
                            Sponge.getServer().getPlayer(ticket.getPlayerUUID()).ifPresent(p -> isOnline.set(p.isOnline()));

                            contents.add(
                                    Text.of(
                                            plugin.fromLegacy(
                                                    "&bOpened by: "
                                                            + online
                                                            + CommonUtil.getPlayerNameFromData(plugin, ticket.getPlayerUUID()) + " &7(" + (isOnline.get() ? "&cOnline" : "&bOffline") + "&7)"),
                                            TextColors.DARK_AQUA,
                                            " | Submission : #" + ticketNum));

                            contents.add(
                                    plugin.fromLegacy("&bWhen: " + CommonUtil.getTimeAgo(ticket.getTimestamp())));

                            contents.add(promotionActions.build());
                        }
                    }

                    if (contents.isEmpty()) {
                        throw new CommandException(Messages.getTicketNotExist(ticketID));
                    }

                    paginationService
                            .builder()
                            .title(plugin.fromLegacy("&3Request #" + ticketID + " &b- &3" + ticketStatus))
                            .contents(contents)
                            .padding(plugin.fromLegacy("&b-"))
                            .sendTo(src);
                    return CommandResult.success();
                }
                else {
                    throw new CommandException(
                            Messages.getErrorPermission(Permissions.COMMAND_TICKET_READ_SELF));
                }
            }
        }
    }

    private Consumer<CommandSource> teleportTo(
            World world, int x, int y, int z, double pitch, double yaw, int ticketID) {
        return consumer -> {
            Player player = (Player) consumer;

            Location<World> location = new Location<>(world, x, y, z);
            if (!world.isLoaded()) Sponge.getServer().loadWorld(world.getUniqueId());

            Vector3d playerRotation = new Vector3d(pitch, yaw, 0);
            player.setLocationAndRotation(location, playerRotation);
            player.sendMessage(Messages.getTeleportToTicket(ticketID));
        };
    }
}
