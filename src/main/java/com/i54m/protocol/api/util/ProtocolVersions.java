package com.i54m.protocol.api.util;

import java.util.ArrayList;

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
    public final static int MINECRAFT_1_16_1 = 736;// https://wiki.vg/Pre-release_protocol https://wiki.vg/Protocol_version_numbers


    public final static int MINECRAFT_LATEST = MINECRAFT_1_16_1;

    private ProtocolVersions() {}

    public static ArrayList<Integer> getSupportedVersions() {
        ArrayList<Integer> supportedVersions = new ArrayList<>();
        supportedVersions.add(47);
        supportedVersions.add(107);
        supportedVersions.add(108);
        supportedVersions.add(109);
        supportedVersions.add(110);
        supportedVersions.add(210);
        supportedVersions.add(315);
        supportedVersions.add(316);
        supportedVersions.add(335);
        supportedVersions.add(338);
        supportedVersions.add(340);
        supportedVersions.add(393);
        supportedVersions.add(401);
        supportedVersions.add(404);
        supportedVersions.add(477);
        supportedVersions.add(480);
        supportedVersions.add(485);
        supportedVersions.add(490);
        supportedVersions.add(498);
        supportedVersions.add(573);
        supportedVersions.add(575);
        supportedVersions.add(578);
        supportedVersions.add(735);
        supportedVersions.add(736);
        return supportedVersions;
    }

}
