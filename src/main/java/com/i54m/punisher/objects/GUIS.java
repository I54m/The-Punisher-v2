package com.i54m.punisher.objects;

import com.i54m.protocol.inventory.InventoryType;
import com.i54m.protocol.items.ItemStack;
import com.i54m.protocol.items.ItemType;
import com.i54m.punisher.PunisherPlugin;
import lombok.AccessLevel;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.UUID;

public class GUIS {

    private static final PunisherPlugin PLUGIN = PunisherPlugin.getInstance();
    private static final IconMenu LEVEL_ZERO = new IconMenu(InventoryType.GENERIC_9X1);
    private static final IconMenu LEVEL_ONE = new IconMenu(InventoryType.GENERIC_9X1);
    private static final IconMenu LEVEL_TWO = new IconMenu(InventoryType.GENERIC_9X1);
    private static final IconMenu LEVEL_THREE = new IconMenu(InventoryType.GENERIC_9X1);

    private static final IconMenu CONFIRMATION = new IconMenu(InventoryType.GENERIC_9X1);
    private static final ArrayList<Integer> DENYSLOTS = new ArrayList<>();
    private static final ArrayList<Integer> CONFIRMSLOTS = new ArrayList<>();


    private static Configuration guiConfig;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void preConstructGUIs() throws IOException {
        File guiFile = new File(PLUGIN.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            guiFile.createNewFile();
            saveDefaultConfig();
        }
        guiConfig = YamlConfiguration.getProvider(YamlConfiguration.class).load(guiFile);
        loadPunishMenusFromFile();
        loadConfirmationMenuFromFile();
    }

    public static void openMenu(GUI gui, final ProxiedPlayer punisher, UUID targetUUID, String targetName, String reputation) {
        if (gui == GUI.CONFIRMATION) return;
        final IconMenu MENU = gui.getMenu();
        MENU.setTitle(new ComponentBuilder("Punish: " + targetName + " ").color(ChatColor.RED).appendLegacy(reputation).create());
        MENU.setClick((player, menu, slot, item) -> {
            // TODO: 11/11/2020 need to rewrite this to fetch the reason from the config
            if (item.getLore() != null && !item.getLore().isEmpty()) {
                menu.close(player);
                openConfirmationGUI(punisher, targetUUID, targetName, item, slot);
                return true;
            }
            return false;
        });
        MENU.open(punisher);
    }

    private static void openConfirmationGUI(final ProxiedPlayer punisher, UUID targetUUID, String targetName, ItemStack chosenItem, int chosenItemSlot){
        CONFIRMATION.setTitle(new ComponentBuilder("Confirm Punishing: " + targetName).color(ChatColor.RED).create());
        CONFIRMATION.setClick((clicker, menu1, slot, item) -> {
            if (clicker == punisher) {
                if (item == null || item.getDisplayName().isEmpty()) return false;
                if (item.getDisplayName().contains(ChatColor.GREEN + "Confirm") && CONFIRMSLOTS.contains(slot)) {
                    PunisherPlugin.getInstance().getLogger().info("Now punishing " + targetName);// TODO: 11/11/2020 write punishment construction and issuing after confirmation
                    //PunishGUI.punishmentSelected(targetuuid, targetName, chosenItemSlot, chosenItem, clicker);
                    return true;
                } else if (item.getDisplayName().contains(ChatColor.RED + "Deny") && DENYSLOTS.contains(slot)) {
                    clicker.sendMessage(new ComponentBuilder("Punishment Cancelled!").color(ChatColor.RED).create());
                    return true;
                }
            }
            return false;
        });
        CONFIRMATION.addButton(13, new ItemStack(ItemType.PAPER), ChatColor.RED + "" + ChatColor.BOLD + "WARNING!!!",
                ChatColor.RED + "Please confirm the following punishment:",
                ChatColor.RED + "Punishing: " + ChatColor.WHITE + targetName,
                ChatColor.RED + "Reason: " + ChatColor.WHITE + ChatColor.stripColor(chosenItem.getDisplayName()),
                " ",
                ChatColor.RED + "" + ChatColor.BOLD + "Think before you punish!"); // TODO: 11/11/2020 rewrite this so that it fetches the values from the config
        CONFIRMATION.open(punisher);
    }

    private static void loadPunishMenusFromFile() {// TODO: 11/11/2020 write this

    }

    private static void loadConfirmationMenuFromFile() { // TODO: 11/11/2020 write this

    }

    private static void saveGUIFile() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(guiConfig, new File(PLUGIN.getDataFolder(), "gui.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveDefaultConfig() {
        try {
            Files.copy(PLUGIN.getResourceAsStream("gui.yml"), new File(PLUGIN.getDataFolder(), "gui.yml").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create configuration file", e);
        }
    }

    public enum GUI {

        LEVEL_ZERO(GUIS.LEVEL_ZERO), LEVEL_ONE(GUIS.LEVEL_ONE), LEVEL_TWO(GUIS.LEVEL_TWO), LEVEL_THREE(GUIS.LEVEL_THREE), CONFIRMATION(GUIS.CONFIRMATION);

        @Getter(AccessLevel.PRIVATE)
        private final IconMenu menu;

        GUI(IconMenu menu) {
            this.menu = menu;
        }
    }
}
