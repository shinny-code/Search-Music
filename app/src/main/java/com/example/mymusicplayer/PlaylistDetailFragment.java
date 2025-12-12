package com.example.mymusicplayer;

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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailFragment extends Fragment {

    private static final String ARG_PLAYLIST_ID = "playlist_id";
    private static final String ARG_PLAYLIST_NAME = "playlist_name";

    private int playlistId;
    private String playlistName;

    private TextView tvPlaylistName, tvSongCount, tvEmptyMessage;
    private ImageView ivBack, ivPlayAll;
    private RecyclerView rvSongs;
    private ProgressBar progressBar;

    private PlaylistSongAdapter adapter;
    private List<PlaylistSong> songs = new ArrayList<>();
    private AppDatabase db;

    public static PlaylistDetailFragment newInstance(int playlistId, String playlistName) {
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PLAYLIST_ID, playlistId);
        args.putString(ARG_PLAYLIST_NAME, playlistName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            playlistId = getArguments().getInt(ARG_PLAYLIST_ID);
            playlistName = getArguments().getString(ARG_PLAYLIST_NAME);
        }
        db = AppDatabase.getDatabase(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist_detail, container, false);

        tvPlaylistName = view.findViewById(R.id.tvPlaylistName);
        tvSongCount = view.findViewById(R.id.tvSongCount);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        ivBack = view.findViewById(R.id.ivBack);
        ivPlayAll = view.findViewById(R.id.ivPlayAll);
        rvSongs = view.findViewById(R.id.rvSongs);
        progressBar = view.findViewById(R.id.progressBar);

        tvPlaylistName.setText(playlistName);

        setupRecyclerView();
        setupClickListeners();
        loadPlaylistSongs();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new PlaylistSongAdapter(songs, new PlaylistSongAdapter.OnSongClickListener() {
            @Override
            public void onSongClick(PlaylistSong song, int position) {
                playSong(position);
            }

            @Override
            public void onRemoveClick(PlaylistSong song, int position) {
                showRemoveSongDialog(song, position);
            }
        });

        rvSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSongs.setAdapter(adapter);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        ivPlayAll.setOnClickListener(v -> {
            if (!songs.isEmpty()) {
                playSong(0); // Play first song
            } else {
                Toast.makeText(requireContext(), "No songs in playlist", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPlaylistSongs() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyMessage.setVisibility(View.GONE);

        new Thread(() -> {
            List<PlaylistSong> playlistSongs = db.playlistDao().getSongsForPlaylist(playlistId);

            requireActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);

                if (playlistSongs.isEmpty()) {
                    tvEmptyMessage.setVisibility(View.VISIBLE);
                    tvSongCount.setText("0 songs");
                } else {
                    songs.clear();
                    songs.addAll(playlistSongs);
                    adapter.notifyDataSetChanged();
                    tvSongCount.setText(songs.size() + " songs");
                    tvEmptyMessage.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void playSong(int position) {
        if (position < 0 || position >= songs.size()) return;

        PlaylistSong currentSong = songs.get(position);

        // Convert PlaylistSong list to Music list for PlayerFragment
        ArrayList<String> urls = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        ArrayList<String> artists = new ArrayList<>();
        ArrayList<String> artworks = new ArrayList<>();

        for (PlaylistSong song : songs) {
            urls.add(song.getPreviewUrl() != null ? song.getPreviewUrl() : "");
            titles.add(song.getTrackName() != null ? song.getTrackName() : "");
            artists.add(song.getArtistName() != null ? song.getArtistName() : "");
            artworks.add(song.getArtworkUrl() != null ? song.getArtworkUrl() : "");
        }

        PlayerFragment playerFragment = PlayerFragment.newInstance(urls, titles, artists, artworks, position);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, playerFragment)
                .addToBackStack(null)
                .commit();
    }

    private void showRemoveSongDialog(PlaylistSong song, int position) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Remove Song")
                .setMessage("Remove '" + song.getTrackName() + "' from playlist?")
                .setPositiveButton("Remove", (dialog, which) -> removeSongFromPlaylist(song, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeSongFromPlaylist(PlaylistSong song, int position) {
        new Thread(() -> {
            db.playlistDao().removeSongFromPlaylist(playlistId, song.getSongId());

            requireActivity().runOnUiThread(() -> {
                songs.remove(position);
                adapter.notifyItemRemoved(position);
                tvSongCount.setText(songs.size() + " songs");

                if (songs.isEmpty()) {
                    tvEmptyMessage.setVisibility(View.VISIBLE);
                }

                Toast.makeText(requireContext(), "Song removed", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh songs when fragment resumes
        loadPlaylistSongs();
    }
}