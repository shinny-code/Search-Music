package com.example.mymusicplayer.model;

import java.io.Serializable;

public class Artist implements Serializable {
    private String name;
    private String genre;
    private String searchQuery;
    private String imageUrl;
    private int totalSongs;

    // Default constructor REQUIRED for Serializable
    public Artist() {
    }

    public Artist(String name, String genre, String searchQuery, String imageUrl, int totalSongs) {
        this.name = name;
        this.genre = genre;
        this.searchQuery = searchQuery;
        this.imageUrl = imageUrl;
        this.totalSongs = totalSongs;
    }

    // Getters
    public String getName() { return name; }
    public String getGenre() { return genre; }
    public String getSearchQuery() { return searchQuery; }
    public String getImageUrl() { return imageUrl; }
    public int getTotalSongs() { return totalSongs; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setTotalSongs(int totalSongs) { this.totalSongs = totalSongs; }
}