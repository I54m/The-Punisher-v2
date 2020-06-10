package com.i54mpenguin.punisher.utils;


import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;

public class NameFetcher {

    private static final HashMap<String, String> NAMES = new HashMap<>();

    public static String getName(String uuid) {
        if (uuid.equalsIgnoreCase("console"))
            return "CONSOLE";
        uuid = uuid.replace("-", "");
        if (NAMES.containsKey(uuid))
            return NAMES.get(uuid);
        String output = callURL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
        Gson g = new Gson();
        Profile profile;
        profile = g.fromJson(output.substring(0, output.indexOf(",\n  \"properties\""))+ "}", Profile.class);
        new UUIDFetcher().storeUUID(uuid, profile.getName());
        NAMES.put(uuid, profile.getName());
        return profile.getName();
    }

    protected static String callURL(String URL) {
        StringBuilder sb = new StringBuilder();
        URLConnection urlConn;
        InputStreamReader in = null;
        try {
            URL url = new URL(URL);
            urlConn = url.openConnection();
            if (urlConn != null) urlConn.setReadTimeout(6000);
            if (urlConn != null && urlConn.getInputStream() != null) {
                in = new InputStreamReader(urlConn.getInputStream(), Charset.defaultCharset());
                BufferedReader bufferedReader = new BufferedReader(in);
                if (bufferedReader != null) {
                    int cp;
                    while ((cp = bufferedReader.read()) != -1) {
                        sb.append((char) cp);
                    }
                    bufferedReader.close();
                }
            }
            if (in != null)
                in.close();
        } catch (IOException e) {
            //ignore error
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static void updateStoredName(String uuid, String name) {
        storeName(uuid, name);
    }

    public static void storeName(String uuid, String name) {
        uuid = uuid.replace("-", "");
        NAMES.put(uuid, name);
    }

    public static String getStoredName(String uuid) {
        return NAMES.get(uuid);
    }

    public static boolean hasNameStored(String uuid) {
        return NAMES.containsKey(uuid);
    }

    protected static class Profile {
        private final String name, id;

        public Profile(String name, String id) {
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public String getUuid() {
            return id;
        }
    }


}