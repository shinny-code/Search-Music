package com.example.mymusicplayer;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlists",
        foreignKeys = @ForeignKey(entity = User.class,
                parentColumns = "userId",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE))
public class Playlist {
    @PrimaryKey(autoGenerate = true)
    public int playlistId;

    public String name;
    public int userId; // Foreign key to User
    public String createdAt;

    // Add this constructor
    public Playlist() {
        this.createdAt = String.valueOf(System.currentTimeMillis());
    }

    public Playlist(String name, int userId) {
        this.name = name;
        this.userId = userId;
        this.createdAt = String.valueOf(System.currentTimeMillis());
    }

    // Getters and Setters
    public int getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(int playlistId) {
        this.playlistId = playlistId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}