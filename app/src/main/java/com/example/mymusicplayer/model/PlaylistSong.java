package com.example.mymusicplayer.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlist_songs")
public class PlaylistSong {
    @PrimaryKey(autoGenerate = true)
    public int songId;

    public String previewUrl;
    public String trackName;
    public String artistName;
    public String artworkUrl;
    public String genre;
    public String releaseYear;

    public PlaylistSong() {}

    public PlaylistSong(String previewUrl, String trackName, String artistName,
                        String artworkUrl, String genre, String releaseYear) {
        this.previewUrl = previewUrl;
        this.trackName = trackName;
        this.artistName = artistName;
        this.artworkUrl = artworkUrl;
        this.genre = genre;
        this.releaseYear = releaseYear;
    }

    public PlaylistSong(Music music) {
        this.previewUrl = music.getUrlLagu();
        this.trackName = music.getJudulLagu();
        this.artistName = music.getPenyanyi();
        this.artworkUrl = music.getFotoAlbum();
        this.genre = music.getGenre();
        this.releaseYear = music.getTahunRilis();
    }

    // Getters and Setters
    public int getSongId() {
        return songId;
    }

    public void setSongId(int songId) {
        this.songId = songId;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    public void setArtworkUrl(String artworkUrl) {
        this.artworkUrl = artworkUrl;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(String releaseYear) {
        this.releaseYear = releaseYear;
    }
}