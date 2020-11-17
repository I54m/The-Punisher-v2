package com.i54m.punisher.utils;

import com.i54m.punisher.PunisherPlugin;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class UserFetcher implements Callable<User> {
    private UUID uuid;

    public void setUuid(UUID uuid){
        this.uuid = uuid;
    }

    @Override
    public User call() {
        UserManager userManager = PunisherPlugin.getInstance().getLuckPermsHook().getApi().getUserManager();
        CompletableFuture<User> userFuture = userManager.loadUser(uuid);
        return userFuture.join();
    }
}
