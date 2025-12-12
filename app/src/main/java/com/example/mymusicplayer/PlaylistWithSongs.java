package com.example.mymusicplayer;

import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;

public class PlaylistWithSongs {
    @Embedded
    public Playlist playlist;

    @Relation(
            parentColumn = "playlistId",
            entityColumn = "songId"
    )
    public List<PlaylistSong> songs;

    public PlaylistWithSongs() {}

    public PlaylistWithSongs(Playlist playlist, List<PlaylistSong> songs) {
        this.playlist = playlist;
        this.songs = songs;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    public List<PlaylistSong> getSongs() {
        return songs;
    }

    public void setSongs(List<PlaylistSong> songs) {
        this.songs = songs;
    }
}