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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search_music, container, false);

        etSearch = view.findViewById(R.id.etSearch);
        btnSearch = view.findViewById(R.id.btnSearch);
        rvResult = view.findViewById(R.id.rvResult);
        progressBar = view.findViewById(R.id.pbSearch); // from xml

        executor = Executors.newSingleThreadExecutor();

        rvResult.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MusicAdapter(new ArrayList<>(), music -> {

            ArrayList<String> url = new ArrayList<>();
            ArrayList<String> title = new ArrayList<>();
            ArrayList<String> artist = new ArrayList<>();
            ArrayList<String> artwork = new ArrayList<>();

            // gunakan getter
            url.add(music.getUrlLagu() != null ? music.getUrlLagu() : "");
            title.add(music.getJudulLagu() != null ? music.getJudulLagu() : "");
            artist.add(music.getPenyanyi() != null ? music.getPenyanyi() : "");
            artwork.add(music.getFotoAlbum() != null ? music.getFotoAlbum() : "");

            PlayerFragment pf = PlayerFragment.newInstance(url, title, artist, artwork, 0);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, pf)
                    .addToBackStack(null)
                    .commit();
        });

        rvResult.setAdapter(adapter);

        // Button click search
        btnSearch.setOnClickListener(v -> startSearch());

        return view;
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

            String year = releaseDate.length() >= 4 ? releaseDate.substring(0, 4) : "";

            Music music = new Music(trackName, genre, artistName, year, artwork, previewUrl);
            list.add(music);
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
