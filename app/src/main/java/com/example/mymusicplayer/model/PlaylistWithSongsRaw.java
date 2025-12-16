package com.example.mymusicplayer.model;

import androidx.room.Ignore;

public class PlaylistWithSongsRaw {
    public int playlistId;
    public String playlistName;
    public int userId;
    public String createdAt;

    public Integer songId;
    public String previewUrl;
    public String trackName;
    public String artistName;
    public String artworkUrl;
    public String genre;
    public String releaseYear;

    @Ignore
    public Playlist getPlaylist() {
        Playlist playlist = new Playlist();
        playlist.setPlaylistId(playlistId);
        playlist.setName(playlistName);
        playlist.setUserId(userId);
        playlist.setCreatedAt(createdAt);
        return playlist;
    }
}