package com.i54m.punisher.handlers;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.chats.AdminChat;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ErrorHandler {

    @Getter
    private static final ErrorHandler INSTANCE = new ErrorHandler();
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private Throwable previousException = null;

    private ErrorHandler() {
    }

    public void log(Throwable e) {
        if (!isExceptionCausedByPunisher(e) && !isExceptionCausedByProtocol(e))
            return;
        if (previousException != null && e.getMessage().equals(previousException.getMessage())) {
            logToFile(e);
            return;
        } else
            previousException = e;
        logToFile(e);
        if (isExceptionCausedByProtocol(e)) {
            plugin.getLogger().severe(" ");
            plugin.getLogger().severe(ChatColor.RED + "An error was encountered in the packet system! Debug info was logged to log file!");
            plugin.getLogger().severe(ChatColor.RED + "Error Message: " + e.getMessage());
            plugin.getLogger().severe(" ");
        } else {
            plugin.getLogger().warning(" ");
            plugin.getLogger().warning(ChatColor.RED + "An error was encountered and debug info was logged to log file!");
            plugin.getLogger().warning(ChatColor.RED + "Error Message: " + e.getMessage());
            plugin.getLogger().warning(" ");
        }
    }

    private void logToFile(Throwable e) {
        PunisherPlugin.getLOGS().severe("Error Type: " + e.getClass().getName());
        PunisherPlugin.getLOGS().severe("Error Message: " + e.getMessage());
        StringBuilder stacktrace = new StringBuilder();
        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
            stacktrace.append(stackTraceElement.toString()).append("\n");
        }
        PunisherPlugin.getLOGS().severe("Stack Trace: " + stacktrace.toString());
        PunisherPlugin.getLOGS().severe("\n");
    }

    private void detailedAlert(Throwable e, CommandSender sender) {
        if (!isExceptionCausedByPunisher(e) && !isExceptionCausedByProtocol(e))
            return;
        if (e.getMessage().equals(previousException.getMessage()))
            return;
        else
            previousException = e;
        if (isExceptionCausedByProtocol(e)) {
            sender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("CRITICAL ERROR: ").color(ChatColor.DARK_RED).append(e.getMessage()).color(ChatColor.RED).create());
            sender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("This error will be logged! Please inform a dev ASAP this error may produce instabilities!").color(ChatColor.RED).create());

        }
        sender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("ERROR: ").color(ChatColor.DARK_RED).append(e.getMessage()).color(ChatColor.RED).create());
        sender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("This error will be logged! Please inform a dev ASAP, this error may produce instabilities!").color(ChatColor.RED).create());
    }

    public void alert(Throwable e, CommandSender sender) {
        if (!isExceptionCausedByPunisher(e) && !isExceptionCausedByProtocol(e))
            return;
        if (e.getMessage().equals(previousException.getMessage()))
            return;
        else
            previousException = e;
        if (sender.hasPermission("punisher.admin")) detailedAlert(e, sender);
        else {
            sender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("ERROR: ").color(ChatColor.DARK_RED).append("An unexpected error occurred while trying to perform that action!").color(ChatColor.RED).create());
            sender.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("This error will be logged! Please inform an admin+ ASAP, this error may produce instabilities!").color(ChatColor.RED).create());
            adminChatAlert(e, sender);
        }
    }

    public void adminChatAlert(Throwable e, CommandSender sender) {
        if (!isExceptionCausedByPunisher(e) && !isExceptionCausedByProtocol(e))
            return;
        if (e.getMessage().equals(previousException.getMessage()))
            return;
        else
            previousException = e;
        AdminChat.sendMessage(sender.getName() + " ENCOUNTERED AN ERROR: " + e.getMessage());
        AdminChat.sendMessage("This error will be logged! Please inform a dev ASAP, this error may produce instabilities!");
    }

    public void loginError(PreLoginEvent event) {
        event.setCancelled(true);
        event.setCancelReason(new TextComponent(ChatColor.RED + "ERROR: An error occurred during your login process and we were unable to fetch required data.\n Please inform an admin+ ASAP this error may produce instabilities!"));
    }

    public void loginError(LoginEvent event) {
        event.setCancelled(true);
        event.setCancelReason(new TextComponent(ChatColor.RED + "ERROR: An error occurred during your login process and we were unable to fetch required data.\n Please inform an admin+ ASAP this error may produce instabilities!"));
    }

    public void loginError(PostLoginEvent event) {
        event.getPlayer().disconnect(new TextComponent(ChatColor.RED + "ERROR: An error occurred during your login process and we were unable to fetch required data.\n Please inform an admin+ ASAP this error may produce instabilities!"));
    }

    public boolean isExceptionCausedByPunisher(final Throwable e) {
        final List<StackTraceElement> all = getEverything(e, new ArrayList<>());
        for (final StackTraceElement element : all) {
            if (element.getClassName().toLowerCase().contains("com.i54m.punisher"))
                return true;
        }
        return false;
    }

    private List<StackTraceElement> getEverything(final Throwable e, List<StackTraceElement> objects) {
        if (e.getCause() != null)
            objects = getEverything(e.getCause(), objects);
        objects.addAll(Arrays.asList(e.getStackTrace()));
        return objects;
    }

    public boolean isExceptionCausedByProtocol(final Throwable e) {
        final List<StackTraceElement> all = getEverything(e, new ArrayList<>());
        for (final StackTraceElement element : all) {
            if (element.getClassName().toLowerCase().contains("com.i54m.protocol"))
                return true;
        }
        return false;
    }

}
