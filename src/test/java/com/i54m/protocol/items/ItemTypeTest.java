package com.i54m.protocol.items;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.i54m.protocol.api.util.ProtocolVersions;
import lombok.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ItemTypeTest {

    @Test
    void checkLatestVersionID() {
        boolean errors = false;
        for (ItemType itemType : ItemType.values()) {
            if (itemType.getApplicableMappingNoFallback(ProtocolVersions.MINECRAFT_LATEST) == null) {
                //missing id for latest version!
                System.out.println("[ItemType Testing] [ERROR] Detected Missing id in latest version for item: " + itemType);
                errors = true;
            }
        }
        assertFalse(errors);
    }

    @Test
    void checkForMultipleIDs() {
        boolean errors = false;
        for (ItemType itemType : ItemType.values()) {
            Map<Integer, ArrayList<ItemIDMapping>> versionsWithID = new TreeMap<>();
            for (String protocolVersion : ProtocolVersions.getSupportedMajorVersions().keySet()) {
                int protocolId = ProtocolVersions.getSupportedMajorVersions().get(protocolVersion);
                for (ItemIDMapping mapping : itemType.getItemMapping()) {
                    if (mapping.getProtocolVersionRangeStart() <= protocolId && mapping.getProtocolVersionRangeEnd() >= protocolId) {
                        ArrayList<ItemIDMapping> mappings = versionsWithID.containsKey(protocolId) ? versionsWithID.get(protocolId) : new ArrayList<>();
                        mappings.add(mapping);
                        versionsWithID.put(protocolId, mappings);
                    }
                }
                if (versionsWithID.containsKey(protocolId)) {
                    if (versionsWithID.get(protocolId).size() > 1) {
                        //Item has multiple ids for the same version
                        ArrayList<Integer> ids = new ArrayList<>();
                        versionsWithID.get(protocolId).forEach((idMapping) -> ids.add(idMapping.getId()));
                        System.out.println("[ItemType Testing] [ERROR] Detected Multiple ids on protocol version: " + protocolVersion + " for item: " + itemType + ", ids: " + ids.toString());
                        errors = true;
                    }
                }
            }
        }
        assertFalse(errors);
    }

    @Test
    void checkForDuplicateID() {
        boolean errors = false;
        for (int protocolVersion : ProtocolVersions.getSupportedVersions()) {
            for (ItemType itemType : ItemType.values()) {
                for (ItemType itemType2 : ItemType.values()) {
                    if (itemType2 == itemType) continue;
                    if (itemType.getApplicableMappingNoFallback(protocolVersion) != null && itemType2.getApplicableMappingNoFallback(protocolVersion) != null)
                        if (itemType.getApplicableMappingNoFallback(protocolVersion) == itemType2.getApplicableMappingNoFallback(protocolVersion)) {
                            //duplicate item detected!
                            System.out.println("[ItemType Testing] [ERROR] Duplicate id detected for items: " + itemType + " and " + itemType2 + " on protocol version: " + protocolVersion);
                            errors = true;
                        }
                }
            }
        }
        assertFalse(errors);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Test
    void checkForCorrectID() {
        //set this to true to show warnings as a warning is sent if the item is not defined in a previous version of minecraft's item list and it can be quite spammy
        boolean showWarnings = false;
        boolean errors = false;
        boolean warnings = false;
        Map<String, Integer> supportedMajorVersions = new HashMap<>(ProtocolVersions.getSupportedMajorVersions());
        for (Map.Entry<String, Integer> protocolVersion : ProtocolVersions.getSupportedMajorVersions().entrySet()) {
            //remove all versions lower than 1.12 as the ids don't change with a new version but 1.12 has the most
            //items added before the flattening (1.13) in which item ids are no longer as stable between versions
            if (protocolVersion.getValue() < ProtocolVersions.MINECRAFT_1_12)
                supportedMajorVersions.remove(protocolVersion.getKey());
        }
        for (String protocolVersion : supportedMajorVersions.keySet()) {
            System.out.println("\n===========================================================[Version: " + protocolVersion + "]=========================================================================\n");
            try {
                FileReader fileReader = new FileReader(new File("src/test/resources/formatted items " + protocolVersion + ".json"));
                Gson g = new Gson();
                List<ProtocolItem> protocolItems = g.fromJson(fileReader, new TypeToken<List<ProtocolItem>>() {
                }.getType());
                Map<String, ProtocolItem> nameMapping = new HashMap<>();
                for (ProtocolItem protocolItem : protocolItems) {
                    nameMapping.put(protocolItem.getName().replace("minecraft:", "").toUpperCase(), protocolItem);
                }
                List<String> correctIDIncorrectData = new ArrayList<>();
                List<String> incorrectID = new ArrayList<>();
                List<String> missingID = new ArrayList<>();
                for (ItemType itemType : ItemType.values()) {
                    if (itemType == ItemType.NO_DATA) continue;
                    if (nameMapping.containsKey(itemType.toString())) {
                        int id = nameMapping.get(itemType.toString()).getProtocol_id();
                        int data = nameMapping.get(itemType.toString()).getProtocol_data();
                        ItemIDMapping itemIDMapping = itemType.getApplicableMappingNoFallback(ProtocolVersions.getSupportedMajorVersions().get(protocolVersion));
                        if (itemIDMapping != null) {
                            if (id == itemIDMapping.getId()) {
                                if (data != itemIDMapping.getData()) {
                                    //correct id but incorrect data defined
                                    correctIDIncorrectData.add("[ItemType Testing] [ERROR] Item: " + itemType.toString() + " has incorrect data BUT correct id for major version: "
                                            + protocolVersion + ", Correct data is: " + data);
                                    errors = true;
                                }
                            } else {
                                //incorrect id defined
                                if (data == 0)
                                    incorrectID.add("[ItemType Testing] [ERROR] Item: " + itemType.toString() + " has an incorrect id for major version: " + protocolVersion +
                                            ", correct id is: " + id);
                                else
                                    incorrectID.add("[ItemType Testing] [ERROR] Item: " + itemType.toString() + " has an incorrect id for major version: " + protocolVersion +
                                            ", correct id is: " + id + " data is: " + data);
                                errors = true;
                            }
                        } else {
                            //item defined in minecraft lists but no id was defined in ItemType
                            if (data == 0)
                                missingID.add("[ItemType Testing] [ERROR] Item: " + itemType.toString() + " was defined in minecraft lists but was missing id for major version: "
                                        + protocolVersion + ", correct id is: " + id);
                            else
                                missingID.add("[ItemType Testing] [ERROR] Item: " + itemType.toString() + " was defined in minecraft lists but was missing id for major version: "
                                        + protocolVersion + ", correct id is: " + id + " data is: " + data);
                            errors = true;
                        }
                    } else {
                        if (showWarnings)
                            //item not defined in minecraft lists, newer or renamed item?
                            System.out.println("[ItemType Testing] [WARNING] Item: " + itemType.toString() + " was not defined in minecraft lists for major version: " + protocolVersion +
                                    ", this could possibly be an item for a newer version, renamed item or a typo!");
                        warnings = true;
                    }
                }
                //the below sorts the error messages into categories and prints them out so that they are a lot easier to read
                if (!correctIDIncorrectData.isEmpty()) {
                    System.out.println("---------------------------------------------------------------------------------------------------------------------------------");
                    correctIDIncorrectData.sort(String::compareToIgnoreCase);
                    correctIDIncorrectData.forEach(System.out::println);
                }
                if (!incorrectID.isEmpty()) {
                    System.out.println("---------------------------------------------------------------------------------------------------------------------------------");
                    incorrectID.sort(String::compareToIgnoreCase);
                    incorrectID.forEach(System.out::println);
                }
                if (!missingID.isEmpty()) {
                    System.out.println("---------------------------------------------------------------------------------------------------------------------------------");
                    missingID.sort(String::compareToIgnoreCase);
                    missingID.forEach(System.out::println);
                }
            } catch (Exception e) {
                System.out.println("[ItemType Testing] [ERROR] Error occurred on major version " + protocolVersion + ". \nStackTrace: ");
                e.printStackTrace();
                errors = true;
            }
        }
        assertFalse(errors);
        if (warnings)
            System.out.println("[ItemType Testing] [WARNING] Correct id check finished successfully, but warnings were detected it is advised to check them!");
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    private class ProtocolItem {
        @Getter
        @Setter
        String name;
        @Getter
        @Setter
        int protocol_id;
        @Setter
        @Getter
        int protocol_data = 0;

        ProtocolItem(String name, int protocol_id) {
            this.name = name;
            this.protocol_id = protocol_id;
        }
    }
}