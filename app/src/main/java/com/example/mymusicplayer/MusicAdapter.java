// java
package com.example.mymusicplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(Music music);
    }

    private List<Music> data;
    private final ExecutorService imgExecutor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OnItemClickListener listener;

    public MusicAdapter(List<Music> data, OnItemClickListener listener) {
        this.data = data;
        this.listener = listener;
    }

    public synchronized void setData(List<Music> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    public synchronized void appendData(List<Music> newItems) {
        if (newItems == null || newItems.isEmpty()) return;
        int start = data.size();
        data.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_music, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Music m = data.get(position);

        // Pakai getter (karena variabel private)
        holder.tvTitle.setText(m.getJudulLagu());
        holder.tvArtist.setText(m.getPenyanyi());
        holder.tvYear.setText(m.getTahunRilis());

        // Load gambar
        holder.ivArtwork.setImageResource(R.drawable.ic_launcher_background);

        String url = m.getFotoAlbum();
        if (url != null && !url.isEmpty()) {
            imgExecutor.execute(() -> {
                try {
                    InputStream in = (InputStream) new URL(url).getContent();
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    mainHandler.post(() -> holder.ivArtwork.setImageBitmap(bmp));
                } catch (Exception ignored) {}
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(m);
        });
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivArtwork;
        TextView tvTitle, tvArtist, tvYear;
        VH(@NonNull View itemView) {
            super(itemView);
            ivArtwork = itemView.findViewById(R.id.ivArtwork);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvYear = itemView.findViewById(R.id.tvYear);
        }
    }
}
