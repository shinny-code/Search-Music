package com.example.mymusicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.example.mymusicplayer.view.MusicList;
import com.example.mymusicplayer.view.Profile;
import com.example.mymusicplayer.view.SearchMusic;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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

            return false;
        });
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }
}
