package com.example.mymusicplayer.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusicplayer.model.PlaylistSong;
import com.example.mymusicplayer.R;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

public class PlaylistSongAdapter extends RecyclerView.Adapter<PlaylistSongAdapter.ViewHolder> {

    public interface OnSongClickListener {
        void onSongClick(PlaylistSong song, int position);
        void onRemoveClick(PlaylistSong song, int position);
    }

    private List<PlaylistSong> songs;
    private final OnSongClickListener listener;

    public PlaylistSongAdapter(List<PlaylistSong> songs, OnSongClickListener listener) {
        this.songs = songs != null ? songs : new ArrayList<>();
        this.listener = listener;
    }

    public void setSongs(List<PlaylistSong> songs) {
        this.songs = songs != null ? songs : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaylistSong song = songs.get(position);

        // Set data
        holder.tvTitle.setText(song.getTrackName() != null ? song.getTrackName() : "Unknown Title");
        holder.tvArtist.setText(song.getArtistName() != null ? song.getArtistName() : "Unknown Artist");
        holder.tvGenre.setText(song.getGenre() != null ? song.getGenre() : "");

        if (song.getReleaseYear() != null && !song.getReleaseYear().isEmpty()) {
            holder.tvYear.setText(song.getReleaseYear());
            holder.tvYear.setVisibility(View.VISIBLE);
        } else {
            holder.tvYear.setVisibility(View.GONE);
        }

        // Load artwork
        if (song.getArtworkUrl() != null && !song.getArtworkUrl().isEmpty()) {
            Picasso.get()
                    .load(song.getArtworkUrl())
                    .placeholder(R.drawable.place_album)
                    .error(R.drawable.place_album)
                    .fit()
                    .centerCrop()
                    .into(holder.ivArtwork);
        } else {
            holder.ivArtwork.setImageResource(R.drawable.place_album);
        }

        // Song click (play) - WHOLE ITEM
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(song, position);
            }
        });

        // Remove button click - ONLY IF BUTTON EXISTS
        if (holder.ivRemove != null) {
            holder.ivRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveClick(song, position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivArtwork, ivRemove;
        TextView tvTitle, tvArtist, tvGenre, tvYear;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArtwork = itemView.findViewById(R.id.ivArtwork);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvGenre = itemView.findViewById(R.id.tvGenre);
            tvYear = itemView.findViewById(R.id.tvYear);
            ivRemove = itemView.findViewById(R.id.ivRemove);
        }
    }
}