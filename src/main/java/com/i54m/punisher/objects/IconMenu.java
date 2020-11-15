package com.i54m.punisher.objects;

import com.i54m.protocol.inventory.Inventory;
import com.i54m.protocol.inventory.InventoryModule;
import com.i54m.protocol.inventory.InventoryType;
import com.i54m.protocol.inventory.event.InventoryClickEvent;
import com.i54m.protocol.items.ItemStack;
import com.i54m.punisher.PunisherPlugin;
import lombok.Setter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.*;


public class IconMenu implements Listener {

    @Setter
    private BaseComponent[] title;
    @Setter
    private InventoryType type;
    @Setter
    private onClick click;
    private final Map<Integer, ItemStack> items = new HashMap<>();
    private final List<UUID> viewing = new ArrayList<>();
    private final Inventory menu;

    public IconMenu(InventoryType type) {
        this.menu = new Inventory(type, new TextComponent("Icon Menu"));
        this.title = new ComponentBuilder("Icon Menu").create();
        this.type = type;
        ProxyServer.getInstance().getPluginManager().registerListener(PunisherPlugin.getInstance(), this);
    }

    public IconMenu(InventoryType type, BaseComponent[] title) {
        this.menu = new Inventory(type, title);
        this.type = type;
        this.title = title;
        ProxyServer.getInstance().getPluginManager().registerListener(PunisherPlugin.getInstance(), this);
    }

    public void open(ProxiedPlayer p) {
        menu.setTitle(title);
        if (!menu.getType().equals(this.type)) menu.setType(this.type);

        if (this.type != null && !items.isEmpty()) {
            if (this.title == null) this.title = new TextComponent[]{new TextComponent("Icon Menu")};

            this.menu.setItems(items);
            InventoryModule.sendInventory(p, menu);
            viewing.add(p.getUniqueId());
        } else {
            PunisherPlugin.getInstance().getLogger().severe("Error: Cannot open an IconMenu that is missing a type or items!");
        }
    }

    public void close(ProxiedPlayer p) {
        InventoryModule.closeInventory(menu, p);
        viewing.remove(p.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (viewing.contains(event.getPlayer().getUniqueId())) {
            ProxiedPlayer p = event.getPlayer();
            if (event.getInventory() != null && event.getClickedItem() != null && event.getInventory().equals(menu)) {
                event.setCancelled(true);
                if (click.click(p, this, event.getSlot(), event.getClickedItem()) && viewing.contains(p.getUniqueId()))
                    close(p);
            }
        }
    }

    public void addButton(int position, ItemStack item, String name, String... lore) {
        items.put(position, getItem(item, name, lore));
    }

    private ItemStack getItem(ItemStack item, String name, String... lore) {
        item.setDisplayName(name);
        item.setLore(Arrays.asList(lore));
        return item;
    }

    public interface onClick {
        boolean click(ProxiedPlayer clicker, IconMenu menu, int slot, ItemStack item);
    }
}