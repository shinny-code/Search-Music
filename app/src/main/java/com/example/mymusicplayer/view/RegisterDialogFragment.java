package com.example.mymusicplayer.view;

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

import com.example.mymusicplayer.R;
import com.example.mymusicplayer.model.User;
import com.example.mymusicplayer.database.AppDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterDialogFragment extends DialogFragment {
    private EditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private OnRegisterSuccessListener registerSuccessListener;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

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

        etUsername = view.findViewById(R.id.etUsername);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        btnRegister = view.findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> attemptRegistration());

        builder.setView(view)
                .setTitle("Create Account")
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dismiss();
                });

        return builder.create();
    }

    private void attemptRegistration() {
        final String username = etUsername.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        final String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        Toast.makeText(requireContext(), "Registering...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(requireContext());

                // Check if email already exists
                User existingUser = db.userDao().getUserByEmail(email);
                if (existingUser != null) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Email already registered", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Create new user
                User newUser = new User();
                newUser.username = username;
                newUser.email = email;
                newUser.password = password;
                newUser.isLoggedIn = false; // Don't auto-login
                newUser.profilePictureUrl = "";

                long userId = db.userDao().insertUser(newUser);
                newUser.userId = (int) userId;

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_SHORT).show();
                    if (registerSuccessListener != null) {
                        registerSuccessListener.onRegisterSuccess(email, password);
                    }
                    dismiss();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}