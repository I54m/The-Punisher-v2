package com.i54mpenguin.punisher.utils;

import com.google.gson.Gson;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;

public class UUIDFetcher implements Callable<String> {

    private static final HashMap<String, String> UUID_CACHE = new HashMap<>();
    private String name;

    public static UUID formatUUID(String uuid) {
        if (uuid.contains("-")) return UUID.fromString(uuid);
        StringBuffer sb = new StringBuffer(uuid);
        sb.insert(8, "-");
        sb = new StringBuffer(sb.toString());
        sb.insert(13, "-");
        sb = new StringBuffer(sb.toString());
        sb.insert(18, "-");
        sb = new StringBuffer(sb.toString());
        sb.insert(23, "-");
        return UUID.fromString(sb.toString());
    }

    public static void updateStoredUUID(String name, String uuid) {
        for (String storedName : UUID_CACHE.keySet()) {
            String StoredUUID = UUID_CACHE.get(storedName);
            if (uuid.equals(StoredUUID)) {
                if (!name.toLowerCase().equals(storedName)) {
                    UUID_CACHE.remove(storedName);
                    UUID_CACHE.put(name.toLowerCase(), uuid);
                }
                return;
            }
        }
        UUID_CACHE.put(name.toLowerCase(), uuid);
    }

    public void fetch(String name) {
        this.name = name;
    }

    @Override
    public String call() throws Exception {
        if (name.equalsIgnoreCase("console"))
            return "CONSOLE";
        if (UUID_CACHE.containsKey(name.toLowerCase()))
            return UUID_CACHE.get(name.toLowerCase());
        StringBuilder sb = new StringBuilder();
        URLConnection urlConn = new URL("https://api.mojang.com/users/profiles/minecraft/" + this.name).openConnection();
        urlConn.setReadTimeout(5000);
        if (urlConn.getInputStream() != null) {
            InputStreamReader in = new InputStreamReader(urlConn.getInputStream(), Charset.defaultCharset());
            BufferedReader bufferedReader = new BufferedReader(in);
            if (bufferedReader.ready()) {
                int cp;
                while ((cp = bufferedReader.read()) != -1) {
                    sb.append((char) cp);
                }
                bufferedReader.close();
            }
            in.close();
        }
        if (sb.toString().isEmpty() || sb.toString().length() < 7) return null;
        Gson g = new Gson();
        Profile profile;
        profile = g.fromJson(sb.toString(), Profile.class);

        NameFetcher.storeName(profile.getId(), profile.getName());
        UUID_CACHE.put(name.toLowerCase(), profile.getId());
        return profile.getId();
    }

    public void storeUUID(String uuid, String name) {
        uuid = uuid.replace("-", "");
        UUID_CACHE.put(name.toLowerCase(), uuid);
    }

    private static class Profile {
        @Getter
        private final String name, id;

        public Profile(String name, String id) {
            this.name = name;
            this.id = id;
        }
    }
}