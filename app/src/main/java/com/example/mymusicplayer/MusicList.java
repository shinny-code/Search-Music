package com.example.mymusicplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicList extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private MusicAdapter adapter;
    private ExecutorService executor;
    private final MusicClient musicClient = new MusicClient();

    private static final int PAGE_SIZE = 25;
    private int currentOffset = 0;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private String currentQuery = "a";  // default search
    private LinearLayoutManager layoutManager;

    private AppDatabase db;
    private int currentUserId = -1; // Store current user ID
    private TextView tvUserName; // Add this field
    private User currentUser; // Store current user object

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getDatabase(requireContext());
        executor = Executors.newSingleThreadExecutor();

        // Get current logged in user
        executor.execute(() -> {
            User loggedInUser = db.userDao().getLoggedInUser();
            if (loggedInUser != null) {
                currentUserId = loggedInUser.userId;
                currentUser = loggedInUser;
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_list, container, false);

        ImageView ivProfile = view.findViewById(R.id.ivProfile);
        tvUserName = view.findViewById(R.id.tvUserName); // Initialize the TextView

        // Update user name in UI
        updateUserName();

        // Load profile picture
        loadProfilePicture(ivProfile);

        recyclerView = view.findViewById(R.id.rvMusic);
        progressBar = view.findViewById(R.id.pbLoading);

        layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);

        adapter = new MusicAdapter(new ArrayList<>(), new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Music music, int position) {
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
                            .addToBackStack(null)
                            .commit();

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onMoreOptionsClick(Music music, int position) {
                showAddToPlaylistDialog(music);
            }
        });

        recyclerView.setAdapter(adapter);

        // Setup click listeners for icons
        ImageView ivSearch = view.findViewById(R.id.ivSearch);
        ImageView ivFav = view.findViewById(R.id.ivFav);

        ivProfile.setOnClickListener(v -> {
            // Navigate to Profile fragment
            Profile profileFragment = new Profile();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, profileFragment)
                    .addToBackStack(null)
                    .commit();
        });

        ivSearch.setOnClickListener(v -> {
            // Navigate to Search fragment
            SearchMusic searchFragment = new SearchMusic();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, searchFragment)
                    .addToBackStack(null)
                    .commit();
        });

        ivFav.setOnClickListener(v -> {
            // Navigate to Profile fragment or show favorite songs
            Profile profileFragment = new Profile();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, profileFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Setup tab click listeners
        setupTabListeners(view);

        RecyclerView rvTrending = view.findViewById(R.id.rvRecommended);
        rvTrending.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        List<TrendingModel> trending = new ArrayList<>();
        trending.add(new TrendingModel(
                "Discover weekly",
                "The original slow instrumental best playlist",
                R.drawable.girl_headphone
        ));

        TrendingAdapter trendingAdapter = new TrendingAdapter(trending);
        rvTrending.setAdapter(trendingAdapter);

        loadMore();
        return view;
    }

    private void updateUserName() {
        executor.execute(() -> {
            User loggedInUser = db.userDao().getLoggedInUser();
            if (loggedInUser != null) {
                currentUser = loggedInUser;
                currentUserId = loggedInUser.userId;

                requireActivity().runOnUiThread(() -> {
                    if (tvUserName != null) {
                        String displayName = loggedInUser.username != null ?
                                loggedInUser.username : "User";
                        tvUserName.setText("Hi, " + displayName);
                    }
                });
            } else {
                requireActivity().runOnUiThread(() -> {
                    if (tvUserName != null) {
                        tvUserName.setText("Hi, Guest");
                    }
                });
            }
        });
    }

    private void loadProfilePicture(ImageView ivProfile) {
        executor.execute(() -> {
            User loggedInUser = db.userDao().getLoggedInUser();
            requireActivity().runOnUiThread(() -> {
                if (loggedInUser != null && loggedInUser.profilePictureUrl != null &&
                        !loggedInUser.profilePictureUrl.isEmpty()) {
                    // Load profile picture from URL using Picasso
                    Picasso.get()
                            .load(loggedInUser.profilePictureUrl)
                            .placeholder(R.drawable.profile)
                            .error(R.drawable.profile)
                            .into(ivProfile);
                } else {
                    // Use default circular profile picture
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.profile);
                    RoundedBitmapDrawable roundedDrawable =
                            RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                    roundedDrawable.setCircular(true);
                    ivProfile.setImageDrawable(roundedDrawable);
                }
            });
        });
    }

    private void setupTabListeners(View view) {
        TextView tabAll = view.findViewById(R.id.tabAll);
        TextView tabNew = view.findViewById(R.id.tabNew);
        TextView tabTrending = view.findViewById(R.id.tabTrending);
        TextView tabTop = view.findViewById(R.id.tabTop);

        tabAll.setOnClickListener(v -> {
            // Reset search to default
            currentQuery = "a";
            currentOffset = 0;
            loadMore();
            updateTabSelection(tabAll, tabNew, tabTrending, tabTop);
        });

        tabNew.setOnClickListener(v -> {
            // Search for new releases
            currentQuery = "2024";
            currentOffset = 0;
            loadMore();
            updateTabSelection(tabNew, tabAll, tabTrending, tabTop);
        });

        tabTrending.setOnClickListener(v -> {
            // Search for trending songs
            currentQuery = "popular";
            currentOffset = 0;
            loadMore();
            updateTabSelection(tabTrending, tabAll, tabNew, tabTop);
        });

        tabTop.setOnClickListener(v -> {
            // Search for top songs
            currentQuery = "top";
            currentOffset = 0;
            loadMore();
            updateTabSelection(tabTop, tabAll, tabNew, tabTrending);
        });
    }

    private void updateTabSelection(TextView selectedTab, TextView... otherTabs) {
        // Update selected tab style
        selectedTab.setBackgroundResource(R.drawable.tab_selected);
        selectedTab.setTextColor(getResources().getColor(android.R.color.black));

        // Update other tabs style
        for (TextView tab : otherTabs) {
            tab.setBackgroundResource(R.drawable.tab_unselected);
            tab.setTextColor(getResources().getColor(android.R.color.white));
        }
    }

    private void loadMore() {
        if (isLoading) return;
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        final int offsetToLoad = currentOffset;

        executor.execute(() -> {
            try {
                String json = musicClient.searchSongs(currentQuery, PAGE_SIZE, offsetToLoad);
                List<Music> list = parseSongs(json);

                requireActivity().runOnUiThread(() -> {
                    if (offsetToLoad == 0) {
                        adapter.setData(list);
                    } else {
                        adapter.appendData(list);
                    }

                    currentOffset += list.size();
                    if (list.size() < PAGE_SIZE) hasMore = false;

                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load songs", Toast.LENGTH_SHORT).show();
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

            if (previewUrl == null || previewUrl.isEmpty() || !previewUrl.startsWith("http")) {
                continue;
            }

            String year = releaseDate.length() >= 4 ? releaseDate.substring(0, 4) : "";
            list.add(new Music(trackName, genre, artistName, year, artwork, previewUrl));
        }

        return list;
    }

    private void showAddToPlaylistDialog(Music music) {
        // Check if user is logged in
        if (currentUserId == -1) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            // Get only this user's playlists
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
                        // Create new playlist WITH USER ID
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

    private void addSongToExistingPlaylist(Music music, Playlist playlist) {
        addSongToExistingPlaylist(music, playlist.playlistId);
    }

    private void addSongToExistingPlaylist(Music music, int playlistId) {
        executor.execute(() -> {
            try {
                // 1. Check if playlist exists
                List<Playlist> playlists = db.playlistDao().getUserPlaylists(currentUserId);
                boolean playlistExists = false;
                for (Playlist p : playlists) {
                    if (p.playlistId == playlistId) {
                        playlistExists = true;
                        break;
                    }
                }

                if (!playlistExists) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "Playlist not found",
                                    Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                // 2. Check if song already exists in songs table
                PlaylistSong existingSong = db.playlistDao().getSongByUrl(music.getUrlLagu());
                int songId;

                if (existingSong == null) {
                    // Song doesn't exist, insert it
                    PlaylistSong song = new PlaylistSong(music);
                    songId = (int) db.playlistDao().insertSong(song);
                } else {
                    // Song already exists
                    songId = existingSong.getSongId();
                }

                // 3. Check if song is already in this playlist
                int count = db.playlistDao().isSongInPlaylist(playlistId, songId);
                if (count > 0) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "Song already in playlist",
                                    Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                // 4. Create the link between the song and the playlist
                PlaylistSongCrossRef crossRef = new PlaylistSongCrossRef(playlistId, songId);
                db.playlistDao().addSongToPlaylist(crossRef);

                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                "Song added to playlist!",
                                Toast.LENGTH_SHORT).show()
                );

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                "Error adding song: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user info when fragment resumes
        updateUserName();
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