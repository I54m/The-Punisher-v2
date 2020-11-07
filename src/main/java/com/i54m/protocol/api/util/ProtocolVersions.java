package com.i54m.protocol.api.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class contains protocol constants similar to {@link net.md_5.bungee.protocol.ProtocolConstants}
 */
public final class ProtocolVersions {

    public final static int MINECRAFT_1_8 = 47;
    public final static int MINECRAFT_1_9 = 107;
    public final static int MINECRAFT_1_9_1 = 108;
    public final static int MINECRAFT_1_9_2 = 109;
    public final static int MINECRAFT_1_9_3 = 110;
    public final static int MINECRAFT_1_9_4 = MINECRAFT_1_9_3;
    public final static int MINECRAFT_1_10 = 210;
    public final static int MINECRAFT_1_11 = 315;
    public final static int MINECRAFT_1_11_1 = 316;
    public final static int MINECRAFT_1_11_2 = MINECRAFT_1_11_1;
    public final static int MINECRAFT_1_12 = 335;
    public final static int MINECRAFT_1_12_1 = 338;
    public final static int MINECRAFT_1_12_2 = 340;
    public final static int MINECRAFT_1_13 = 393;
    public final static int MINECRAFT_1_13_1 = 401;
    public final static int MINECRAFT_1_13_2 = 404;
    public final static int MINECRAFT_1_14 = 477;
    public final static int MINECRAFT_1_14_1 = 480;
    public final static int MINECRAFT_1_14_2 = 485;
    public final static int MINECRAFT_1_14_3 = 490;
    public final static int MINECRAFT_1_14_4 = 498;
    public final static int MINECRAFT_1_15 = 573;
    public final static int MINECRAFT_1_15_1 = 575;
    public final static int MINECRAFT_1_15_2 = 578;
    public final static int MINECRAFT_1_16 = 735;
    public final static int MINECRAFT_1_16_1 = 736;
    public static final int MINECRAFT_1_16_2 = 751;
    public static final int MINECRAFT_1_16_3 = 753;
    public static final int MINECRAFT_1_16_4 = 754;// https://wiki.vg/Pre-release_protocol https://wiki.vg/Protocol_version_numbers


    public final static int MINECRAFT_LATEST = MINECRAFT_1_16_4;

    private ProtocolVersions() {}

    public static ArrayList<Integer> getSupportedVersions() {
        ArrayList<Integer> supportedVersions = new ArrayList<>();
        supportedVersions.add(MINECRAFT_1_8);
        supportedVersions.add(MINECRAFT_1_9);
        supportedVersions.add(MINECRAFT_1_9_1);
        supportedVersions.add(MINECRAFT_1_9_2);
        supportedVersions.add(MINECRAFT_1_9_3);
        supportedVersions.add(MINECRAFT_1_10);
        supportedVersions.add(MINECRAFT_1_11);
        supportedVersions.add(MINECRAFT_1_11_1);
        supportedVersions.add(MINECRAFT_1_12);
        supportedVersions.add(MINECRAFT_1_12_1);
        supportedVersions.add(MINECRAFT_1_12_2);
        supportedVersions.add(MINECRAFT_1_13);
        supportedVersions.add(MINECRAFT_1_13_1);
        supportedVersions.add(MINECRAFT_1_13_2);
        supportedVersions.add(MINECRAFT_1_14);
        supportedVersions.add(MINECRAFT_1_14_1);
        supportedVersions.add(MINECRAFT_1_14_2);
        supportedVersions.add(MINECRAFT_1_14_3);
        supportedVersions.add(MINECRAFT_1_14_4);
        supportedVersions.add(MINECRAFT_1_15);
        supportedVersions.add(MINECRAFT_1_15_1);
        supportedVersions.add(MINECRAFT_1_15_2);
        supportedVersions.add(MINECRAFT_1_16);
        supportedVersions.add(MINECRAFT_1_16_1);
        supportedVersions.add(MINECRAFT_1_16_2);
        supportedVersions.add(MINECRAFT_1_16_3);
        supportedVersions.add(MINECRAFT_1_16_4);
        return supportedVersions;
    }

    public static Map<String, Integer> getSupportedMajorVersions() {
        Map<String, Integer> supportedVersions = new TreeMap<>();
        supportedVersions.put("1_8", MINECRAFT_1_8);
        supportedVersions.put("1_9", MINECRAFT_1_9);
        supportedVersions.put("1_10", MINECRAFT_1_10);
        supportedVersions.put("1_11", MINECRAFT_1_11);
        supportedVersions.put("1_12", MINECRAFT_1_12);
        supportedVersions.put("1_13", MINECRAFT_1_13);
        supportedVersions.put("1_14", MINECRAFT_1_14);
        supportedVersions.put("1_15", MINECRAFT_1_15);
        supportedVersions.put("1_16", MINECRAFT_1_16);
        supportedVersions.put("1_16_2", MINECRAFT_1_16_2); // considered a major version due to the piglin brute spawn egg and some other id changes
        return supportedVersions;
    }

}
