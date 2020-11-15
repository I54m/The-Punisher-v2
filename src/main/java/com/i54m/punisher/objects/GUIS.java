package com.i54m.punisher.objects;

import com.i54m.protocol.inventory.InventoryType;
import com.i54m.protocol.items.ItemFlag;
import com.i54m.protocol.items.ItemStack;
import com.i54m.protocol.items.ItemType;
import com.i54m.punisher.PunisherPlugin;
import com.i54m.punisher.handlers.ErrorHandler;
import com.i54m.punisher.managers.PunishmentManager;
import lombok.AccessLevel;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GUIS {

    private static final PunisherPlugin PLUGIN = PunisherPlugin.getInstance();
    private static final PunishmentManager PUNISHMENT_MANAGER = PunishmentManager.getINSTANCE();
    private static final ErrorHandler ERROR_HANDLER = ErrorHandler.getINSTANCE();

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
        loadMenusFromFile();
    }

    public static void openMenu(GUI gui, final ProxiedPlayer punisher, UUID targetUUID, String targetName, String reputation) {
        if (gui == GUI.CONFIRMATION) return;
        final IconMenu MENU = gui.getMenu();
        MENU.setTitle(new ComponentBuilder("Punish: " + targetName + " ").color(ChatColor.RED).appendLegacy(reputation).create());
        MENU.setClick((clicker, menu, slot, item) -> {
            if (clicker == punisher) {
                String reason = getReason(gui, slot, item);
                if (reason != null) {
                    menu.close(clicker);
                    openConfirmationGUI(punisher, targetUUID, targetName, reason);
                    return true;
                }
            }
            return false;
        });
        MENU.open(punisher);
    }

    private static void openConfirmationGUI(final ProxiedPlayer punisher, UUID targetUUID, String targetName, String reason) {
        CONFIRMATION.setTitle(new ComponentBuilder("Confirm Punishing: " + targetName).color(ChatColor.RED).create());
        CONFIRMATION.setClick((clicker, menu1, slot, item) -> {
            if (clicker == punisher) {
                if (item == null || item.getDisplayName().isEmpty()) return false;
                if (item.getDisplayName().contains(guiConfig.getString("CONFIRMATION.items.confirm.name", "&a&lConfirm")) && CONFIRMSLOTS.contains(slot)) {
                    PunisherPlugin.getInstance().getLogger().info("Now punishing " + targetName);
                    try {
                        PUNISHMENT_MANAGER.issue(PUNISHMENT_MANAGER.constructPunishment(targetUUID, reason, punisher.getUniqueId()), punisher, true, true, true);
                    } catch (Exception e) {
                        ERROR_HANDLER.log(e);
                        ERROR_HANDLER.alert(e, punisher);
                    }
                    return true;
                } else if (item.getDisplayName().contains(guiConfig.getString("CONFIRMATION.items.deny.name", "&a&lDeny")) && DENYSLOTS.contains(slot)) {
                    clicker.sendMessage(new ComponentBuilder("Punishment Cancelled!").color(ChatColor.RED).create());
                    return true;
                }
            }
            return false;
        });
        List<String> lore = guiConfig.getStringList("CONFIRMATION.items.info.lore");
        new ArrayList<>(lore).forEach((i) -> {
            String line = i.replace("%targetname%", targetName).replace("%reason%", reason);
            lore.set(lore.indexOf(i), line);
        });
        CONFIRMATION.addButton(
                13,
                new ItemStack(ItemType.PAPER),
                guiConfig.getString("CONFIRMATION.items.info.name", "&c&lWARNING!!"),
                lore);
        CONFIRMATION.open(punisher);
    }

    private static void loadMenusFromFile() {// TODO: 15/11/2020 test that these menus load correctly
        for (GUI gui : GUI.values()) {
            if (gui == GUI.CONFIRMATION) {
                ItemStack deny = new ItemStack(ItemType.valueOf(guiConfig.getString("CONFIRMATION.items.deny.material", "RED_STAINED_GLASS_PANE")), 1);
                String denyName = guiConfig.getString("CONFIRMATION.items.deny.name", "&a&lDeny");// TODO: 15/11/2020 check color codes

                CONFIRMATION.addButton(0, deny, denyName);
                CONFIRMATION.addButton(1, deny, denyName);
                CONFIRMATION.addButton(2, deny, denyName);
                CONFIRMATION.addButton(9, deny, denyName);
                CONFIRMATION.addButton(10, deny, denyName);
                CONFIRMATION.addButton(11, deny, denyName);
                CONFIRMATION.addButton(18, deny, denyName);
                CONFIRMATION.addButton(19, deny, denyName);
                CONFIRMATION.addButton(20, deny, denyName);

                DENYSLOTS.add(0);
                DENYSLOTS.add(1);
                DENYSLOTS.add(2);
                DENYSLOTS.add(9);
                DENYSLOTS.add(10);
                DENYSLOTS.add(11);
                DENYSLOTS.add(18);
                DENYSLOTS.add(19);
                DENYSLOTS.add(20);

                ItemStack confirm = new ItemStack(ItemType.valueOf(guiConfig.getString("CONFIRMATION.items.confirm.material", "LIME_STAINED_GLASS_PANE")), 1);
                String confirmName = guiConfig.getString("CONFIRMATION.items.confirm.name", "&a&lConfirm");

                CONFIRMATION.addButton(6, confirm, confirmName);
                CONFIRMATION.addButton(7, confirm, confirmName);
                CONFIRMATION.addButton(8, confirm, confirmName);
                CONFIRMATION.addButton(15, confirm, confirmName);
                CONFIRMATION.addButton(16, confirm, confirmName);
                CONFIRMATION.addButton(17, confirm, confirmName);
                CONFIRMATION.addButton(24, confirm, confirmName);
                CONFIRMATION.addButton(25, confirm, confirmName);
                CONFIRMATION.addButton(26, confirm, confirmName);

                CONFIRMSLOTS.add(6);
                CONFIRMSLOTS.add(7);
                CONFIRMSLOTS.add(8);
                CONFIRMSLOTS.add(15);
                CONFIRMSLOTS.add(16);
                CONFIRMSLOTS.add(17);
                CONFIRMSLOTS.add(24);
                CONFIRMSLOTS.add(25);
                CONFIRMSLOTS.add(26);

                CONFIRMATION.setFill(new ItemStack(ItemType.valueOf(guiConfig.getString("CONFIRMATION.items.confirm.material", "GRAY_STAINED_GLASS_PANE")), 1));

            }
            IconMenu MENU = gui.getMenu();
            MENU.setType(InventoryType.getChestInventoryWithRows(guiConfig.getInt(gui.name() + ".rows")));
            Configuration section = guiConfig.getSection(gui.name() + ".items");
            for (String key : section.getKeys()) {
                if (key.equalsIgnoreCase("fill"))
                    MENU.setFill(new ItemStack(ItemType.valueOf(section.getString("fill.material")), 1));
                else {
                    ItemStack item = new ItemStack(ItemType.valueOf(section.getString(key + ".material")), 1);
                    List<String> flags = section.getStringList(key + ".flags");
                    if (!flags.isEmpty())
                        for (String flag : flags) {
                            item.setFlag(ItemFlag.valueOf(flag.toUpperCase()), true);
                        }
                    try {
                        MENU.addButton(
                                Integer.parseInt(key),
                                item,
                                section.getString(key + ".name"),
                                section.getStringList(key + ".lore"));
                    } catch (NumberFormatException nfe) {
                        ERROR_HANDLER.log(new YAMLException(key + " is not a valid integer for a slot number!", nfe));
                    }
                }
            }
        }
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

    private static String getReason(GUI gui, int slot, ItemStack item) {
        if (verifyItem(gui, slot, item))
            return guiConfig.getString(gui.name() + "." + slot + ".reason").toUpperCase();
        else return null;
    }

    private static boolean verifyItem(GUI gui, int slot, ItemStack item) {
        String itemType = guiConfig.getString(gui.name() + "." + slot + ".material").toUpperCase();
        String name = ChatColor.translateAlternateColorCodes('&', guiConfig.getString(gui.name() + "." + slot + ".name"));
        List<String> lore = guiConfig.getStringList(gui.name() + "." + slot + ".lore");
        //check item matches type, name and lore defined in config for the slot
        return itemType.equals(item.getType().toString()) && lore.equals(item.getLore()) && name.equals(item.getDisplayName());
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
