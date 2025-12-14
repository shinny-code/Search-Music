package com.example.mymusicplayer.view;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mymusicplayer.R;
import com.example.mymusicplayer.model.User;
import com.example.mymusicplayer.database.AppDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileDialogFragment extends DialogFragment {
    private CircleImageView ivProfile;
    private TextView tvUploadPhoto;
    private EditText etUsername, etEmail;
    private Button btnSave;
    private User currentUser;
    private Uri profileImageUri = null;
    private OnProfileUpdatedListener profileUpdatedListener;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Activity result launchers
    private final ActivityResultLauncher<String> pickImageFromGallery =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            profileImageUri = uri;
                            Glide.with(requireContext())
                                    .load(uri)
                                    .circleCrop()
                                    .into(ivProfile);
                        }
                    });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            pickImageFromGallery.launch("image/*");
                        } else {
                            Toast.makeText(requireContext(), "Permission needed to upload photos", Toast.LENGTH_SHORT).show();
                        }
                    });

    public interface OnProfileUpdatedListener {
        void onProfileUpdated(User updatedUser);
    }

    public void setOnProfileUpdatedListener(OnProfileUpdatedListener listener) {
        this.profileUpdatedListener = listener;
    }

    public static EditProfileDialogFragment newInstance(int userId) {
        EditProfileDialogFragment fragment = new EditProfileDialogFragment();
        Bundle args = new Bundle();
        args.putInt("userId", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_edit_profile_dialog, null);

        initializeViews(view);
        loadUserData();

        builder.setView(view)
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dismiss();
                });

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setOnShowListener(dialogInterface -> {
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negativeButton != null) {
                negativeButton.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.white)
                );
                negativeButton.setAllCaps(false);
                negativeButton.setTextSize(16);
            }
        });

        return dialog;
    }

    private void initializeViews(View view) {
        ivProfile = view.findViewById(R.id.ivProfile);
        tvUploadPhoto = view.findViewById(R.id.tvUploadPhoto);
        etUsername = view.findViewById(R.id.etUsername);
        etEmail = view.findViewById(R.id.etEmail);
        btnSave = view.findViewById(R.id.btnSave);

        // Set click listeners
        ivProfile.setOnClickListener(v -> openImagePicker());
        tvUploadPhoto.setOnClickListener(v -> openImagePicker());

        btnSave.setOnClickListener(v -> updateProfile());
    }

    private void loadUserData() {
        Bundle args = getArguments();
        if (args == null) return;

        int userId = args.getInt("userId", -1);
        if (userId == -1) return;

        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            currentUser = db.userDao().getUserById(userId);

            if (currentUser != null) {
                requireActivity().runOnUiThread(() -> {
                    etUsername.setText(currentUser.username);
                    etEmail.setText(currentUser.email);

                    // Load existing profile picture
                    if (currentUser.profilePictureUrl != null && !currentUser.profilePictureUrl.isEmpty()) {
                        loadProfileImage(currentUser.profilePictureUrl);
                    }
                });
            }
        });
    }

    private void loadProfileImage(String imagePath) {
        try {
            if (imagePath.startsWith("/")) {
                // Local file path
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Glide.with(requireContext())
                            .load(imageFile)
                            .circleCrop()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(ivProfile);
                }
            } else if (imagePath.startsWith("http") || imagePath.startsWith("content://")) {
                // URL or content URI
                Glide.with(requireContext())
                        .load(imagePath)
                        .circleCrop()
                        .into(ivProfile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openImagePicker() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Change Profile Picture")
                .setItems(new CharSequence[]{"Choose from Gallery"},
                        (dialog, which) -> {
                            if (which == 0) {
                                checkAndRequestPermission();
                            }
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // NEW METHOD: Check and request appropriate permission based on Android version
    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - Use READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery.launch("image/*");
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0 to 12 (API 23-32) - Use READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery.launch("image/*");
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        } else {
            // Android 5.1 and below - No runtime permission needed
            pickImageFromGallery.launch("image/*");
        }
    }

    private void updateProfile() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email format");
            etEmail.requestFocus();
            return;
        }

        // Check if email is changed and already exists
        if (!email.equals(currentUser.email)) {
            executorService.execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(requireContext());
                User existingUser = db.userDao().getUserByEmail(email);

                if (existingUser != null) {
                    requireActivity().runOnUiThread(() -> {
                        etEmail.setError("Email already registered");
                        etEmail.requestFocus();
                    });
                    return;
                }
                saveProfileChanges(username, email);
            });
        } else {
            saveProfileChanges(username, email);
        }
    }

    private void saveProfileChanges(String username, String email) {
        executorService.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(requireContext());

                // Handle profile picture update
                if (profileImageUri != null) {
                    // Save new profile picture
                    String newProfilePictureUrl = saveImageToInternalStorage(profileImageUri, email);
                    if (!newProfilePictureUrl.isEmpty()) {
                        currentUser.profilePictureUrl = newProfilePictureUrl;
                    }
                }

                // Update user data
                currentUser.username = username;
                currentUser.email = email;

                db.userDao().updateUser(currentUser);

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();

                    if (profileUpdatedListener != null) {
                        profileUpdatedListener.onProfileUpdated(currentUser);
                    }

                    dismiss();
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String saveImageToInternalStorage(Uri imageUri, String email) {
        try {
            // Create directory if it doesn't exist
            File directory = new File(requireContext().getFilesDir(), "profile_pictures");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Generate unique filename
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = "profile_" + email.hashCode() + "_" + timeStamp + ".jpg";
            File file = new File(directory, filename);

            // Copy image to internal storage
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            OutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            // Delete old profile picture if exists
            if (currentUser.profilePictureUrl != null && !currentUser.profilePictureUrl.isEmpty()) {
                File oldFile = new File(currentUser.profilePictureUrl);
                if (oldFile.exists() && !oldFile.getAbsolutePath().equals(file.getAbsolutePath())) {
                    oldFile.delete();
                }
            }

            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}