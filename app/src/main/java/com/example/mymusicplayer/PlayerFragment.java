package com.example.mymusicplayer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class PlayerFragment extends Fragment {

    private static final String ARG_URLS = "preview_urls";
    private static final String ARG_TITLES = "titles";
    private static final String ARG_ARTISTS = "artists";
    private static final String ARG_ARTWORKS = "artworks";
    private static final String ARG_INDEX = "start_index";

    private ArrayList<String> urls;
    private ArrayList<String> titles;
    private ArrayList<String> artists;
    private ArrayList<String> artworks;
    private int currentIndexOriginal = 0;

    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPrepared = false;

    private ImageView btnPlay, btnPrev, btnNext, btnShuffle, btnLoop, ivArtwork;
    private SeekBar seekBar;
    private TextView tvTitle, tvArtist, tvDuration;

    private ArrayList<Integer> playOrder = new ArrayList<>();
    private int playPos = 0;
    private boolean shuffleEnabled = false;
    private boolean loopEnabled = false;

    public static PlayerFragment newInstance(ArrayList<String> urls,
                                             ArrayList<String> titles,
                                             ArrayList<String> artists,
                                             ArrayList<String> artworks,
                                             int startIndex) {
        PlayerFragment f = new PlayerFragment();
        Bundle b = new Bundle();
        b.putStringArrayList(ARG_URLS, urls);
        b.putStringArrayList(ARG_TITLES, titles);
        b.putStringArrayList(ARG_ARTISTS, artists);
        b.putStringArrayList(ARG_ARTWORKS, artworks);
        b.putInt(ARG_INDEX, startIndex);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle a = getArguments();
        if (a != null) {
            urls = a.getStringArrayList(ARG_URLS);
            titles = a.getStringArrayList(ARG_TITLES);
            artists = a.getStringArrayList(ARG_ARTISTS);
            artworks = a.getStringArrayList(ARG_ARTWORKS);
            currentIndexOriginal = a.getInt(ARG_INDEX, 0);
        }
        if (urls == null) urls = new ArrayList<>();
        if (titles == null) titles = new ArrayList<>();
        if (artists == null) artists = new ArrayList<>();
        if (artworks == null) artworks = new ArrayList<>();

        buildPlayOrder(shuffleEnabled);

        // Find playPos safely: if indexOf returns -1 (empty list), set to 0
        int desiredOriginal = Math.max(0, Math.min(currentIndexOriginal, Math.max(0, urls.size() - 1)));
        playPos = playOrder.isEmpty() ? 0 : playOrder.indexOf(desiredOriginal);
        if (playPos < 0) playPos = 0;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_player, container, false);
        btnPlay = v.findViewById(R.id.btnPlay);
        btnPrev = v.findViewById(R.id.btnPrev);
        btnNext = v.findViewById(R.id.btnNext);
        btnShuffle = v.findViewById(R.id.btnShuffle);
        btnLoop = v.findViewById(R.id.btnLoop);
        seekBar = v.findViewById(R.id.seekBar);
        tvTitle = v.findViewById(R.id.tvTitle);
        tvArtist = v.findViewById(R.id.tvArtist);
        ivArtwork = v.findViewById(R.id.ivArtwork);
        tvDuration = v.findViewById(R.id.tvDuration);

        updateToggleTint(btnShuffle, shuffleEnabled);
        updateToggleTint(btnLoop, loopEnabled);

        setupListeners();
        playAtPlayPos();

        return v;
    }

    private void setupListeners() {
        btnPlay.setOnClickListener(view -> {
            if (!isPrepared || mediaPlayer == null) return;
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnPlay.setImageResource(R.drawable.play);
            } else {
                mediaPlayer.start();
                btnPlay.setImageResource(R.drawable.pause);
                updateSeekBar();
            }
        });

        // Next/Prev should simply move track (no toggling booleans)
        btnNext.setOnClickListener(view -> playNext());
        btnPrev.setOnClickListener(view -> playPrevious());

        btnShuffle.setOnClickListener(view -> {
            shuffleEnabled = !shuffleEnabled;
            buildPlayOrder(shuffleEnabled);

            // keep current track the same original index after rebuild
            int currentOriginal = (playOrder.isEmpty() ? 0 : playOrder.get(Math.max(0, Math.min(playPos, playOrder.size()-1))));
            // find new playPos matching currentOriginal
            playPos = playOrder.indexOf(currentOriginal);
            if (playPos < 0) playPos = 0;

            updateToggleTint(btnShuffle, shuffleEnabled);
        });

        btnLoop.setOnClickListener(view -> {
            loopEnabled = !loopEnabled;
            updateToggleTint(btnLoop, loopEnabled);
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null && isPrepared) {
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void updateToggleTint(ImageView iv, boolean enabled) {
        int onColor = Color.WHITE;
        int offColor = Color.parseColor("#A0A0A0"); // subtle gray when off
        ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(enabled ? onColor : offColor));
    }

    private void loadArtworkIntoImageView(String imgArtwork) {
        int placeholder = R.drawable.ic_launcher_background;
        int errorDrawable = R.drawable.ic_launcher_background;

        if (imgArtwork == null || imgArtwork.trim().isEmpty()) {
            ivArtwork.setImageResource(placeholder);
            return;
        }

        imgArtwork = imgArtwork.trim();

        // Remote URL
        if (imgArtwork.startsWith("http://") || imgArtwork.startsWith("https://")) {
            Picasso.get()
                    .load(imgArtwork)
                    .placeholder(placeholder)
                    .error(errorDrawable)
                    .into(ivArtwork);
            return;
        }

        try {
            int resId = Integer.parseInt(imgArtwork);
            ivArtwork.setImageResource(resId);
            return;
        } catch (NumberFormatException ignored) {}

        int resIdByName = getResources().getIdentifier(imgArtwork, "drawable", requireContext().getPackageName());
        if (resIdByName != 0) {
            ivArtwork.setImageResource(resIdByName);
            return;
        }

        Picasso.get()
                .load(imgArtwork)
                .placeholder(placeholder)
                .error(errorDrawable)
                .into(ivArtwork);
    }

    private void buildPlayOrder(boolean shuffle) {
        playOrder.clear();
        int n = urls.size();
        for (int i = 0; i < n; i++) playOrder.add(i);
        if (shuffle) Collections.shuffle(playOrder);
    }

    private void playAtPlayPos() {
        if (playOrder.isEmpty()) return;
        if (playPos < 0 || playPos >= playOrder.size()) playPos = 0;

        int originalIndex = playOrder.get(playPos);
        currentIndexOriginal = originalIndex;
        String url = (originalIndex >= 0 && originalIndex < urls.size()) ? urls.get(originalIndex) : "";
        String title = (originalIndex >= 0 && originalIndex < titles.size()) ? titles.get(originalIndex) : "";
        String artist = (originalIndex >= 0 && originalIndex < artists.size()) ? artists.get(originalIndex) : "";
        String imgArtwork = (originalIndex >= 0 && originalIndex < artworks.size()) ? artworks.get(originalIndex) : "";

        loadArtworkIntoImageView(imgArtwork);
        tvTitle.setText(title);
        tvArtist.setText(artist);

        prepareAudio(url);
    }

    private void playNext() {
        if (playOrder.isEmpty()) return;
        if (playPos < playOrder.size() - 1) {
            playPos++;
            playAtPlayPos();
        } else {
            if (loopEnabled) {
                playPos = 0;
                playAtPlayPos();
            } else {
                if (mediaPlayer != null && isPrepared) {
                    mediaPlayer.pause();
                    mediaPlayer.seekTo(mediaPlayer.getDuration());
                    btnPlay.setImageResource(R.drawable.play);
                }
            }
        }
    }

    private void playPrevious() {
        if (playOrder.isEmpty()) return;
        if (playPos > 0) {
            playPos--;
            playAtPlayPos();
        } else {
            if (loopEnabled) {
                playPos = playOrder.size() - 1;
                playAtPlayPos();
            } else {
                if (mediaPlayer != null && isPrepared) {
                    mediaPlayer.seekTo(0);
                }
            }
        }
    }

    private void prepareAudio(String url) {
        releasePlayer();
        if (url == null || url.isEmpty()) return;

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                seekBar.setMax(mediaPlayer.getDuration());
                tvDuration.setText(formatTime(mediaPlayer.getDuration()));
                mediaPlayer.start();
                btnPlay.setImageResource(R.drawable.pause);
                updateSeekBar();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                playNext();
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            // Silently fail but release
            releasePlayer();
        }
    }

    private void updateSeekBar() {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPrepared) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    handler.postDelayed(this, 500);
                }
            }
        }, 500);
    }

    private String formatTime(int ms) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignored) {}
            try { mediaPlayer.reset(); } catch (Exception ignored) {}
            try { mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
            isPrepared = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        releasePlayer();
        handler.removeCallbacksAndMessages(null);
    }
}
