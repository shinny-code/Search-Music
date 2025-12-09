package com.example.mymusicplayer;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        executor = Executors.newSingleThreadExecutor();
        View view = inflater.inflate(R.layout.fragment_music_list, container, false);

        recyclerView = view.findViewById(R.id.rvMusic);
        progressBar = view.findViewById(R.id.pbLoading);
        androidx.appcompat.widget.SearchView searchView = view.findViewById(R.id.searchView);

        layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);

        // adapter dengan listener yang menggunakan getter (access via getter!)
        adapter = new MusicAdapter(new ArrayList<>(), music -> {
            ArrayList<String> urls = new ArrayList<>();
            ArrayList<String> titles = new ArrayList<>();
            ArrayList<String> artists = new ArrayList<>();
            ArrayList<String> artworks = new ArrayList<>();

            // gunakan getter, jangan akses field private secara langsung
            urls.add(music.getUrlLagu() != null ? music.getUrlLagu() : "");
            titles.add(music.getJudulLagu() != null ? music.getJudulLagu() : "");
            artists.add(music.getPenyanyi() != null ? music.getPenyanyi() : "");
            artworks.add(music.getFotoAlbum() != null ? music.getFotoAlbum() : "");

            PlayerFragment pf = PlayerFragment.newInstance(urls, titles, artists, artworks, 0);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, pf)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerView.setAdapter(adapter);

        // Infinite scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy <= 0) return;

                int total = adapter.getItemCount();
                int lastVisible = layoutManager.findLastVisibleItemPosition();

                if (!isLoading && hasMore && lastVisible >= total - 5) {
                    loadMore();
                }
            }
        });

        // Search bar
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = (query == null || query.trim().isEmpty()) ? "a" : query.trim();
                currentOffset = 0;
                hasMore = true;
                loadSearch(currentQuery);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        loadMore();
        return view;
    }

    private void loadMore() {
        if (isLoading) return;
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        final int offsetToLoad = currentOffset;

        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor();
        }

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

    private void loadSearch(String keyword) {
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor();
        }

        executor.execute(() -> {
            try {
                String json = musicClient.searchSongs(keyword, PAGE_SIZE, 0);
                List<Music> list = parseSongs(json);

                requireActivity().runOnUiThread(() -> {
                    adapter.setData(list);
                    currentOffset = list.size();
                    hasMore = list.size() >= PAGE_SIZE;
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Gagal mengambil data", Toast.LENGTH_SHORT).show();
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

            String year = releaseDate.length() >= 4 ? releaseDate.substring(0, 4) : "";

            list.add(new Music(trackName, genre, artistName, year, artwork, previewUrl));
        }

        return list;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        executor = null;
    }
}
