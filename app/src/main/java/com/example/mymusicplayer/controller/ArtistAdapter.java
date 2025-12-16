package com.example.mymusicplayer.controller;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mymusicplayer.R;
import com.example.mymusicplayer.model.Artist;

import java.util.ArrayList;
import java.util.List;

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder> {

    public interface OnArtistClickListener {
        void onArtistClick(Artist artist);
    }

    private List<Artist> artists;
    private final OnArtistClickListener listener;

    public ArtistAdapter(List<Artist> artists, OnArtistClickListener listener) {
        this.artists = artists != null ? artists : new ArrayList<>();
        this.listener = listener;
    }

    public void setData(List<Artist> newArtists) {
        this.artists = newArtists != null ? newArtists : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_artist_card, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        try {
            Artist artist = artists.get(position);

            Log.d("ArtistAdapter", "Binding artist: " + artist.getName() + " at position: " + position);

            holder.tvArtistName.setText(artist.getName());
            holder.tvArtistGenre.setText(artist.getGenre());
            holder.tvSongCount.setText(artist.getTotalSongs() + " songs");

            // Load artist image with Glide
            if (artist.getImageUrl() != null && !artist.getImageUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(artist.getImageUrl())
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .centerCrop()
                        .into(holder.ivArtistImage);
            } else {
                holder.ivArtistImage.setImageResource(R.drawable.default_profile);
            }

            // Set click listener
            holder.itemView.setOnClickListener(v -> {
                Log.d("ArtistAdapter", "Artist clicked: " + artist.getName());
                if (listener != null) {
                    listener.onArtistClick(artist);
                } else {
                    Log.e("ArtistAdapter", "Listener is null!");
                }
            });

            // Hover effects
            holder.itemView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        holder.itemView.setScaleX(0.95f);
                        holder.itemView.setScaleY(0.95f);
                        holder.ivPlayButton.setVisibility(View.VISIBLE);
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        holder.itemView.setScaleX(1f);
                        holder.itemView.setScaleY(1f);
                        holder.ivPlayButton.setVisibility(View.INVISIBLE);
                        break;
                }
                return false;
            });

        } catch (Exception e) {
            Log.e("ArtistAdapter", "Error in onBindViewHolder: " + e.getMessage(), e);
        }
    }

    @Override
    public int getItemCount() {
        return artists != null ? artists.size() : 0;
    }

    static class ArtistViewHolder extends RecyclerView.ViewHolder {
        ImageView ivArtistImage;
        ImageView ivPlayButton;
        TextView tvArtistName, tvArtistGenre, tvSongCount;

        public ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArtistImage = itemView.findViewById(R.id.ivArtistImage);
            ivPlayButton = itemView.findViewById(R.id.ivPlayButton);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            tvArtistGenre = itemView.findViewById(R.id.tvArtistGenre);
            tvSongCount = itemView.findViewById(R.id.tvSongCount);
        }
    }
}