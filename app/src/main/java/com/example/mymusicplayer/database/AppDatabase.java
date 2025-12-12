package com.example.mymusicplayer.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.example.mymusicplayer.model.Playlist;
import com.example.mymusicplayer.controller.PlaylistDao;
import com.example.mymusicplayer.model.PlaylistSong;
import com.example.mymusicplayer.model.PlaylistSongCrossRef;
import com.example.mymusicplayer.model.User;
import com.example.mymusicplayer.controller.UserDao;

@Database(entities = {
        User.class,
        Playlist.class,
        PlaylistSong.class,
        PlaylistSongCrossRef.class
},
        version = 1,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract UserDao userDao();
    public abstract PlaylistDao playlistDao();

    public static synchronized AppDatabase getDatabase(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "music_player.db")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries() // REMOVE THIS IN PRODUCTION - only for debugging
                    .build();
        }
        return instance;
    }
}