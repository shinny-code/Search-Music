package com.example.mymusicplayer.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(primaryKeys = {"playlistId", "songId"},
        tableName = "playlist_song_cross_ref",
        foreignKeys = {
                @ForeignKey(entity = Playlist.class,
                        parentColumns = "playlistId",
                        childColumns = "playlistId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = PlaylistSong.class,
                        parentColumns = "songId",
                        childColumns = "songId",
                        onDelete = ForeignKey.CASCADE)
        })
public class PlaylistSongCrossRef {
    public int playlistId;
    public int songId;

    public PlaylistSongCrossRef() {}

    public PlaylistSongCrossRef(int playlistId, int songId) {
        this.playlistId = playlistId;
        this.songId = songId;
    }

    public int getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(int playlistId) {
        this.playlistId = playlistId;
    }

    public int getSongId() {
        return songId;
    }

    public void setSongId(int songId) {
        this.songId = songId;
    }
}