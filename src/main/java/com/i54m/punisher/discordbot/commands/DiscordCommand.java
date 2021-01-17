package com.i54m.punisher.discordbot.commands;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.discordbot.DiscordMain;
import com.i54m.punisher.exceptions.DataFecthException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.utils.NameFetcher;
import com.i54m.punisher.utils.UUIDFetcher;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DiscordCommand extends Command {

    public DiscordCommand() {
        super("discord", "punisher.discord", "disc");
    }

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();
    private UUID targetuuid;

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (strings.length > 0) {
                if (DiscordMain.jda == null) {
                    if (player.hasPermission("punisher.discord.admin")) {
                        if (strings.length > 1) {
                            if (!strings[0].equals("admin") && !strings[1].equals("enable")) {
                                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Discord integration is currently disabled, Do /discord admin enable to re-enable it!").color(ChatColor.RED).create());
                                return;
                            }
                        } else {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Discord integration is currently disabled, Do /discord admin enable to re-enable it!").color(ChatColor.RED).create());
                            return;
                        }
                    } else if (!player.hasPermission("punisher.discord.admin")) {
                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Discord integration is currently disabled!").color(ChatColor.RED).create());
                        return;
                    }
                }
                switch (strings[0].toLowerCase()) {
                    case "link":
                        if (!DiscordMain.verifiedUsers.containsKey(player.getUniqueId())) {
                            Random rand = new Random();
                            String charctersToUse = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
                            int length = 8;
                            char[] chars = new char[length];
                            for (int i = 0; i < length; i++) {
                                chars[i] = charctersToUse.charAt(rand.nextInt(charctersToUse.length()));
                            }
                            StringBuilder string = new StringBuilder();
                            for (char charcter : chars) {
                                string.append(charcter);
                            }
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Please private message " + DiscordMain.jda.getSelfUser().getName() + "#" + DiscordMain.jda.getSelfUser().getDiscriminator() + " Bot on discord with this code to complete the link.").color(ChatColor.GREEN)
                                    .append("\nCODE: ").bold(true).color(ChatColor.GREEN).append(string.toString()).color(ChatColor.GREEN).bold(false).create());
                            DiscordMain.userCodes.put(player.getUniqueId(), string.toString());
                        } else {
                            User user = DiscordMain.jda.getUserById(DiscordMain.verifiedUsers.get(player.getUniqueId()));
                            if (user != null) {
                                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You are already linked to discord user: " + user.getName() + "#" + user.getDiscriminator()).color(ChatColor.RED).create());
                                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("To unlink this discord user do \"/discord unlink\"").color(ChatColor.GREEN).create());
                            }
                        }
                        return;
                    case "unlink":
                        if (DiscordMain.verifiedUsers.containsKey(player.getUniqueId())) {
                            User user = DiscordMain.jda.getUserById(DiscordMain.verifiedUsers.get(player.getUniqueId()));
                            DiscordMain.verifiedUsers.remove(player.getUniqueId());
                            if (user != null) {
                                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Successfully unlinked from discord user: " + user.getName() + "#" + user.getDiscriminator()).color(ChatColor.RED).create());
                                Guild guild = DiscordMain.jda.getGuildById(plugin.getConfig().getString("DiscordIntegration.GuildId"));
                                if (guild != null) {
                                    Member member = guild.getMember(user);
                                    if (member != null) {
                                        for (String roleids : plugin.getConfig().getStringList("DiscordIntegration.RolesIdsToAddToLinkedUser")) {
                                            Role role = guild.getRoleById(roleids);
                                            if (role != null)
                                                guild.removeRoleFromMember(member, role).queue();
                                        }
                                        for (String roleids : plugin.getConfig().getStringList("DiscordIntegration.RolesToSync")) {
                                            Role role = guild.getRoleById(roleids);
                                            if (role != null)
                                                guild.removeRoleFromMember(member, role).queue();
                                        }
                                    }
                                }
                            }
                        } else {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You are not Linked to a discord user!").color(ChatColor.RED).create());
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("To link a discord user do \"/discord link\"").color(ChatColor.GREEN).create());
                        }
                        return;
                    case "linked":
                        if (player.hasPermission("punisher.discord.admin") && strings.length > 2) {
                            ProxiedPlayer findTarget = ProxyServer.getInstance().getPlayer(strings[0]);
                            Future<UUID> future = null;
                            ExecutorService executorService = null;
                            if (findTarget != null) {
                                targetuuid = findTarget.getUniqueId();
                            } else {
                                UUIDFetcher uuidFetcher = new UUIDFetcher();
                                uuidFetcher.fetch(strings[0]);
                                executorService = Executors.newSingleThreadExecutor();
                                future = executorService.submit(uuidFetcher);
                            }
                            if (future != null) {
                                try {
                                    targetuuid = future.get(1, TimeUnit.SECONDS);
                                } catch (Exception e) {
                                    try {
                                        throw new DataFecthException("UUID Required for next step", strings[0], "UUID", this.getName(), e);
                                    } catch (DataFecthException dfe) {
                                        ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                                        errorHandler.log(dfe);
                                        errorHandler.alert(dfe, commandSender);
                                    }
                                    executorService.shutdown();
                                    return;
                                }
                                executorService.shutdown();
                            }
                            if (targetuuid == null) {
                                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not a player's name!").color(ChatColor.RED).create());
                            } else {
                                String targetName = NameFetcher.getName(targetuuid);
                                if (DiscordMain.verifiedUsers.containsKey(targetuuid)) {
                                    User user = DiscordMain.jda.getUserById(DiscordMain.verifiedUsers.get(targetuuid));
                                    if (user != null)
                                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(targetName + "'s Minecraft account is currently linked to Discord user: " + user.getName() + "#" + user.getDiscriminator()).color(ChatColor.GREEN).create());
                                } else {
                                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(targetName + "'s Minecraft account is not yet linked to a Discord user").color(ChatColor.GREEN).create());
                                }
                            }
                        }
                        if (DiscordMain.verifiedUsers.containsKey(player.getUniqueId())) {
                            User user = DiscordMain.jda.getUserById(DiscordMain.verifiedUsers.get(player.getUniqueId()));
                            if (user != null)
                                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Your Minecraft account is currently linked to Discord user: " + user.getName() + "#" + user.getDiscriminator()).color(ChatColor.GREEN).create());
                        } else {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You are not Linked to a discord user!").color(ChatColor.RED).create());
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("To link a discord user do \"/discord link\"").color(ChatColor.GREEN).create());
                        }
                        return;
                    case "admin":
                        if (player.hasPermission("punisher.discord.admin")) {
                            if (strings.length > 1) {
                                switch (strings[1].toLowerCase()) {
                                    case "reboot":
                                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Rebooting Discord bot...").color(ChatColor.GREEN).create());
                                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Shutting down Discord bot...").color(ChatColor.GREEN).create());
                                        DiscordMain.shutdown();
                                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Shutting down complete!").color(ChatColor.GREEN).create());
                                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Booting Discord bot...").color(ChatColor.GREEN).create());
                                        DiscordMain.startBot();
                                        player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Successfully rebooted Discord bot!").color(ChatColor.GREEN).create());
                                        PunisherPlugin.getLOGS().warning(player.getName() + " Rebooted Discord Bot! (through /discord admin reboot)");
                                        return;
                                    case "disable":
                                        if (DiscordMain.jda != null) {
                                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Shutting down Discord bot...").color(ChatColor.GREEN).create());
                                            DiscordMain.shutdown();
                                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Shutting down complete!").color(ChatColor.GREEN).create());
                                            PunisherPlugin.getLOGS().warning(player.getName() + " Disabled/Shutdown Discord Bot! (through /discord admin disable)");
                                        } else {
                                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Discord bot is already offline, boot it up with /discord enable").color(ChatColor.RED).create());
                                        }
                                        return;
                                    case "enable":
                                        if (DiscordMain.jda == null) {
                                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Booting Discord bot...").color(ChatColor.GREEN).create());
                                            DiscordMain.startBot();
                                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Successfully booted Discord bot!").color(ChatColor.GREEN).create());
                                            PunisherPlugin.getLOGS().warning(player.getName() + " Enabled/Booted the Discord Bot! (through /discord admin enable)");
                                        } else {
                                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Discord bot is already online, shut it down with /discord disable").color(ChatColor.RED).create());
                                        }
                                        return;
                                    case "unlink":
                                        if (!(strings.length > 2)) {
                                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Please provide a player's name!").color(ChatColor.RED).create());
                                            return;
                                        }
                                        ProxiedPlayer findTarget = ProxyServer.getInstance().getPlayer(strings[0]);
                                        Future<UUID> future = null;
                                        ExecutorService executorService = null;
                                        if (findTarget != null) {
                                            targetuuid = findTarget.getUniqueId();
                                        } else {
                                            UUIDFetcher uuidFetcher = new UUIDFetcher();
                                            uuidFetcher.fetch(strings[2]);
                                            executorService = Executors.newSingleThreadExecutor();
                                            future = executorService.submit(uuidFetcher);
                                        }
                                        if (future != null) {
                                            try {
                                                targetuuid = future.get(1, TimeUnit.SECONDS);
                                            } catch (Exception e) {
                                                try {
                                                    throw new DataFecthException("UUID Required for next step", strings[0], "UUID", this.getName(), e);
                                                } catch (DataFecthException dfe) {
                                                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                                                    errorHandler.log(dfe);
                                                    errorHandler.alert(dfe, commandSender);
                                                }
                                                executorService.shutdown();
                                                return;
                                            }
                                            executorService.shutdown();
                                        }
                                        if (targetuuid == null) {
                                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("That is not a player's name!").color(ChatColor.RED).create());
                                        } else {
                                            String targetName = NameFetcher.getName(targetuuid);
                                            if (DiscordMain.verifiedUsers.containsKey(player.getUniqueId())) {
                                                User user = DiscordMain.jda.getUserById(DiscordMain.verifiedUsers.get(player.getUniqueId()));
                                                DiscordMain.verifiedUsers.remove(player.getUniqueId());
                                                if (user != null) {
                                                    player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Successfully unlinked: " + targetName + " from discord user: " + user.getName() + "#" + user.getDiscriminator()).color(ChatColor.RED).create());
                                                    Guild guild = DiscordMain.jda.getGuildById(plugin.getConfig().getString("DiscordIntegration.GuildId"));
                                                    if (guild != null) {
                                                        Member member = guild.getMember(user);
                                                        if (member != null) {
                                                            for (String roleids : plugin.getConfig().getStringList("DiscordIntegration.RolesIdsToAddToLinkedUser")) {
                                                                Role role = guild.getRoleById(roleids);
                                                                if (role != null)
                                                                    guild.removeRoleFromMember(member, role).queue();
                                                            }
                                                            for (String roleids : plugin.getConfig().getStringList("DiscordIntegration.RolesToSync")) {
                                                                Role role = guild.getRoleById(roleids);
                                                                if (role != null)
                                                                    guild.removeRoleFromMember(member, role).queue();
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append(targetName + " is not yet Linked to a discord user!").color(ChatColor.RED).create());
                                            }
                                        }
                                        return;
                                }
                            } else {
                                player.sendMessage(new ComponentBuilder("|-----").color(ChatColor.AQUA).strikethrough(true).append(" Discord Integration Admin Commands").color(ChatColor.BLUE).strikethrough(false).bold(true).append(" -----|").color(ChatColor.AQUA).bold(false).strikethrough(true).create());
                                player.sendMessage(new ComponentBuilder("/discord admin reboot").color(ChatColor.AQUA).append(" - reboot the Discord bot").color(ChatColor.WHITE).create());
                                player.sendMessage(new ComponentBuilder("/discord admin disable").color(ChatColor.AQUA).append(" - disable the Discord bot and related commands").color(ChatColor.WHITE).create());
                                player.sendMessage(new ComponentBuilder("/discord admin enable").color(ChatColor.AQUA).append(" - enable the Discord bot and related commands").color(ChatColor.WHITE).create());
                                player.sendMessage(new ComponentBuilder("/discord admin unlink").color(ChatColor.AQUA).append(" - force unlink a user's discord and minecraft").color(ChatColor.WHITE).create());
                                player.sendMessage(new ComponentBuilder("/discord admin help").color(ChatColor.AQUA).append(" - view this help menu").color(ChatColor.WHITE).create());
                                return;
                            }
                        } else {
                            player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("You do not have permission to do that!").color(ChatColor.RED).create());
                            return;
                        }
                }
            }
            if (DiscordMain.jda != null) {
                player.sendMessage(new ComponentBuilder("|-----").color(ChatColor.AQUA).strikethrough(true).append(" Discord Integration Commands").color(ChatColor.BLUE).strikethrough(false).bold(true).append(" -----|").color(ChatColor.AQUA).bold(false).strikethrough(true).create());
                player.sendMessage(new ComponentBuilder("/discord link").color(ChatColor.AQUA).append(" - link your minecraft account to your discord").color(ChatColor.WHITE).create());
                player.sendMessage(new ComponentBuilder("/discord unlink").color(ChatColor.AQUA).append(" - unlink your minecraft and discord accounts").color(ChatColor.WHITE).create());
                player.sendMessage(new ComponentBuilder("/discord linked").color(ChatColor.AQUA).append(" - view the currently linked discord account").color(ChatColor.WHITE).create());
                player.sendMessage(new ComponentBuilder("/discord help").color(ChatColor.AQUA).append(" - view this help menu").color(ChatColor.WHITE).create());
            } else if (!player.hasPermission("punisher.admin")) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Discord integration is currently disabled!").color(ChatColor.RED).create());
            } else if (player.hasPermission("punisher.admin")) {
                player.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Discord integration is currently disabled, Do /discord admin enable to re-enable it!").color(ChatColor.RED).create());
            }
        } else {
            commandSender.sendMessage(new TextComponent("You must be a player to use this command!"));
        }
    }
}
