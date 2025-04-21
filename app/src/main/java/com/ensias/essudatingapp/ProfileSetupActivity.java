package com.ensias.essudatingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {

    private EditText bioEditText;
    private EditText hobbiesEditText;
    private Spinner relationshipGoalsSpinner;
    private Button saveButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI elements
        bioEditText = findViewById(R.id.bio_edit_text);
        hobbiesEditText = findViewById(R.id.hobbies_edit_text);
        relationshipGoalsSpinner = findViewById(R.id.relationship_goals_spinner);
        saveButton = findViewById(R.id.save_button);

        // Setup relationship goals spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.relationship_goals_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        relationshipGoalsSpinner.setAdapter(adapter);

        // Setup save button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileInfo();
            }
        });
    }

    private void saveProfileInfo() {
        String bio = bioEditText.getText().toString().trim();
        String hobbies = hobbiesEditText.getText().toString().trim();
        String relationshipGoals = relationshipGoalsSpinner.getSelectedItem().toString();

        // Validate inputs
        if (bio.isEmpty() || hobbies.isEmpty()) {
            Toast.makeText(ProfileSetupActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        // Create profile data map
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("bio", bio);
        profileData.put("hobbies", hobbies);
        profileData.put("relationshipGoals", relationshipGoals);

        // Save to Firebase
        mDatabase.child("users").child(userId).child("profile").setValue(profileData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ProfileSetupActivity.this, "Profile setup successful!", Toast.LENGTH_SHORT).show();
                            // Redirect to preferences setup
                            Intent intent = new Intent(ProfileSetupActivity.this, PreferencesActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(ProfileSetupActivity.this, "Failed to save profile: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
