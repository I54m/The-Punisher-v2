package com.i54m.punisher.listeners;

import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.WorkerManager;
import com.i54m.punisher.utils.UUIDFetcher;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.UUID;

public class PrePlayerLogin implements Listener {

    private final ErrorHandler errorHandler = ErrorHandler.getINSTANCE();
    private final WorkerManager workerManager = WorkerManager.getINSTANCE();
    private final PunisherPlugin plugin = PunisherPlugin.getInstance();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PreLoginEvent event) {
        final String targetName = event.getConnection().getName();
        final PendingConnection connection = event.getConnection();
        workerManager.runWorker(new WorkerManager.Worker(() -> {
            UUIDFetcher uuidFetcher = new UUIDFetcher();
            uuidFetcher.fetch(targetName);
            try {
                UUID uuid = uuidFetcher.call();
                plugin.getStorageManager().loadUser(uuid);
            } catch (Exception pde) {
                errorHandler.log(pde);
                errorHandler.loginError(event);
            }
        }));
        workerManager.runWorker(new WorkerManager.Worker(() -> {
            UUIDFetcher uuidFetcher = new UUIDFetcher();
            uuidFetcher.fetch(targetName);
            try {
                UUID uuid = uuidFetcher.call();
                plugin.getStorageManager().updateAlts(uuid, connection.getSocketAddress().toString());
                plugin.getStorageManager().updateIpHist(uuid, connection.getSocketAddress().toString());
            } catch (Exception pde) {
                errorHandler.log(pde);
                errorHandler.loginError(event);
            }
        }));
    }
}
