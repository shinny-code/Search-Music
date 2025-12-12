package com.example.mymusicplayer.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusicplayer.model.Music;
import com.example.mymusicplayer.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(Music music, int position);

        void onMoreOptionsClick(Music music, int position);
    }

    private List<Music> data;
    private final OnItemClickListener listener;

    public MusicAdapter(List<Music> data, OnItemClickListener listener) {
        this.data = data != null ? data : new ArrayList<>();
        this.listener = listener;
    }

    // ✨ Add this getter method
    public List<Music> getMusicList() {
        return data != null ? data : new ArrayList<>();
    }

    // Aman: mencegah null + langsung refresh
    public void setData(List<Music> newData) {
        this.data = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void appendData(List<Music> newItems) {
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

        holder.tvTitle.setText(m.getJudulLagu());
        holder.tvArtist.setText(m.getPenyanyi());
        holder.tvYear.setText(m.getTahunRilis());

        // ✨ Load artwork dengan Picasso
        Picasso.get()
                .load(m.getFotoAlbum())
                .placeholder(R.drawable.place_album)
                .error(R.drawable.place_album)
                .fit()
                .centerCrop()
                .into(holder.ivArtwork);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onItemClick(m, position);
        });

        holder.ivMoreOptions.setOnClickListener(v -> {
            if (listener != null)
                listener.onMoreOptionsClick(m, position);
        });
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivArtwork, ivMoreOptions;
        TextView tvTitle, tvArtist, tvYear;

        VH(@NonNull View itemView) {
            super(itemView);
            ivArtwork = itemView.findViewById(R.id.ivArtwork);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvYear = itemView.findViewById(R.id.tvYear);
            ivMoreOptions = itemView.findViewById(R.id.ivMoreOptions);
        }
    }
}
