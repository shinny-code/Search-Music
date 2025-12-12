package com.example.mymusicplayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class EditProfileDialogFragment extends DialogFragment {

    private EditText etUsername, etEmail, etProfilePictureUrl;
    private Button btnSave;
    private User currentUser;
    private OnProfileUpdatedListener profileUpdatedListener;

    public interface OnProfileUpdatedListener {
        void onProfileUpdated(User updatedUser);
    }

    public static EditProfileDialogFragment newInstance(int userId) {
        EditProfileDialogFragment fragment = new EditProfileDialogFragment();
        Bundle args = new Bundle();
        args.putInt("userId", userId);  // Just pass the ID
        fragment.setArguments(args);
        return fragment;
    }

    private void fetchUserFromDatabase(int userId) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            currentUser = db.userDao().getUserById(userId);

            requireActivity().runOnUiThread(() -> {
                if (currentUser != null) {
                    // Populate fields with current user data
                    etUsername.setText(currentUser.username);
                    etEmail.setText(currentUser.email);
                    etProfilePictureUrl.setText(currentUser.profilePictureUrl != null ? currentUser.profilePictureUrl : "");
                }
            });
        }).start();
    }

    public void setOnProfileUpdatedListener(OnProfileUpdatedListener listener) {
        this.profileUpdatedListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            int userId = getArguments().getInt("userId", -1);
            if (userId != -1) {
                // Fetch user from database
                fetchUserFromDatabase(userId);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_edit_profile_dialog, null);

        etUsername = view.findViewById(R.id.etUsername);
        etEmail = view.findViewById(R.id.etEmail);
        etProfilePictureUrl = view.findViewById(R.id.etProfilePictureUrl);
        btnSave = view.findViewById(R.id.btnSave);

        // Populate fields with current user data
        if (currentUser != null) {
            etUsername.setText(currentUser.username);
            etEmail.setText(currentUser.email);
            etProfilePictureUrl.setText(currentUser.profilePictureUrl != null ? currentUser.profilePictureUrl : "");
        }

        btnSave.setOnClickListener(v -> updateProfile());

        builder.setView(view)
                .setTitle("Edit Profile")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        return builder.create();
    }

    private void updateProfile() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String profilePictureUrl = etProfilePictureUrl.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), "Username and email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());

            // Check if email is taken by another user
            if (!email.equals(currentUser.email)) {
                User existingUser = db.userDao().getUserByEmail(email);
                if (existingUser != null && existingUser.userId != currentUser.userId) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Email already taken", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
            }

            // Update user
            currentUser.username = username;
            currentUser.email = email;
            currentUser.profilePictureUrl = profilePictureUrl;

            db.userDao().updateUser(currentUser);

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                if (profileUpdatedListener != null) {
                    profileUpdatedListener.onProfileUpdated(currentUser);
                }
                dismiss();
            });
        }).start();
    }
}