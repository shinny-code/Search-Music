package com.example.mymusicplayer.view;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mymusicplayer.model.Music;
import com.example.mymusicplayer.controller.MusicAdapter;
import com.example.mymusicplayer.controller.MusicClient;
import com.example.mymusicplayer.model.Playlist;
import com.example.mymusicplayer.model.PlaylistSong;
import com.example.mymusicplayer.model.PlaylistSongCrossRef;
import com.example.mymusicplayer.R;
import com.example.mymusicplayer.controller.ArtistAdapter;
import com.example.mymusicplayer.model.Artist;
import com.example.mymusicplayer.model.User;
import com.example.mymusicplayer.database.AppDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicList extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerView rvArtists;
    private ProgressBar progressBar;
    private MusicAdapter adapter;
    private ArtistAdapter artistAdapter;
    private ExecutorService executor;
    private final MusicClient musicClient = new MusicClient();

    private static final int PAGE_SIZE = 25;
    private int currentOffset = 0;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private String currentQuery = "a";  // default search
    private LinearLayoutManager layoutManager;

    private AppDatabase db;
    private int currentUserId = -1;
    private TextView tvUserName;
    private User currentUser;

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
        tvUserName = view.findViewById(R.id.tvUserName);

        updateUserName();
        loadProfilePicture(ivProfile);

        recyclerView = view.findViewById(R.id.rvMusic);
        rvArtists = view.findViewById(R.id.rvArtists);
        progressBar = view.findViewById(R.id.pbLoading);

        // Setup main music list
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

        // Setup artists grid
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 2);
        rvArtists.setLayoutManager(gridLayoutManager);

        // Create adapter ONCE
        artistAdapter = new ArtistAdapter(new ArrayList<>(), artist -> {
            showArtistSongs(artist);
        });
        rvArtists.setAdapter(artistAdapter);

        // Load popular artists
        loadPopularArtists();

        // Other click listeners
        ImageView ivSearch = view.findViewById(R.id.ivSearch);
        ImageView ivFav = view.findViewById(R.id.ivFav);

        ivProfile.setOnClickListener(v -> {
            Profile profileFragment = new Profile();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, profileFragment)
                    .addToBackStack(null)
                    .commit();
        });

        ivSearch.setOnClickListener(v -> {
            SearchMusic searchFragment = new SearchMusic();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, searchFragment)
                    .addToBackStack(null)
                    .commit();
        });

        ivFav.setOnClickListener(v -> {
            Profile profileFragment = new Profile();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, profileFragment)
                    .addToBackStack(null)
                    .commit();
        });

        setupTabListeners(view);
        loadMore();
        return view;
    }

    private void loadPopularArtists() {
        Log.d("MusicList", "loadPopularArtists called");

        List<Artist> popularArtists = new ArrayList<>();

        // Use reliable image URLs
        popularArtists.add(new Artist("STAYC", "K-Pop", "STAYC",
                "https://i.scdn.co/image/ab67616d0000b273fdb32eb57423828c253e23bb", 45));

        popularArtists.add(new Artist("MAMAMOO", "K-Pop, R&B", "MAMAMOO",
                "https://i.scdn.co/image/ab6761610000e5ebe12972169702affd7a4c48ec", 120));

        popularArtists.add(new Artist("Mocca", "Indie Pop, Jazz", "Mocca band",
                "https://blue.kumparan.com/image/upload/fl_progressive,fl_lossy,c_fill,f_auto,q_auto:best,w_640/v1614670937/oyib2z1youul732qeuro.jpg", 35));

        popularArtists.add(new Artist("Blackpink", "K-Pop, EDM", "Blackpink",
                "https://i.scdn.co/image/ab676161000051749b57f5eccf180a0049be84b3", 85));

        Log.d("MusicList", "Loaded " + popularArtists.size() + " artists");
        artistAdapter.setData(popularArtists);
        Log.d("MusicList", "Adapter has " + artistAdapter.getItemCount() + " items");
    }

    private void showArtistSongs(Artist artist) {
        try {
            Log.d("MusicList", "showArtistSongs called for artist: " + artist.getName());

            // Create ArtistSongsFragment with the artist data
            ArtistSongsFragment artistSongsFragment = ArtistSongsFragment.newInstance(artist);

            // Navigate to artist songs fragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, artistSongsFragment)
                    .addToBackStack("artist_songs")
                    .commit();

        } catch (Exception e) {
            Log.e("MusicList", "Error showing artist songs: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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

                    loadProfilePictureWithGlide(loggedInUser.profilePictureUrl, ivProfile);

                } else {
                    setDefaultProfileImage(ivProfile);
                }
            });
        });
    }

    private void loadProfilePictureWithGlide(String profilePictureUrl, ImageView imageView) {
        try {
            if (profilePictureUrl.startsWith("/")) {
                File imageFile = new File(profilePictureUrl);

                if (imageFile.exists()) {
                    Glide.with(requireContext())
                            .load(imageFile)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .circleCrop()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(imageView);
                } else {
                    setDefaultProfileImage(imageView);
                }
            } else if (profilePictureUrl.startsWith("http") || profilePictureUrl.startsWith("content://")) {
                Glide.with(requireContext())
                        .load(profilePictureUrl)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageView);
            } else {
                setDefaultProfileImage(imageView);
            }
        } catch (Exception e) {
            e.printStackTrace();
            setDefaultProfileImage(imageView);
        }
    }

    private void setDefaultProfileImage(ImageView imageView) {
        try {
            Glide.with(requireContext())
                    .load(R.drawable.default_profile)
                    .circleCrop()
                    .into(imageView);
        } catch (Exception e) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_profile);
            if (bitmap != null) {
                RoundedBitmapDrawable roundedDrawable =
                        RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                roundedDrawable.setCircular(true);
                imageView.setImageDrawable(roundedDrawable);
            }
        }
    }

    private void setupTabListeners(View view) {
        TextView tabAll = view.findViewById(R.id.tabAll);
        TextView tabNew = view.findViewById(R.id.tabNew);
        TextView tabTrending = view.findViewById(R.id.tabTrending);
        TextView tabTop = view.findViewById(R.id.tabTop);

        tabAll.setOnClickListener(v -> {
            currentQuery = "a";
            currentOffset = 0;
            loadMore();
            updateTabSelection(tabAll, tabNew, tabTrending, tabTop);
        });

        tabNew.setOnClickListener(v -> {
            currentQuery = "2024";
            currentOffset = 0;
            loadMore();
            updateTabSelection(tabNew, tabAll, tabTrending, tabTop);
        });

        tabTrending.setOnClickListener(v -> {
            currentQuery = "popular";
            currentOffset = 0;
            loadMore();
            updateTabSelection(tabTrending, tabAll, tabNew, tabTop);
        });

        tabTop.setOnClickListener(v -> {
            currentQuery = "top";
            currentOffset = 0;
            loadMore();
            updateTabSelection(tabTop, tabAll, tabNew, tabTrending);
        });
    }

    private void updateTabSelection(TextView selectedTab, TextView... otherTabs) {
        selectedTab.setBackgroundResource(R.drawable.tab_selected);
        selectedTab.setTextColor(getResources().getColor(android.R.color.black));

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
        if (currentUserId == -1) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            List<Playlist> playlists = db.playlistDao().getUserPlaylists(currentUserId);

            if (getActivity() == null) return;

            requireActivity().runOnUiThread(() -> {
                List<String> dialogOptions = new ArrayList<>();
                for (Playlist p : playlists) {
                    dialogOptions.add(p.name);
                }
                dialogOptions.add("Create new playlist...");

                if (playlists.isEmpty()) {
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
                        Playlist newPlaylist = new Playlist();
                        newPlaylist.name = playlistName;
                        newPlaylist.userId = currentUserId;

                        long playlistId = db.playlistDao().createPlaylist(newPlaylist);
                        Thread.sleep(100);

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

                PlaylistSong existingSong = db.playlistDao().getSongByUrl(music.getUrlLagu());
                int songId;

                if (existingSong == null) {
                    PlaylistSong song = new PlaylistSong(music);
                    songId = (int) db.playlistDao().insertSong(song);
                } else {
                    songId = existingSong.getSongId();
                }

                int count = db.playlistDao().isSongInPlaylist(playlistId, songId);
                if (count > 0) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "Song already in playlist",
                                    Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

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
        updateUserName();

        if (getView() != null) {
            ImageView ivProfile = getView().findViewById(R.id.ivProfile);
            if (ivProfile != null) {
                loadProfilePicture(ivProfile);
            }
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