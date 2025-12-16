package com.example.mymusicplayer.controller;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.mymusicplayer.model.Playlist;
import com.example.mymusicplayer.model.PlaylistSong;
import com.example.mymusicplayer.model.PlaylistSongCrossRef;
import com.example.mymusicplayer.model.PlaylistWithSongsRaw;

import java.util.List;

@Dao
public interface PlaylistDao {

    // Playlist operations
    @Insert
    long createPlaylist(Playlist playlist);

    @Query("SELECT * FROM playlists WHERE userId = :userId")
    List<Playlist> getUserPlaylists(int userId);

    @Query("SELECT * FROM playlists")
    List<Playlist> getAllPlaylists();

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    void deletePlaylist(int playlistId);

    // Song operations
    @Insert
    long insertSong(PlaylistSong song);

    @Query("SELECT * FROM playlist_songs WHERE previewUrl = :previewUrl")
    PlaylistSong getSongByUrl(String previewUrl);

    @Query("SELECT * FROM playlist_songs WHERE songId = :songId")
    PlaylistSong getSongById(int songId);

    // Cross-reference operations
    @Insert
    void addSongToPlaylist(PlaylistSongCrossRef crossRef);

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    void removeSongFromPlaylist(int playlistId, int songId);

    // Get songs for a playlist (using JOIN)
    @Query("SELECT ps.* FROM playlist_songs ps " +
            "INNER JOIN playlist_song_cross_ref psc ON ps.songId = psc.songId " +
            "WHERE psc.playlistId = :playlistId")
    List<PlaylistSong> getSongsForPlaylist(int playlistId);

    // Get playlists with songs (using JOIN - simpler approach)
    @Query("SELECT p.*, ps.* FROM playlists p " +
            "LEFT JOIN playlist_song_cross_ref psc ON p.playlistId = psc.playlistId " +
            "LEFT JOIN playlist_songs ps ON psc.songId = ps.songId " +
            "WHERE p.userId = :userId")
    List<PlaylistWithSongsRaw> getPlaylistsWithSongsRaw(int userId);

    // Check if song already exists in playlist
    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref " +
            "WHERE playlistId = :playlistId AND songId = :songId")
    int isSongInPlaylist(int playlistId, int songId);
}