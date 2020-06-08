package com.i54mpenguin.punisher.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

public class LuckPermsHook {

    public LuckPerms LuckPermsInstance;

    public LuckPermsHook hook() {
        if (LuckPermsInstance != null) return this;
        try { // TODO: 15/05/2020 luckperms not hooking correctly :/
            LuckPermsInstance = LuckPermsProvider.get();
        } catch (Exception e) {
            return null;
        }
        return this;
    }

    public LuckPerms getApi() {return LuckPermsInstance;}

}
