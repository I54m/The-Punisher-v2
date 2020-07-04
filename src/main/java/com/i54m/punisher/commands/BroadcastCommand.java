package com.i54m.punisher.commands;

import com.i54m.punisher.PunisherPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class BroadcastCommand extends Command {
    public BroadcastCommand() {
        super("broadcast", "punisher.broadcast", "global", "shout");
    }

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    public static String prefix;
    public static ChatColor color;

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) commandSender;
//        if (PunisherPlugin.cooldownsConfig.contains(player.getUniqueId().toString()) ){
//            long cooldowntime = PunisherPlugin.cooldownsConfig.getLong(player.getUniqueId().toString());
//            if (cooldowntime > System.currentTimeMillis() && !player.hasPermission("punisher.cooldowns.override")) {
//                long cooldownleftmillis = cooldowntime - System.currentTimeMillis();
//                int hoursleft = (int) (cooldownleftmillis / (1000 * 60 * 60));
//                int minutesleft = (int) (cooldownleftmillis / (1000 * 60) % 60);
//                int secondsleft = (int) (cooldownleftmillis / 1000 % 60);
//                if(hoursleft > 0){
//                    player.sendMessage(new ComponentBuilder("You have done that recently, please wait: " + hoursleft + "h " + minutesleft + "m " + secondsleft + "s before doing this again!").color(ChatColor.RED).create());
//                }else{
//                    if(minutesleft > 0) {
//                        player.sendMessage(new ComponentBuilder("You have done that recently, please wait: " + minutesleft + "m " + secondsleft + "s before doing this again!").color(ChatColor.RED).create());
//                    }else{
//                        player.sendMessage(new ComponentBuilder("You have done that recently, please wait: " + secondsleft + "s before doing this again!").color(ChatColor.RED).create());
//                    }
//                }
//            }else{
//                if (strings.length == 0) {
//                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Similar to Super Broadcast but not as powerful").color(ChatColor.RED).append("\nUsage: /broadcast <message>").color(ChatColor.WHITE).create());
//                    return;
//                }
//                StringBuilder sb = new StringBuilder();
//                for (String message : strings)
//                    sb.append(message).append(" ");
//                ProxyServer.getInstance().broadcast(new ComponentBuilder(prefix.replace("%server%", player.getServer().getInfo().getName()).replace("%player%", player.getName())).append(sb.toString()).color(color).create());
//                long setcooldowntime = plugin.getConfig().getLong("broadcast-cooldown");
//                setcooldowntime = setcooldowntime * 60 * 60 * 1000;
//                setcooldowntime += System.currentTimeMillis();
//                PunisherPlugin.cooldownsConfig.set(player.getUniqueId().toString(), setcooldowntime);
//                PunisherPlugin.saveCooldowns();
//            }
//            return;
//        }
        if (strings.length == 0) {
            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Similar to Super Broadcast but not as powerful").color(ChatColor.RED).append("\nUsage: /global <message>").color(ChatColor.WHITE).create());
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String message : strings)
            sb.append(message).append(" ");
        ProxyServer.getInstance().broadcast(new ComponentBuilder(prefix.replace("%server%", player.getServer().getInfo().getName()).replace("%player%", player.getName())).append(sb.toString()).color(color).create());
        long setcooldowntime = plugin.getConfig().getLong("broadcast-cooldown");
        setcooldowntime = setcooldowntime * 60 * 60 * 1000;
        setcooldowntime += System.currentTimeMillis();
//        PunisherPlugin.cooldownsConfig.set(player.getUniqueId().toString(), setcooldowntime);
//        PunisherPlugin.saveCooldowns();
    }
}
