package com.example.mymusicplayer.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.mymusicplayer.R;
import com.example.mymusicplayer.model.User;
import com.example.mymusicplayer.database.AppDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginDialogFragment extends DialogFragment {
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private OnLoginSuccessListener loginSuccessListener;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface OnLoginSuccessListener {
        void onLoginSuccess();
    }

    public void setOnLoginSuccessListener(OnLoginSuccessListener listener) {
        this.loginSuccessListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_login_dialog, null);

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnRegister = view.findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v -> showRegisterDialog());

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

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(), "Logging in...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(requireContext());

                db.userDao().clearLoggedInUsers();

                // Check if user exists
                User user = db.userDao().getUserByEmailAndPassword(email, password);

                requireActivity().runOnUiThread(() -> {
                    if (user != null) {
                        executorService.execute(() -> {
                            user.isLoggedIn = true;
                            db.userDao().updateUser(user);

                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show();
                                if (loginSuccessListener != null) {
                                    loginSuccessListener.onLoginSuccess();
                                }
                                dismiss();
                            });
                        });
                    } else {
                        Toast.makeText(requireContext(), "Invalid email or password", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showRegisterDialog() {
        RegisterDialogFragment registerDialog = new RegisterDialogFragment();
        registerDialog.setOnRegisterSuccessListener((email, password) -> {
            // Auto-fill the login fields with registered credentials
            if (email != null) etEmail.setText(email);
            if (password != null) etPassword.setText(password);
            Toast.makeText(requireContext(), "Registration successful! Please login.", Toast.LENGTH_SHORT).show();
        });
        registerDialog.show(getParentFragmentManager(), "register_dialog");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}