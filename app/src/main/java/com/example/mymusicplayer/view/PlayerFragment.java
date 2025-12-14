package com.example.mymusicplayer.view;

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
import android.widget.Toast;

import com.example.mymusicplayer.R;
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

    private ImageView btnPlay, btnPrev, btnNext, btnShuffle, btnLoop, ivArtwork, btnBack;
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

        buildPlayOrder(false);
        playPos = Math.max(0, Math.min(currentIndexOriginal, urls.size() - 1));
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
        btnBack = v.findViewById(R.id.btnBack);

        seekBar = v.findViewById(R.id.seekBar);
        tvTitle = v.findViewById(R.id.tvTitle);
        tvArtist = v.findViewById(R.id.tvArtist);
        ivArtwork = v.findViewById(R.id.ivArtwork);
        tvDuration = v.findViewById(R.id.tvDuration);

        updateToggleTint(btnShuffle, shuffleEnabled);
        updateToggleTint(btnLoop, loopEnabled);

        setupListeners();
        playAtCurrentIndex();

        return v;
    }

    private void setupListeners() {

        btnBack.setOnClickListener(view -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        btnPlay.setOnClickListener(view -> {
            if (!isPrepared) return;
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnPlay.setImageResource(R.drawable.play);
            } else {
                mediaPlayer.start();
                btnPlay.setImageResource(R.drawable.pause);
                updateSeekBar();
            }
        });

        btnNext.setOnClickListener(view -> playNext());
        btnPrev.setOnClickListener(view -> playPrevious());

        btnShuffle.setOnClickListener(view -> {
            shuffleEnabled = !shuffleEnabled;
            buildPlayOrder(shuffleEnabled);
            playPos = playOrder.indexOf(currentIndexOriginal);
            updateToggleTint(btnShuffle, shuffleEnabled);
        });

        btnLoop.setOnClickListener(view -> {
            loopEnabled = !loopEnabled;
            updateToggleTint(btnLoop, loopEnabled);
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && isPrepared) {
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }


    private void buildPlayOrder(boolean shuffle) {
        playOrder.clear();
        for (int i = 0; i < urls.size(); i++) playOrder.add(i);
        if (shuffle) Collections.shuffle(playOrder);
    }

    private void playAtCurrentIndex() {
        if (urls.isEmpty()) return;

        int original = playOrder.get(playPos);
        currentIndexOriginal = original;

        tvTitle.setText(titles.get(original));
        tvArtist.setText(artists.get(original));

        loadArtwork(artworks.get(original));
        prepareAudio(urls.get(original));
    }

    private void loadArtwork(String art) {
        if (art == null || art.isEmpty()) {
            ivArtwork.setImageResource(R.drawable.ic_launcher_background);
            return;
        }

        Picasso.get()
                .load(art)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .fit()
                .centerCrop()
                .into(ivArtwork);
    }

    private void playNext() {
        if (playPos < playOrder.size() - 1) {
            playPos++;
        } else if (loopEnabled) {
            playPos = 0;
        } else {
            stopPlayer();
            return;
        }
        playAtCurrentIndex();
    }

    private void playPrevious() {
        if (playPos > 0) {
            playPos--;
        } else if (loopEnabled) {
            playPos = playOrder.size() - 1;
        }
        playAtCurrentIndex();
    }

    private void prepareAudio(String url) {
        releasePlayer();

        // Check if URL is valid
        if (url == null || url.isEmpty() || !url.startsWith("http")) {
            Toast.makeText(requireContext(), "Invalid audio URL", Toast.LENGTH_SHORT).show();
            return;
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );

            mediaPlayer.setDataSource(url);

            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                seekBar.setMax(mp.getDuration());
                tvDuration.setText(formatTime(mp.getDuration()));
                mp.start();
                btnPlay.setImageResource(R.drawable.pause);
                updateSeekBar();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Audio playback error", Toast.LENGTH_SHORT).show();
                    btnPlay.setImageResource(R.drawable.play);
                });
                return true;
            });

            mediaPlayer.setOnCompletionListener(mp -> playNext());
            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            e.printStackTrace();
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Failed to load audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
            releasePlayer();
        }
    }

    private void updateToggleTint(ImageView iv, boolean enabled) {
        int onColor = Color.WHITE;
        int offColor = Color.parseColor("#65FFFFFF");
        ImageViewCompat.setImageTintList(iv,
                ColorStateList.valueOf(enabled ? onColor : offColor)
        );
    }

    private void updateSeekBar() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPrepared && mediaPlayer.isPlaying()) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    handler.postDelayed(this, 500);
                }
            }
        }, 500);
    }

    private String formatTime(int ms) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(ms),
                TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        );
    }

    private void stopPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            btnPlay.setImageResource(R.drawable.play);
        }
    }

    private void releasePlayer() {
        if (mediaPlayer == null) return;

        try { mediaPlayer.stop(); } catch (Exception ignored) {}
        try { mediaPlayer.reset(); } catch (Exception ignored) {}
        try { mediaPlayer.release(); } catch (Exception ignored) {}

        mediaPlayer = null;
        isPrepared = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
    }
}
