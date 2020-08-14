package com.i54m.punisher.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

public class LuckPermsHook {

    public LuckPerms LuckPermsInstance;

    public LuckPermsHook hook() {
        if (LuckPermsInstance != null) return this;
        try {
            LuckPermsInstance = LuckPermsProvider.get();
        } catch (Exception e) {
            return null;
        }
        return this;
    }

    public LuckPerms getApi() {return LuckPermsInstance;}

}
