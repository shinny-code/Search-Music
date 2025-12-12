package com.example.mymusicplayer.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusicplayer.model.Playlist;
import com.example.mymusicplayer.R;
import com.example.mymusicplayer.model.User;
import com.example.mymusicplayer.controller.PlaylistAdapter;
import com.example.mymusicplayer.database.AppDatabase;
import com.squareup.picasso.Picasso;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Profile extends Fragment {
    private ImageView ivProfile;
    private TextView tvUserName, tvEmail, tvPlaylistCount;
    private Button btnLogin, btnLogout, btnEditProfile;
    private RecyclerView rvPlaylists;
    private AppDatabase db;
    private User currentUser;
    private ExecutorService executorService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getDatabase(requireContext());
        // Initialize ExecutorService in onCreate
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        ivProfile = v.findViewById(R.id.ivProfile);
        tvUserName = v.findViewById(R.id.tvUserName);
        tvEmail = v.findViewById(R.id.tvEmail);
        tvPlaylistCount = v.findViewById(R.id.tvPlaylistCount);
        btnLogin = v.findViewById(R.id.btnLogin);
        btnLogout = v.findViewById(R.id.btnLogout);
        btnEditProfile = v.findViewById(R.id.btnEditProfile);
        rvPlaylists = v.findViewById(R.id.rvPlaylists);

        checkLoginStatus();

        btnLogin.setOnClickListener(view -> showLoginDialog());
        btnLogout.setOnClickListener(view -> logout());
        btnEditProfile.setOnClickListener(view -> {
            if (currentUser != null) {
                showEditProfileDialog();
            } else {
                Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkLoginStatus();
    }

    private void checkLoginStatus() {
        // Check if executor is still active
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }

        executorService.execute(() -> {
            User loggedInUser = db.userDao().getLoggedInUser();
            requireActivity().runOnUiThread(() -> {
                if (loggedInUser != null) {
                    currentUser = loggedInUser;
                    updateUIWithUserData(currentUser);
                    loadUserPlaylists();
                } else {
                    showGuestUI();
                }
            });
        });
    }

    private void updateUIWithUserData(User user) {
        if (getView() == null) return;

        tvUserName.setText(user.username != null ? user.username : "User");
        tvEmail.setText(user.email != null ? user.email : "user@example.com");

        if (user.profilePictureUrl != null && !user.profilePictureUrl.isEmpty()) {
            Picasso.get()
                    .load(user.profilePictureUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.profile);
        }

        btnLogin.setVisibility(View.GONE);
        btnLogout.setVisibility(View.VISIBLE);
        btnEditProfile.setVisibility(View.VISIBLE);
    }

    private void showGuestUI() {
        if (getView() == null) return;

        tvUserName.setText("Guest User");
        tvEmail.setText("Login to see your profile");
        tvPlaylistCount.setText("0 playlists");
        ivProfile.setImageResource(R.drawable.profile);

        btnLogin.setVisibility(View.VISIBLE);
        btnLogout.setVisibility(View.GONE);
        btnEditProfile.setVisibility(View.GONE);

        rvPlaylists.setAdapter(null);
    }

    private void loadUserPlaylists() {
        // Check if executor is still active
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }

        executorService.execute(() -> {
            if (currentUser != null && getActivity() != null) {
                List<Playlist> playlists = db.playlistDao().getUserPlaylists(currentUser.userId);
                requireActivity().runOnUiThread(() -> {
                    if (getView() == null) return;

                    if (playlists.isEmpty()) {
                        tvPlaylistCount.setText("0 playlists");
                        rvPlaylists.setAdapter(null);
                    } else {
                        PlaylistAdapter adapter = new PlaylistAdapter(playlists, playlist -> {
                            openPlaylistSongs(playlist);
                        });
                        rvPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));
                        rvPlaylists.setAdapter(adapter);
                        tvPlaylistCount.setText(playlists.size() + " playlists");
                    }
                });
            }
        });
    }

    private void showLoginDialog() {
        LoginDialogFragment loginDialog = new LoginDialogFragment();
        loginDialog.setOnLoginSuccessListener(() -> {
            checkLoginStatus();
        });
        loginDialog.show(getParentFragmentManager(), "login_dialog");
    }

    private void showEditProfileDialog() {
        EditProfileDialogFragment editProfileDialog = EditProfileDialogFragment.newInstance(currentUser.userId);
        editProfileDialog.setOnProfileUpdatedListener(updatedUser -> {
            currentUser = updatedUser;
            updateUIWithUserData(currentUser);
        });
        editProfileDialog.show(getParentFragmentManager(), "edit_profile_dialog");
    }

    private void openPlaylistSongs(Playlist playlist) {
        PlaylistDetailFragment fragment = PlaylistDetailFragment.newInstance(
                playlist.playlistId,
                playlist.name
        );
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void logout() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }

        executorService.execute(() -> {
            if (currentUser != null) {
                // Mark user as not logged in
                currentUser.isLoggedIn = false;
                db.userDao().updateUser(currentUser);
            }

            requireActivity().runOnUiThread(() -> {
                if (getView() == null) return;

                currentUser = null;
                showGuestUI();
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Shutdown executor when fragment is destroyed
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Don't shutdown executor here, only in onDestroy
        // This prevents the RejectedExecutionException
    }
}