package com.i54mpenguin.punisher.chats;

import com.i54mpenguin.punisher.PunisherPlugin;
import com.i54mpenguin.punisher.utils.Permissions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.UUID;

public class AdminChat extends Command {

    public AdminChat() {
        super("ac", "punisher.adminchat", "adminchat");
    }

    public static String prefix;
    public static ChatColor color;
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length == 0) {
                player.sendMessage(new ComponentBuilder("Send a message to all admin members").color(ChatColor.RED).append("\nUsage: /adminchat <message>").color(ChatColor.WHITE).create());
                return;
            }
            int staff = 0;
//            plugin.staff.get(player.getServer().getInfo()).size();
            HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(player.getServer().getInfo().getPlayers().size() + " players on this server!").color(ChatColor.RED)
                    .append("\n" + staff + " Staff on this server!").color(ChatColor.RED).create());
            StringBuilder sb = new StringBuilder();
            for (String arg : strings)
                sb.append(arg).append(" ");
            UUID uuid = player.getUniqueId();
            String prefix = Permissions.getPrefix(uuid);
            sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', prefix.replace("%server%", player.getServer().getInfo().getName()).replace("%player%", player.getName()) + " "))
                    .append(sb.toString()).color(color).event(hover).create(), true);
        } else {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
        }
    }

    public static void sendMessage(String message, boolean prefix){sendMessage(new ComponentBuilder(message).create(), prefix);}

    public static void sendMessage(BaseComponent message, boolean prefix){sendMessage(new ComponentBuilder(message).create(), prefix);}

    public static void sendMessage(BaseComponent[] message, boolean prefix) {
        BaseComponent[] messagetosend;
        if (prefix)
            messagetosend = new ComponentBuilder(AdminChat.prefix).append(message).color(color).create();
        else
            messagetosend = new ComponentBuilder("").append(message).color(color).create();
        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
            if (all.hasPermission("punisher.adminchat")) {
                all.sendMessage(messagetosend);
            }
        }
    }
//todo go back through and re-choose the prefix formatting for this and staffchat
    public static void sendMessages(boolean prefix, String... messages){
        for (String message : messages) {
            sendMessage(message, prefix);
        }
    }
}