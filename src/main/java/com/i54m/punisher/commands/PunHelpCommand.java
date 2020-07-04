package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

public class PunHelpCommand extends Command {

    public PunHelpCommand() {
        super("punisherhelp", null, "punhelp", "phelp");
    }//todo reformat command output to look nicer

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        commandSender.sendMessage(new ComponentBuilder("|-------------------").color(ChatColor.RED).strikethrough(true).append("[").color(ChatColor.GRAY).strikethrough(false).append("Punisher - Help").color(ChatColor.RED).strikethrough(false)
                .append("]").strikethrough(false).color(ChatColor.GRAY).append("-------------------|").color(ChatColor.RED).strikethrough(true).create());
        if (commandSender.hasPermission("punisher.adminchat"))
            commandSender.sendMessage(new ComponentBuilder("/adminchat <message>").color(ChatColor.RED).append(": Send a message to admin chat").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.alts"))
            commandSender.sendMessage(new ComponentBuilder("/alts <reset|player name>").color(ChatColor.RED).append(": Check a players alt or reset their stored ip").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.ban"))
            commandSender.sendMessage(new ComponentBuilder("/ban <player> [length<s|m|h|d|w|M|perm>] [reason]").color(ChatColor.RED).append(": Manually ban a player").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.clearchat"))
            commandSender.sendMessage(new ComponentBuilder("/clearchat").color(ChatColor.RED).append(": clear the chat for all players (except staff)").color(ChatColor.WHITE).create());
        if (PunisherPlugin.getInstance().getConfig().getBoolean("DiscordIntegration.Enabled")) {
            if (commandSender.hasPermission("punisher.discord"))
                commandSender.sendMessage(new ComponentBuilder("/discord").color(ChatColor.RED).append(": Main command for Discord integration").color(ChatColor.WHITE).create());
            if (commandSender.hasPermission("punisher.discord.admin"))
                commandSender.sendMessage(new ComponentBuilder("/discord admin").color(ChatColor.RED).append(": Admin Command for Discord integration").color(ChatColor.WHITE).create());
        }
        if (commandSender.hasPermission("punisher.history"))
            commandSender.sendMessage(new ComponentBuilder("/history <player>").color(ChatColor.RED).append(": View a player's punishment history").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.alts.ip")) {
            commandSender.sendMessage(new ComponentBuilder("/ip <player>").color(ChatColor.RED).append(": View an online player's ip").color(ChatColor.WHITE).create());
            commandSender.sendMessage(new ComponentBuilder("/iphist <player>").color(ChatColor.RED).append(": View a player's ip history").color(ChatColor.WHITE).create());
        }
        if (commandSender.hasPermission("punisher.kick.all"))
            commandSender.sendMessage(new ComponentBuilder("/kickall").color(ChatColor.RED).append(": Kick all players from the current server or entire network").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.kick"))
            commandSender.sendMessage(new ComponentBuilder("/kick <player>").color(ChatColor.RED).append(": Kick a player from the server").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.mute"))
            commandSender.sendMessage(new ComponentBuilder("/mute <player> [length<s|m|h|d|w|M|perm>] [reason]").color(ChatColor.RED).append(": Manually mute a player").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.notes"))
            commandSender.sendMessage(new ComponentBuilder("/notes <player>").color(ChatColor.RED).append(": Add, Remove or View notes for a player").color(ChatColor.WHITE).create());
        commandSender.sendMessage(new ComponentBuilder("/ping [player]").color(ChatColor.RED).append(": Shows the user their ping or another player's ping").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.playerinfo"))
            commandSender.sendMessage(new ComponentBuilder("/playerinfo <player>").color(ChatColor.RED).append(": View useful information on a player").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.admin"))
            commandSender.sendMessage(new ComponentBuilder("/punisher").color(ChatColor.RED).append(": Admin command for the punisher").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.punish.level.0"))
            commandSender.sendMessage(new ComponentBuilder("/punish <player>").color(ChatColor.RED).append(": Bring up the main punishment gui").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.report"))
            commandSender.sendMessage(new ComponentBuilder("/report <player> <reason>").color(ChatColor.RED).append(": Report a player who is breaking the rules to online staff members").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.reputation"))
            commandSender.sendMessage(new ComponentBuilder("/reputation <player> <plus|minus|set> <amount>").color(ChatColor.RED).append(": Change a player's reputation").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.seen"))
            commandSender.sendMessage(new ComponentBuilder("/seen <player>").color(ChatColor.RED).append(": View the last seen information for a player").color(ChatColor.WHITE).create());
        commandSender.sendMessage(new ComponentBuilder("/staff").color(ChatColor.RED).append(": View all online staff members").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.staffchat"))
            commandSender.sendMessage(new ComponentBuilder("/staffchat <message>").color(ChatColor.RED).append(": Send a message to staff chat").color(ChatColor.WHITE).create());
        if(commandSender.hasPermission("punisher.staff.hide"))
            commandSender.sendMessage(new ComponentBuilder("/staffhide <on|off>").color(ChatColor.RED).append(": Hide or unhide from the staff list").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.staffhistory"))
            commandSender.sendMessage(new ComponentBuilder("/staffhistory <player> [-c (clear)|-r (reset)]").color(ChatColor.RED).append(": View a staff member's punishment history").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.superbroadcast"))
            commandSender.sendMessage(new ComponentBuilder("/superbroadcast <message>").color(ChatColor.RED).append(": Make an announcement to everyone on the server").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.togglechat"))
            commandSender.sendMessage(new ComponentBuilder("/togglechat").color(ChatColor.RED).append(": Toggle the chat for all players (expect staff)").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.unban"))
            commandSender.sendMessage(new ComponentBuilder("/unban <player>").color(ChatColor.RED).append(": Unban a player who is banned").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.unmute"))
            commandSender.sendMessage(new ComponentBuilder("/unmute <player>").color(ChatColor.RED).append(": Unmute a player who is muted").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.unpunish"))
            commandSender.sendMessage(new ComponentBuilder("/unpunish <player> <punishment>").color(ChatColor.RED).append(": Remove a punishment from a player's history").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.viewrep"))
            commandSender.sendMessage(new ComponentBuilder("/viewrep <player>").color(ChatColor.RED).append(": View a player's reputation").color(ChatColor.WHITE).create());
        if (commandSender.hasPermission("punisher.warn"))
            commandSender.sendMessage(new ComponentBuilder("/warn <player>").color(ChatColor.RED).append(": Warn a player for breaking the rules").color(ChatColor.WHITE).create());
    }
}
