package com.ensias.essudatingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
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

public class PreferencesActivity extends AppCompatActivity {

    private SeekBar minAgeSeekBar, maxAgeSeekBar;
    private TextView minAgeTextView, maxAgeTextView;
    private Spinner genderPreferenceSpinner, coursePreferenceSpinner;
    private MultiAutoCompleteTextView hobbiesPreferenceTextView;
    private Button saveButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI elements
        minAgeSeekBar = findViewById(R.id.min_age_seek_bar);
        maxAgeSeekBar = findViewById(R.id.max_age_seek_bar);
        minAgeTextView = findViewById(R.id.min_age_text_view);
        maxAgeTextView = findViewById(R.id.max_age_text_view);
        genderPreferenceSpinner = findViewById(R.id.gender_preference_spinner);
        coursePreferenceSpinner = findViewById(R.id.course_preference_spinner);
        hobbiesPreferenceTextView = findViewById(R.id.hobbies_preference);
        saveButton = findViewById(R.id.save_button);

        // Setup gender preference spinner
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.gender_preference_array, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderPreferenceSpinner.setAdapter(genderAdapter);

        // Setup course preference spinner
        ArrayAdapter<CharSequence> courseAdapter = ArrayAdapter.createFromResource(this,
                R.array.courses_array, android.R.layout.simple_spinner_item);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        coursePreferenceSpinner.setAdapter(courseAdapter);

        // Setup hobbies preference with auto-complete
        String[] hobbies = getResources().getStringArray(R.array.hobbies_array);
        ArrayAdapter<String> hobbiesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, hobbies);
        hobbiesPreferenceTextView.setAdapter(hobbiesAdapter);
        hobbiesPreferenceTextView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        // Setup age range seek bars
        setupAgeSeekBars();

        // Setup save button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferences();
            }
        });
    }

    private void setupAgeSeekBars() {
        // Set initial values
        minAgeSeekBar.setProgress(18);
        maxAgeSeekBar.setProgress(30);
        updateAgeTextViews();

        // Set listeners
        minAgeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 18) {
                    seekBar.setProgress(18);
                } else if (progress > maxAgeSeekBar.getProgress()) {
                    seekBar.setProgress(maxAgeSeekBar.getProgress());
                }
                updateAgeTextViews();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        maxAgeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < minAgeSeekBar.getProgress()) {
                    seekBar.setProgress(minAgeSeekBar.getProgress());
                }
                updateAgeTextViews();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateAgeTextViews() {
        minAgeTextView.setText(String.valueOf(minAgeSeekBar.getProgress()));
        maxAgeTextView.setText(String.valueOf(maxAgeSeekBar.getProgress()));
    }

    private void savePreferences() {
        int minAge = minAgeSeekBar.getProgress();
        int maxAge = maxAgeSeekBar.getProgress();
        String genderPreference = genderPreferenceSpinner.getSelectedItem().toString();
        String coursePreference = coursePreferenceSpinner.getSelectedItem().toString();
        String hobbiesPreference = hobbiesPreferenceTextView.getText().toString().trim();

        String userId = mAuth.getCurrentUser().getUid();

        // Create preferences data map
        Map<String, Object> preferencesData = new HashMap<>();
        preferencesData.put("minAge", minAge);
        preferencesData.put("maxAge", maxAge);
        preferencesData.put("genderPreference", genderPreference);
        preferencesData.put("coursePreference", coursePreference);
        preferencesData.put("hobbiesPreference", hobbiesPreference);

        // Save to Firebase
        mDatabase.child("users").child(userId).child("preferences").setValue(preferencesData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(PreferencesActivity.this, "Preferences saved successfully!", Toast.LENGTH_SHORT).show();
                            // Redirect to home activity
                            Intent intent = new Intent(PreferencesActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(PreferencesActivity.this, "Failed to save preferences: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}