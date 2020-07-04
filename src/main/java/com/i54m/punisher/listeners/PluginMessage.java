package com.i54m.punisher.listeners;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.chats.StaffChat;
import com.i54m.punisher.exceptions.PunishmentCalculationException;
import com.i54m.punisher.exceptions.PunishmentIssueException;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.PunishmentManager;
import com.i54m.punisher.managers.ReputationManager;
import com.i54m.punisher.managers.WorkerManager;
import com.i54m.punisher.objects.Punishment;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;

public class PluginMessage implements Listener {

    private final PunishmentManager punishmngr = PunishmentManager.getINSTANCE();
    private final WorkerManager workerManager = WorkerManager.getINSTANCE();
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    private boolean allowGui = true;
    static ArrayList<ServerInfo> chatOffServers = new ArrayList<>();

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (e.getTag().equalsIgnoreCase("punisher:main")) {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));
            try {
                Connection sender = e.getReceiver();
                String chatState, action, uuid, name;
                action = in.readUTF();
                switch (action) {
                    case "chatToggled": {
                        chatState = in.readUTF();
                        ProxiedPlayer toggler = (ProxiedPlayer) sender;
                        if (chatState.equalsIgnoreCase("on")) {
                            StaffChat.sendMessage(toggler.getName() + " Has toggled chat: On!", true);
                            ServerInfo server = toggler.getServer().getInfo();
                            chatOffServers.remove(server);
                        } else {
                            StaffChat.sendMessage(toggler.getName() + " Has toggled chat: Off!", true);
                            ServerInfo server = toggler.getServer().getInfo();
                            chatOffServers.add(server);
                        }
                        return;
                    }
                    case "getrep": {
                        uuid = in.readUTF();
                        String rep = ReputationManager.getRep(uuid);
                        if (rep == null) {
                            rep = "5.0";
                        }
                        name = in.readUTF();
                        ProxiedPlayer player = (ProxiedPlayer) sender;
                        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
                        DataOutputStream out = new DataOutputStream(bytestream);
                        out.writeUTF("rep");
                        out.writeUTF(rep);
                        out.writeUTF(uuid);
                        out.writeUTF(name);
                        player.getServer().sendData("punisher:main", bytestream.toByteArray());
                        return;
                    }
                    case "punish": {
                        ProxiedPlayer punisher = (ProxiedPlayer) sender;
                        if (!allowGui){
                            punisher.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("The Punisher Gui is currently not functioning correctly please inform an admin+ asap!!").color(ChatColor.RED).create());
                            return;
                        }
                        uuid = in.readUTF().replace("-", "");
                        name = in.readUTF();
                        int slot = Integer.parseInt(in.readUTF());
                        String item = in.readUTF();
                        try {
                            String[] properties = punishmngr.translate(slot, item);
                            Punishment.Type type;
                            Punishment.Reason reason;
                            String message;
                            long expiration = 0;
                            if (properties[0].equals("Custom")) {
                                reason = Punishment.Reason.Custom;
                                type = Punishment.Type.valueOf(properties[1]);
                                message = properties[2];
                                expiration = Long.parseLong(properties[3]);
                            } else if (properties[0].contains("Other")) {
                                reason = Punishment.Reason.valueOf(properties[0]);
                                type = Punishment.Type.BAN;
                                message = properties[1];
                            } else {
                                reason = Punishment.Reason.valueOf(properties[0]);
                                type = punishmngr.calculateType(uuid, reason);
                                message = properties[1];
                            }
                            final Punishment punishment = new Punishment(type, reason, expiration > 0 ? expiration : null, uuid, name, punisher.getUniqueId().toString().replace("-", ""), message);
                            workerManager.runWorker(new WorkerManager.Worker(() ->{
                                try {
                                    punishmngr.issue(punishment, punisher, true, true, true);
                                } catch (SQLException sqle) {
                                    try {
                                        throw new PunishmentIssueException("Automatic punishment was unable to issue due to an sql exception", punishment, sqle);
                                    } catch (PunishmentIssueException pie) {
                                        ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                                        errorHandler.log(pie);
                                        errorHandler.alert(pie, punisher);
                                    }
                                    allowGui = false;
                                }
                            }));
                            punisher.sendMessage(new ComponentBuilder(plugin.getPrefix()).append("Punishing " + name + " for: " + message + "...").color(ChatColor.GREEN).create());
                        } catch (SQLException sqle) {
                            try {
                                throw new PunishmentCalculationException("Automatic punishment was unable to issue due to an sql exception", "Initialize required feilds for punishment", sqle);
                            } catch (PunishmentCalculationException pce) {
                                ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                                errorHandler.log(pce);
                                errorHandler.alert(pce, punisher);
                            }
                            allowGui = false;
                        } catch (PunishmentCalculationException pce) {
                            ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                            errorHandler.log(pce);
                            errorHandler.alert(pce, punisher);
                        }
                    }
                }
            } catch (IOException IO) {
                try {
                    throw new PunishmentCalculationException("Automatic punishment was unable to issue due to an IO exception, (the end of the plugin message was reached unexpectedly)", "Punishment from gui plugin message", IO);
                } catch (PunishmentCalculationException pce) {
                    ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
                    errorHandler.log(pce);
                    errorHandler.alert(pce, (ProxiedPlayer) e.getReceiver());
                }
                allowGui = false;
            }
        } else if (e.getTag().equals("punisher:minor")) {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));
            try {
                String action, uuid;
                action = in.readUTF();
                if (action.equals("getrepcache")) {
                    uuid = in.readUTF();
                    String rep = ReputationManager.getRep(uuid);
                    if (rep == null) {
                        rep = "5.0";
                    }
                    ProxiedPlayer player = (ProxiedPlayer) e.getReceiver();
                    ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(bytestream);
                    out.writeUTF("repcache");
                    out.writeUTF(rep);
                    out.writeUTF(uuid);
                    player.getServer().sendData("punisher:minor", bytestream.toByteArray());
                } else if (action.equals("log")) {
                    Level level = Level.parse(in.readUTF());
                    PunisherPlugin.getLOGS().log(level, in.readUTF());
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
