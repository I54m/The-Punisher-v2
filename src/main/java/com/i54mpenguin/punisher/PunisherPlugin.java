package com.i54mpenguin.punisher;

import com.i54mpenguin.protocol.api.injector.NettyPipelineInjector;
import com.i54mpenguin.protocol.api.listener.PlayerListener;
import com.i54mpenguin.protocol.inventory.InventoryModule;
import com.i54mpenguin.protocol.items.ItemsModule;
import com.i54mpenguin.punisher.commands.PunishCommand;
import com.i54mpenguin.punisher.objects.gui.ConfirmationGUI;
import com.i54mpenguin.punisher.objects.gui.punishgui.LevelOne;
import com.i54mpenguin.punisher.objects.gui.punishgui.LevelThree;
import com.i54mpenguin.punisher.objects.gui.punishgui.LevelTwo;
import com.i54mpenguin.punisher.objects.gui.punishgui.LevelZero;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

public class PunisherPlugin extends Plugin {

    @Setter
    @Getter
    public static PunisherPlugin instance;
    @Getter
    private final NettyPipelineInjector nettyPipelineInjector = new NettyPipelineInjector();
    @Setter
    @Getter
    private static boolean isLoaded = false;

    @Override
    public void onEnable() {
        setInstance(this);
        ProxyServer.getInstance().getPluginManager().registerListener(this, new PlayerListener(this));
        ItemsModule.initModule();
        InventoryModule.initModule();
        getProxy().getPluginManager().registerCommand(this, new PunishCommand());
        LevelZero.setupMenu();
        LevelOne.setupMenu();
        LevelTwo.setupMenu();
        LevelThree.setupMenu();
        ConfirmationGUI.setupMenu();


        PunisherPlugin.getInstance().getLogger().info("[Protocol] This is ProxyServer.getInstance().getLogger().info(\"\");");
        PunisherPlugin.getInstance().getLogger().info("This is getLogger().info(\"\");");

        setLoaded(true);
    }

}
