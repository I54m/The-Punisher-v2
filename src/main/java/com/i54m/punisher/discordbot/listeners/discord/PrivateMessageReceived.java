package com.i54m.punisher.discordbot.listeners.discord;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.discordbot.DiscordMain;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.utils.NameFetcher;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PrivateMessageReceived extends ListenerAdapter {

    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        User user = event.getAuthor();
        if (user.isFake() || user.isBot()) return;
        if (DiscordMain.verifiedUsers.containsValue(user.getId())) {
            event.getChannel().sendMessage("You are already a verified user, to unlink your discord from your minecraft type \"/discord unlink\" in game.").queue();
            return;
        }
        String message = event.getMessage().getContentRaw();
        if (DiscordMain.userCodes.containsValue(message.toUpperCase())) {
            UUID uuid = getMinecraftUUID(message);
            linkAccounts(user, event.getChannel(), uuid);
            DiscordMain.userCodes.remove(uuid, message);
        } else {
            event.getChannel().sendMessage("That is not a valid verification code, Please check that the code is the same as the one given to you!").queue();
        }
    }

    private UUID getMinecraftUUID(String code) {
        Map<UUID, String> usercodes = new HashMap<>(DiscordMain.userCodes);
        for (Map.Entry<UUID, String> entry : usercodes.entrySet()) {
            if (entry.getValue().equals(code.toUpperCase())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void linkAccounts(User user, PrivateChannel channel, UUID uuid) {
        Guild guild = DiscordMain.jda.getGuildById(plugin.getConfig().getString("DiscordIntegration.GuildId"));
        if (guild != null) {
            Member member = guild.getMember(user);
            if (member != null) {
                for (String roleids : plugin.getConfig().getStringList("DiscordIntegration.RolesIdsToAddToLinkedUser")) {
                    Role role = guild.getRoleById(roleids);
                    if (role != null)
                        guild.addRoleToMember(member, role).queue();
                    else {
                        ErrorHandler.getINSTANCE().log(new NullPointerException("Could not find role from the provided id: " + roleids));
                        channel.sendMessage("An Error occurred while trying to link your accounts, Please try again later!").queue();
                        return;
                    }
                }

                channel.sendMessage("Linked: `" + user.getName() + "#" + user.getDiscriminator() + "` to minecraft user: " + NameFetcher.getName(uuid.toString().replace("-", "")) + "!" +
                        "\nTo unlink this minecraft account type \"/discord unlink\" in game").queue();
                if (plugin.getConfig().getBoolean("DiscordIntegration.EnableRoleSync"))
                    channel.sendMessage("Login to the server to get your ranks synced over to the discord server").queue();
                DiscordMain.verifiedUsers.put(uuid, user.getId());
            } else {
                channel.sendMessage("An Error occurred while trying to link your accounts! It appears you are not yet joined our discord server!").queue();
            }
        } else {
            ErrorHandler.getINSTANCE().log(new NullPointerException("Could not find guild from the provided id!"));
            channel.sendMessage("An Error occurred while trying to link your accounts, Please try again later!").queue();
        }
    }
}
