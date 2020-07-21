package com.i54m.punisher.utils;

import com.google.gson.Gson;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

public class UpdateChecker {

    private static final Update UPDATE = callURL();

    public static boolean check(final String currentVersion) {
        if (getCurrentVersion() == null)
            return false;
            if (!getCurrentVersion().equals(currentVersion)) {
                String[] thisParts = currentVersion.split("\\.");
                String[] thatParts = getCurrentVersion().split("\\.");
                int length = Math.max(thisParts.length, thatParts.length);
                for (int i = 0; i < length; i++) {
                    int thisPart = i < thisParts.length ?
                            Integer.parseInt(thisParts[i]) : 0;
                    int thatPart = i < thatParts.length ?
                            Integer.parseInt(thatParts[i]) : 0;
                    if (thisPart < thatPart)
                        return true;
                    if (thisPart > thatPart)
                        return false;
                }
            }
            return false;
    }

    public static String getCurrentVersion(){
        assert UPDATE != null;
        if (UPDATE.getCurrentVersionNumber().equals("null")) return null;
        return UPDATE.getCurrentVersionNumber();
    }

    public static String getRealeaseDate(){
        assert UPDATE != null;
        if (UPDATE.getLastUpdate().equals("null")) return null;
        return UPDATE.getLastUpdate();
    }

    protected static Update callURL() {
        StringBuilder sb = new StringBuilder();
        URLConnection urlConn;
        try {
            urlConn = new URL("https://api.i54m.com/the-punisher/version/").openConnection();
            urlConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
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
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }catch(Exception e) {
            e.printStackTrace();
        }
        if (!sb.toString().isEmpty()) {
            Gson g = new Gson();
            Update update;
            update = g.fromJson(sb.toString(), Update.class);
            return update;
        }else return new Update("null", "null");
    }

    protected static class Update {
        @Getter
        private final String currentVersionNumber, lastUpdate;

        public Update(String currentVersionNumber, String lastUpdate) {
            this.currentVersionNumber = currentVersionNumber;
            this.lastUpdate = lastUpdate;
        }
    }
}
