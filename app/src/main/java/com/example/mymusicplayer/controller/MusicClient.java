package com.example.mymusicplayer.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MusicClient {

    public String searchSongs(String term, int limit, int offset) throws Exception {
        if (term == null) term = "";
        if (term.trim().isEmpty()) term = "a";
        String encoded = URLEncoder.encode(term, StandardCharsets.UTF_8.name());
        String urlStr = "https://itunes.apple.com/search?term=" + encoded +
                "&media=music&entity=song&limit=" + limit + "&offset=" + offset;
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    public String searchSongs(String term, int limit) throws Exception {
        return searchSongs(term, limit, 0);
    }
}
