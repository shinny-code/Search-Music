package com.example.mymusicplayer.model;

import java.io.Serializable;

public class Music implements Serializable {
    private final String JudulLagu;
    private final String Genre;
    private final String Penyanyi;
    private final String TahunRilis;
    private final String FotoAlbum;
    private final String UrlLagu;

    public Music(String judulLagu, String genre, String penyanyi, String tahunRilis, String fotoAlbum, String urlLagu) {
        this.JudulLagu = judulLagu;
        this.Genre = genre;
        this.Penyanyi = penyanyi;
        this.TahunRilis = tahunRilis;
        this.FotoAlbum = fotoAlbum;
        this.UrlLagu = urlLagu;
    }

    public String getJudulLagu() {
        return JudulLagu;
    }

    public String getGenre() {
        return Genre;
    }

    public String getPenyanyi() {      // BENAR
        return Penyanyi;               // BENAR
    }

    public String getTahunRilis() {
        return TahunRilis;
    }

    public String getFotoAlbum() {
        return FotoAlbum;
    }

    public String getUrlLagu() {
        return UrlLagu;
    }
}
