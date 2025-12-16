package com.example.mymusicplayer.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mymusicplayer.R;
import com.example.mymusicplayer.controller.MusicAdapter;
import com.example.mymusicplayer.controller.MusicClient;
import com.example.mymusicplayer.model.Artist;
import com.example.mymusicplayer.model.Music;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArtistSongsFragment extends Fragment {

    private static final String ARG_ARTIST = "artist";
    private static final int PAGE_SIZE = 50;

    private Artist artist;
    private ExecutorService executor;
    private MusicClient musicClient;

    private ImageView ivBack;
    private ImageView ivArtistImage;
    private TextView tvArtistName;
    private TextView tvArtistGenre;
    private TextView tvSongCount;
    private RecyclerView rvArtistSongs;
    private ProgressBar pbLoading;
    private MusicAdapter adapter;

    public static ArtistSongsFragment newInstance(Artist artist) {
        ArtistSongsFragment fragment = new ArtistSongsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ARTIST, artist);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            artist = (Artist) getArguments().getSerializable(ARG_ARTIST);
        }

        executor = Executors.newSingleThreadExecutor();
        musicClient = new MusicClient();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_artist_songs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check if artist data exists
        if (artist == null) {
            Toast.makeText(requireContext(), "Artist data not found", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        // Initialize views
        ivBack = view.findViewById(R.id.ivBack);
        ivArtistImage = view.findViewById(R.id.ivArtistImage);
        tvArtistName = view.findViewById(R.id.tvArtistName);
        tvArtistGenre = view.findViewById(R.id.tvArtistGenre);
        tvSongCount = view.findViewById(R.id.tvSongCount);
        rvArtistSongs = view.findViewById(R.id.rvArtistSongs);
        pbLoading = view.findViewById(R.id.pbLoading);

        // Setup back button
        ivBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        rvArtistSongs.setLayoutManager(layoutManager);

        adapter = new MusicAdapter(new ArrayList<>(), new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Music music, int position) {
                playArtistSongs(position);
            }

            @Override
            public void onMoreOptionsClick(Music music, int position) {
                Toast.makeText(requireContext(), "Add to playlist: " + music.getJudulLagu(), Toast.LENGTH_SHORT).show();
            }
        });

        rvArtistSongs.setAdapter(adapter);

        // Play All button
        CardView cvPlayAll = view.findViewById(R.id.cvPlayAll);
        if (cvPlayAll != null) {
            cvPlayAll.setOnClickListener(v -> {
                if (adapter.getItemCount() > 0) {
                    playArtistSongs(0);
                } else {
                    Toast.makeText(requireContext(), "No songs available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Load artist info and songs
        loadArtistInfo();
        loadArtistSongs();
    }

    private void loadArtistInfo() {
        if (artist == null) return;

        tvArtistName.setText(artist.getName());
        tvArtistGenre.setText(artist.getGenre());

        // Load artist image
        if (artist.getImageUrl() != null && !artist.getImageUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(artist.getImageUrl())
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .centerCrop()
                    .into(ivArtistImage);
        }
    }

    private void loadArtistSongs() {
        if (artist == null) return;

        pbLoading.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                // Search for songs by this artist
                String json = musicClient.searchSongs(artist.getSearchQuery(), PAGE_SIZE, 0);
                List<Music> songs = parseSongs(json);

                requireActivity().runOnUiThread(() -> {
                    adapter.setData(songs);
                    tvSongCount.setText(songs.size() + " songs");
                    pbLoading.setVisibility(View.GONE);

                    if (songs.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "No songs found for " + artist.getName(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Failed to load songs: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private List<Music> parseSongs(String json) throws Exception {
        List<Music> list = new ArrayList<>();

        JSONObject root = new JSONObject(json);
        JSONArray results = root.optJSONArray("results");
        if (results == null) return list;

        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.getJSONObject(i);

            String trackName = item.optString("trackName", "");
            String artistName = item.optString("artistName", "");
            String genre = item.optString("primaryGenreName", "");
            String releaseDate = item.optString("releaseDate", "");
            String artwork = item.optString("artworkUrl100", "");
            String previewUrl = item.optString("previewUrl", "");

            // Filter only songs with preview URLs
            if (previewUrl == null || previewUrl.isEmpty() || !previewUrl.startsWith("http")) {
                continue;
            }

            String year = releaseDate.length() >= 4 ? releaseDate.substring(0, 4) : "";
            list.add(new Music(trackName, genre, artistName, year, artwork, previewUrl));
        }

        return list;
    }

    private void playArtistSongs(int position) {
        try {
            ArrayList<String> urls = new ArrayList<>();
            ArrayList<String> titles = new ArrayList<>();
            ArrayList<String> artists = new ArrayList<>();
            ArrayList<String> artworks = new ArrayList<>();

            List<Music> allMusic = adapter.getMusicList();

            for (Music m : allMusic) {
                urls.add(m.getUrlLagu() != null ? m.getUrlLagu() : "");
                titles.add(m.getJudulLagu() != null ? m.getJudulLagu() : "");
                artists.add(m.getPenyanyi() != null ? m.getPenyanyi() : "");
                artworks.add(m.getFotoAlbum() != null ? m.getFotoAlbum() : "");
            }

            PlayerFragment pf = PlayerFragment.newInstance(urls, titles, artists, artworks, position);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, pf)
                    .addToBackStack("player")
                    .commit();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        executor = null;
    }
}