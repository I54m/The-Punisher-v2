package com.i54mpenguin.punisher.commands;

import me.fiftyfour.punisher.bungee.PunisherPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class SuperBroadcast extends Command {
    public SuperBroadcast() {
        super("superbroadcast", "punisher.superbroadcast", "sb", "alert");
    }

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    public static String prefix;
    public static ChatColor color;
    public static Title title;

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) commandSender;
        if (strings.length == 0) {
            player.sendMessage(new ComponentBuilder(plugin.prefix).append("Send a message to all servers").color(ChatColor.RED).append("\nUsage: /superbroadcast <message>").color(ChatColor.WHITE).create());
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String message : strings)
            sb.append(message).append(" ");
        ProxyServer.getInstance().broadcast(new TextComponent("\n"));
        ProxyServer.getInstance().broadcast(new ComponentBuilder(prefix.replace("%server%", player.getServer().getInfo().getName()).replace("%player%", player.getName())).append(sb.toString()).color(color).create());
        ProxyServer.getInstance().broadcast(new TextComponent("\n"));
        if (title == null){
            if (!PunisherPlugin.config.getString("superbroadcast-title").equalsIgnoreCase("none")) {
                String[] titleargs = PunisherPlugin.config.getString("superbroadcast-title").split(":");
                try {
                    SuperBroadcast.title = ProxyServer.getInstance().createTitle().subTitle(new ComponentBuilder(titleargs[0]).create())
                            .fadeIn(Integer.parseInt(titleargs[1])).stay(Integer.parseInt(titleargs[2])).fadeOut(Integer.parseInt(titleargs[3]));
                } catch (NumberFormatException nfe){
                    throw new NumberFormatException("One of your superbroadcast-title numbers is not a number! (fade in, stay or fade out");
                } catch (IndexOutOfBoundsException ioobe){
                    throw new IndexOutOfBoundsException("You have not supplied enough arguments in the superbroadcast-title!");
                }
            }
        }
        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) { // TODO: 15/05/2020 no title sent?
            title.send(all);
        }
    }
}