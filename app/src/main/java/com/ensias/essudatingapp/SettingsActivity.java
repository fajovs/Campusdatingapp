package com.ensias.essudatingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private Switch notificationsSwitch, locationSwitch, darkModeSwitch;
    private Button editPreferencesButton, deleteAccountButton, logoutButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUserId = mAuth.getCurrentUser().getUid();

        // Initialize UI elements
        notificationsSwitch = findViewById(R.id.notifications_switch);
        locationSwitch = findViewById(R.id.location_switch);
        darkModeSwitch = findViewById(R.id.dark_mode_switch);
        editPreferencesButton = findViewById(R.id.edit_preferences_button);
        deleteAccountButton = findViewById(R.id.delete_account_button);
        logoutButton = findViewById(R.id.logout_button);

        // Load current settings
        loadSettings();

        // Setup switch listeners
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSettingToFirebase("notifications", isChecked);
        });

        locationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSettingToFirebase("location", isChecked);
        });

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSettingToFirebase("darkMode", isChecked);
            // In a real app, you would apply the theme change here
        });

        // Setup button listeners
        editPreferencesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, PreferencesActivity.class);
                startActivity(intent);
            }
        });

        deleteAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAccount();
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void loadSettings() {
        mDatabase.child("users").child(currentUserId).child("settings")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Load notification setting
                            if (dataSnapshot.child("notifications").exists()) {
                                boolean notifications = dataSnapshot.child("notifications").getValue(Boolean.class);
                                notificationsSwitch.setChecked(notifications);
                            }

                            // Load location setting
                            if (dataSnapshot.child("location").exists()) {
                                boolean location = dataSnapshot.child("location").getValue(Boolean.class);
                                locationSwitch.setChecked(location);
                            }

                            // Load dark mode setting
                            if (dataSnapshot.child("darkMode").exists()) {
                                boolean darkMode = dataSnapshot.child("darkMode").getValue(Boolean.class);
                                darkModeSwitch.setChecked(darkMode);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(SettingsActivity.this, "Failed to load settings: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveSettingToFirebase(String setting, boolean value) {
        mDatabase.child("users").child(currentUserId).child("settings").child(setting).setValue(value)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(SettingsActivity.this, "Failed to save setting: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void deleteAccount() {
        // In a real app, you would show a confirmation dialog here

        // Delete user data from Firebase Database
        mDatabase.child("users").child(currentUserId).removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Delete user authentication
                            mAuth.getCurrentUser().delete()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(SettingsActivity.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Toast.makeText(SettingsActivity.this, "Failed to delete account: " + task.getException().getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        } else {
                            Toast.makeText(SettingsActivity.this, "Failed to delete user data: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}