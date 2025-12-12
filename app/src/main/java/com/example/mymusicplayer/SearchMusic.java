package com.example.mymusicplayer;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchMusic extends Fragment {

    private EditText etSearch;
    private Button btnSearch;
    private RecyclerView rvResult;
    private ProgressBar progressBar;

    private ExecutorService executor;
    private MusicAdapter adapter;
    private MusicClient client = new MusicClient();

    private AppDatabase db;
    private int currentUserId = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getDatabase(requireContext());

        // Initialize executor in onCreate
        executor = Executors.newSingleThreadExecutor();

        // Get current logged in user
        executor.execute(() -> {
            User loggedInUser = db.userDao().getLoggedInUser();
            if (loggedInUser != null) {
                currentUserId = loggedInUser.userId;
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search_music, container, false);

        etSearch = view.findViewById(R.id.etSearch);
        btnSearch = view.findViewById(R.id.btnSearch);
        rvResult = view.findViewById(R.id.rvResult);
        progressBar = view.findViewById(R.id.pbSearch);

        rvResult.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new MusicAdapter(new ArrayList<>(), new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Music music, int position) {
                ArrayList<String> urls = new ArrayList<>();
                ArrayList<String> titles = new ArrayList<>();
                ArrayList<String> artists = new ArrayList<>();
                ArrayList<String> artworks = new ArrayList<>();

                // Get all songs from adapter
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
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onMoreOptionsClick(Music music, int position) {
                showAddToPlaylistDialog(music);
            }
        });

        rvResult.setAdapter(adapter);

        // Load default music when fragment is created
        loadDefaultMusic();

        // Button click search
        btnSearch.setOnClickListener(v -> startSearch());

        return view;
    }

    private void showAddToPlaylistDialog(Music music) {
        // Check if user is logged in
        if (currentUserId == -1) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            // Get ONLY this user's playlists
            List<Playlist> playlists = db.playlistDao().getUserPlaylists(currentUserId);

            if (getActivity() == null) return;

            requireActivity().runOnUiThread(() -> {
                List<String> dialogOptions = new ArrayList<>();
                for (Playlist p : playlists) {
                    dialogOptions.add(p.name);
                }
                dialogOptions.add("Create new playlist...");

                if (playlists.isEmpty()) {
                    // If user has no playlists, suggest creating one
                    showCreatePlaylistDialog(music);
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Add to Playlist")
                        .setItems(dialogOptions.toArray(new String[0]), (dialog, which) -> {
                            if (which == dialogOptions.size() - 1) {
                                showCreatePlaylistDialog(music);
                            } else {
                                Playlist selectedPlaylist = playlists.get(which);
                                addSongToExistingPlaylist(music, selectedPlaylist);
                            }
                        });
                builder.create().show();
            });
        });
    }

    private void addSongToExistingPlaylist(Music music, Playlist playlist) {
        addSongToExistingPlaylist(music, playlist.playlistId);
    }

    private void addSongToExistingPlaylist(Music music, long playlistId) {
        executor.execute(() -> {
            try {
                // 1. Check if song already exists in songs table
                PlaylistSong existingSong = db.playlistDao().getSongByUrl(music.getUrlLagu());
                long songId;

                if (existingSong == null) {
                    // Song doesn't exist, insert it
                    PlaylistSong song = new PlaylistSong();
                    song.previewUrl = music.getUrlLagu();
                    song.trackName = music.getJudulLagu();
                    song.artistName = music.getPenyanyi();
                    song.artworkUrl = music.getFotoAlbum();
                    song.genre = music.getGenre();
                    song.releaseYear = music.getTahunRilis();

                    songId = db.playlistDao().insertSong(song);
                } else {
                    // Song already exists
                    songId = existingSong.songId;
                }

                // 2. Check if song is already in this playlist
                int count = db.playlistDao().isSongInPlaylist((int) playlistId, (int) songId);
                if (count > 0) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Song already in playlist", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                // 3. Create the link between the song and the playlist
                PlaylistSongCrossRef crossRef = new PlaylistSongCrossRef();
                crossRef.playlistId = (int) playlistId;
                crossRef.songId = (int) songId;
                db.playlistDao().addSongToPlaylist(crossRef);

                // Show a confirmation toast on the main thread
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Song added to playlist", Toast.LENGTH_SHORT).show()
                );
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error adding song: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void showCreatePlaylistDialog(Music music) {
        // Check if user is logged in
        if (currentUserId == -1) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("New Playlist");

        final EditText input = new EditText(requireContext());
        input.setHint("Playlist Name");
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String playlistName = input.getText().toString().trim();
            if (!playlistName.isEmpty()) {
                executor.execute(() -> {
                    try {
                        // Add userId to the playlist
                        Playlist newPlaylist = new Playlist();
                        newPlaylist.name = playlistName;
                        newPlaylist.userId = currentUserId;

                        long playlistId = db.playlistDao().createPlaylist(newPlaylist);

                        // Wait a bit to ensure playlist is created
                        Thread.sleep(100);

                        // Now add the song
                        addSongToExistingPlaylist(music, (int) playlistId);

                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(),
                                    "Playlist created and song added!",
                                    Toast.LENGTH_SHORT).show();
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(),
                                    "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Method to load default music
    private void loadDefaultMusic() {
        progressBar.setVisibility(View.VISIBLE);

        // Check if executor is still running
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor();
        }

        executor.execute(() -> {
            try {
                // Search for popular songs as default
                String json = client.searchSongs("a", 15, 0);
                List<Music> list = parseSongs(json);

                requireActivity().runOnUiThread(() -> {
                    adapter.setData(list);
                    progressBar.setVisibility(View.GONE);

                    if (list.isEmpty()) {
                        // If no results for "popular", try "music" as backup
                        loadBackupDefaultMusic();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    // Load backup if API fails
                    loadBackupDefaultMusic();
                });
            }
        });
    }

    // Backup method with hardcoded popular songs
    private void loadBackupDefaultMusic() {
        List<Music> defaultSongs = getHardcodedDefaultSongs();
        adapter.setData(defaultSongs);
        progressBar.setVisibility(View.GONE);
    }

    // Hardcoded popular songs as backup
    private List<Music> getHardcodedDefaultSongs() {
        List<Music> defaultSongs = new ArrayList<>();

        // Add popular songs (you can customize this list)
        defaultSongs.add(new Music("Blinding Lights", "Pop", "The Weeknd", "2020",
                "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/6b/4c/05/6b4c05a6-8f5c-5c8f-5c5f-5c8f5c5f5c5f/source/100x100bb.jpg",
                "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview125/v4/6b/4c/05/6b4c05a6-8f5c-5c8f-5c5f-5c8f5c5f5c5f/mzaf_1234567890.m4a"));

        defaultSongs.add(new Music("Stay", "Pop", "The Kid LAROI, Justin Bieber", "2021",
                "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/6b/4c/05/6b4c05a6-8f5c-5c8f-5c5f-5c8f5c5f5c5f/source/100x100bb.jpg",
                "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview125/v4/6b/4c/05/6b4c05a6-8f5c-5c8f-5c5f-5c8f5c5f5c5f/mzaf_1234567890.m4a"));

        defaultSongs.add(new Music("Heat Waves", "Alternative", "Glass Animals", "2020",
                "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/6b/4c/05/6b4c05a6-8f5c-5c8f-5c5f-5c8f5c5f5c5f/source/100x100bb.jpg",
                "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview125/v4/6b/4c/05/6b4c05a6-8f5c-5c8f-5c5f-5c8f5c5f5c5f/mzaf_1234567890.m4a"));

        defaultSongs.add(new Music("As It Was", "Pop", "Harry Styles", "2022",
                "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/6b/4c/05/6b4c05a6-8f5c-5c8f-5c5f-5c8f5c5f5c5f/source/100x100bb.jpg",
                "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview125/v4/6b/4c/05/6b4c05a6-8f5c-5c8f-5c5f-5c8f5c5f5c5f/mzaf_1234567890.m4a"));

        defaultSongs.add(new Music("Bad Habit", "Pop", "Steve Lacy", "2022",
                "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/6b/4c/05/6b4c05a6-8f5c-5c8f-5c5f-5c8f5c5f5c5f/source/100x100bb.jpg",
                "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview125/v4/6b/4c/05/6b4c05a6-8f5c-5c8f-5c5f-5c8f5c5f5c5f/mzaf_1234567890.m4a"));

        return defaultSongs;
    }

    private void startSearch() {
        String keyword = etSearch.getText().toString().trim();
        if (keyword.isEmpty()) {
            Toast.makeText(requireContext(), "Masukkan kata kunci!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor();
        }

        executor.execute(() -> {
            try {
                String json = client.searchSongs(keyword, 25, 0);
                List<Music> list = parseSongs(json);

                requireActivity().runOnUiThread(() -> {
                    adapter.setData(list);
                    progressBar.setVisibility(View.GONE);

                    if (list.isEmpty()) {
                        Toast.makeText(requireContext(), "Tidak ada hasil", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Gagal mengambil data", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private List<Music> parseSongs(String json) throws Exception {
        List<Music> list = new ArrayList<>();
        org.json.JSONObject root = new org.json.JSONObject(json);
        org.json.JSONArray results = root.optJSONArray("results");

        if (results == null) return list;

        for (int i = 0; i < results.length(); i++) {
            org.json.JSONObject item = results.getJSONObject(i);

            String trackName = item.optString("trackName", "");
            String artistName = item.optString("artistName", "");
            String genre = item.optString("primaryGenreName", "");
            String releaseDate = item.optString("releaseDate", "");
            String artwork = item.optString("artworkUrl100", "");
            String previewUrl = item.optString("previewUrl", "");

            // Skip songs without preview URL
            if (previewUrl == null || previewUrl.isEmpty() || !previewUrl.startsWith("http")) {
                continue;
            }

            String year = releaseDate.length() >= 4 ? releaseDate.substring(0, 4) : "";

            Music music = new Music(trackName, genre, artistName, year, artwork, previewUrl);
            list.add(music);
        }

        return list;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        executor = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh current user ID when fragment resumes
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor();
        }

        executor.execute(() -> {
            User loggedInUser = db.userDao().getLoggedInUser();
            if (loggedInUser != null) {
                currentUserId = loggedInUser.userId;
            } else {
                currentUserId = -1;
            }
        });
    }
}