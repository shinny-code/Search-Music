package com.example.mymusicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        Fragment musicListFragment = new MusicList();
        Fragment searchMusicFragment = new SearchMusic();
        Fragment profileFragment = new Profile();

        setCurrentFragment(musicListFragment);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.musicList) {
                setCurrentFragment(musicListFragment);
                return true;
            } else if (itemId == R.id.search_music) {
                setCurrentFragment(searchMusicFragment);
                return true;
            } else if (itemId == R.id.profile) {
                setCurrentFragment(profileFragment);
                return true;
            }

            return false; // default
        });
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }
}
