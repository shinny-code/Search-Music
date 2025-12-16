package com.example.mymusicplayer.view;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.mymusicplayer.R;
import com.example.mymusicplayer.model.User;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.example.mymusicplayer.database.AppDatabase;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class RegisterDialogFragment extends DialogFragment {
    private TextInputEditText etUsername, etEmail, etPassword, etConfirmPassword;
    private TextInputLayout tilUsername, tilEmail, tilPassword, tilConfirmPassword;
    private CircleImageView profileImage;
    private TextView btnUploadPhoto;
    private Button btnRegister;
    private OnRegisterSuccessListener registerSuccessListener;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Uri profileImageUri = null;
    private static final int IMAGE_PICKER_REQUEST_CODE = 100;

    public interface OnRegisterSuccessListener {
        void onRegisterSuccess(String email, String password);
    }

    public void setOnRegisterSuccessListener(OnRegisterSuccessListener listener) {
        this.registerSuccessListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_register_dialog, null);

        initializeViews(view);
        setupListeners();

        builder.setView(view)
                .setNegativeButton("Cancel", (dialog, which) -> dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negativeButton != null) {
                negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                negativeButton.setAllCaps(false);
                negativeButton.setTextSize(16);
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        return dialog;
    }

    private void initializeViews(View view) {
        profileImage = view.findViewById(R.id.ivProfilePicture);
        btnUploadPhoto = view.findViewById(R.id.btnUploadPhoto);

        tilUsername = view.findViewById(R.id.tilUsername);
        tilEmail = view.findViewById(R.id.tilEmail);
        tilPassword = view.findViewById(R.id.tilPassword);
        tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword);

        etUsername = view.findViewById(R.id.etUsername);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        btnRegister = view.findViewById(R.id.btnRegister);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> {
            if (validateForm()) {
                attemptRegistration();
            }
        });

        btnUploadPhoto.setOnClickListener(v -> openImagePicker());

        profileImage.setOnClickListener(v -> openImagePicker());
    }

    private void openImagePicker() {
        ImagePicker.with(this)
                .crop()                    // Crop image
                .compress(1024)            // Final image size will be less than 1 MB
                .maxResultSize(1080, 1080) // Final image resolution
                .start(IMAGE_PICKER_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_PICKER_REQUEST_CODE && data != null) {
            profileImageUri = data.getData();
            if (profileImageUri != null) {
                profileImage.setImageURI(profileImageUri);
            }
        }
    }

    private boolean validateForm() {
        boolean isValid = validateUsername();

        if (!validateEmail()) {
            isValid = false;
        }

        if (!validatePassword()) {
            isValid = false;
        }

        if (!validateConfirmPassword()) {
            isValid = false;
        }

        return isValid;
    }

    private boolean validateUsername() {
        String username = etUsername.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            tilUsername.setError("Username is required");
            return false;
        } else if (username.length() < 3) {
            tilUsername.setError("Username must be at least 3 characters");
            return false;
        } else {
            tilUsername.setError(null);
            tilUsername.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateEmail() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email");
            return false;
        } else {
            tilEmail.setError(null);
            tilEmail.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validatePassword() {
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            return false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            return false;
        } else {
            tilPassword.setError(null);
            tilPassword.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateConfirmPassword() {
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Please confirm your password");
            return false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            return false;
        } else {
            tilConfirmPassword.setError(null);
            tilConfirmPassword.setErrorEnabled(false);
            return true;
        }
    }

    private void attemptRegistration() {
        final String username = etUsername.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        executorService.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(requireContext());

                User existingUser = db.userDao().getUserByEmail(email);
                if (existingUser != null) {
                    requireActivity().runOnUiThread(() -> {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Create Account");
                        tilEmail.setError("Email already registered");
                        etEmail.requestFocus();
                    });
                    return;
                }

                List<User> allUsers = db.userDao().getAllUsers();
                boolean usernameExists = false;
                for (User user : allUsers) {
                    if (user.username.equalsIgnoreCase(username)) {
                        usernameExists = true;
                        break;
                    }
                }

                if (usernameExists) {
                    requireActivity().runOnUiThread(() -> {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Create Account");
                        tilUsername.setError("Username already taken");
                        etUsername.requestFocus();
                    });
                    return;
                }

                String profilePictureUrl = "";
                if (profileImageUri != null) {
                    profilePictureUrl = saveImageToInternalStorage(profileImageUri, email);
                }

                User newUser = new User();
                newUser.username = username;
                newUser.email = email;
                newUser.password = password;
                newUser.profilePictureUrl = profilePictureUrl;
                newUser.isLoggedIn = false;

                long userId = db.userDao().insertUser(newUser);
                newUser.userId = (int) userId;

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            "Account created successfully!",
                            Toast.LENGTH_SHORT).show();

                    if (registerSuccessListener != null) {
                        registerSuccessListener.onRegisterSuccess(email, password);
                    }
                    dismiss();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Create Account");
                    Toast.makeText(requireContext(),
                            "Registration failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String saveImageToInternalStorage(Uri imageUri, String email) {
        try {
            File directory = new File(requireContext().getFilesDir(), "profile_pictures");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String filename = "profile_" + email.hashCode() + ".jpg";
            File file = new File(directory, filename);

            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                    requireContext().getContentResolver(),
                    imageUri
            );

            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tilUsername != null) tilUsername.setError(null);
        if (tilEmail != null) tilEmail.setError(null);
        if (tilPassword != null) tilPassword.setError(null);
        if (tilConfirmPassword != null) tilConfirmPassword.setError(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}