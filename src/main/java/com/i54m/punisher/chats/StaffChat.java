package com.i54m.punisher.chats;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.utils.Permissions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class StaffChat extends Command {

    public StaffChat() {
        super("sc", "punisher.staffchat", "staffchat");
    }

    public static String prefix;
    public static String prefixRaw;
    public static ChatColor color;
    private static final PunisherPlugin plugin = PunisherPlugin.getInstance();

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length == 0) {//todo make the command without args toggle chat mode?
                player.sendMessage(new ComponentBuilder("Send a message to all staff members").color(ChatColor.RED).append("\nUsage: /staffchat <message>").color(ChatColor.WHITE).create());
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (String arg : strings)
                sb.append(arg).append(" ");
            sendChatMessage(new ComponentBuilder(sb.toString()).color(color).create(), player);
        } else {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
        }
    }

    public static void sendMessage(String message){sendMessage(new ComponentBuilder(message).color(ChatColor.RED).create());}

    public static void sendMessage(BaseComponent message){sendMessage(new ComponentBuilder(message).create());}

    public static void sendChatMessage(BaseComponent[] message, ProxiedPlayer player){
        BaseComponent[] messagetosend;
        int staff = plugin.getStaff(player.getServer().getInfo()).size();
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(player.getServer().getInfo().getPlayers().size() + " players on this server!").color(ChatColor.RED)
                .append("\n" + staff + " Staff on this server!").color(ChatColor.RED).create());
        String userPrefix = Permissions.getPrefix(player.getUniqueId());
        messagetosend = new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', prefix.replace("%server%", player.getServer().getInfo().getName()).replace("%player%", userPrefix + " " + player.getName()))).event(hover).append(message).color(color).create();
        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
            if (all.hasPermission("punisher.staffchat")) {
                all.sendMessage(messagetosend);
            }
        }
    }

    public static void sendMessage(BaseComponent[] message){
        BaseComponent[] messagetosend = new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', prefixRaw)).append(message).color(color).create();
        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
            if (all.hasPermission("punisher.staffchat")) {
                all.sendMessage(messagetosend);
            }
        }
    }

    public static void sendMessages(BaseComponent... messages){
        for (BaseComponent message : messages) {
            sendMessage(message);
        }
    }
}