package com.example.mymusicplayer;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

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