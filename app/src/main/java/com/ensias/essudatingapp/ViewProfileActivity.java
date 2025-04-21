package com.ensias.essudatingapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ViewProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private TextView nameAgeTextView, courseTextView, genderTextView, bioTextView, hobbiesTextView, relationshipGoalsTextView;
    private Button likeButton, skipButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        // Get user ID from intent
        userId = getIntent().getStringExtra("userId");

        if (userId == null) {
            Toast.makeText(this, "Error: User ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUserId = mAuth.getCurrentUser().getUid();

        // Initialize UI elements
        profileImageView = findViewById(R.id.profile_image_view);
        nameAgeTextView = findViewById(R.id.name_age_text_view);
        courseTextView = findViewById(R.id.course_text_view);
        genderTextView = findViewById(R.id.gender_text_view);
        bioTextView = findViewById(R.id.bio_text_view);
        hobbiesTextView = findViewById(R.id.hobbies_text_view);
        relationshipGoalsTextView = findViewById(R.id.relationship_goals_text_view);
        likeButton = findViewById(R.id.like_button);
        skipButton = findViewById(R.id.skip_button);

        // Setup buttons
        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likeUser();
            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipUser();
            }
        });

        // Load user data
        loadUserData();
    }

    private void loadUserData() {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);

                    // Set profile image
                    if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                        Glide.with(ViewProfileActivity.this)
                                .load(user.getProfileImage())
                                .placeholder(R.drawable.default_profile)
                                .into(profileImageView);
                    }

                    // Set basic info
                    nameAgeTextView.setText(user.getFirstName() + " " + user.getLastName() + ", " + user.getAge());
                    courseTextView.setText(user.getCourse());
                    genderTextView.setText(user.getGender());

                    // Get profile info
                    DataSnapshot profileSnapshot = dataSnapshot.child("profile");
                    if (profileSnapshot.exists()) {
                        String bio = profileSnapshot.child("bio").getValue(String.class);
                        String hobbies = profileSnapshot.child("hobbies").getValue(String.class);
                        String relationshipGoals = profileSnapshot.child("relationshipGoals").getValue(String.class);

                        bioTextView.setText(bio != null ? bio : "No bio available");
                        hobbiesTextView.setText(hobbies != null ? hobbies : "No hobbies listed");
                        relationshipGoalsTextView.setText(relationshipGoals != null ? relationshipGoals : "Not specified");
                    } else {
                        bioTextView.setText("No bio available");
                        hobbiesTextView.setText("No hobbies listed");
                        relationshipGoalsTextView.setText("Not specified");
                    }

                    // Check if already interacted
                    mDatabase.child("users").child(currentUserId).child("interactions").child(userId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        // Already interacted, hide buttons
                                        likeButton.setVisibility(View.GONE);
                                        skipButton.setVisibility(View.GONE);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    // Do nothing
                                }
                            });
                } else {
                    Toast.makeText(ViewProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ViewProfileActivity.this, "Failed to load user data: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void likeUser() {
        // Save like to database
        Map<String, Object> likeData = new HashMap<>();
        likeData.put("type", "like");
        likeData.put("timestamp", System.currentTimeMillis());

        mDatabase.child("users").child(currentUserId).child("interactions").child(userId).setValue(likeData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Check if this is a mutual like (match)
                        checkForMatch();

                        // Hide buttons
                        likeButton.setVisibility(View.GONE);
                        skipButton.setVisibility(View.GONE);

                        Toast.makeText(ViewProfileActivity.this, "You liked this user!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ViewProfileActivity.this, "Failed to save like: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void skipUser() {
        // Save skip to database
        Map<String, Object> skipData = new HashMap<>();
        skipData.put("type", "skip");
        skipData.put("timestamp", System.currentTimeMillis());

        mDatabase.child("users").child(currentUserId).child("interactions").child(userId).setValue(skipData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Hide buttons
                        likeButton.setVisibility(View.GONE);
                        skipButton.setVisibility(View.GONE);

                        Toast.makeText(ViewProfileActivity.this, "You skipped this user", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ViewProfileActivity.this, "Failed to save skip: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkForMatch() {
        mDatabase.child("users").child(userId).child("interactions").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String interactionType = dataSnapshot.child("type").getValue(String.class);

                            if (interactionType != null && interactionType.equals("like")) {
                                // It's a match!
                                createMatch();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Do nothing
                    }
                });
    }

    private void createMatch() {
        // Create a unique match ID
        String matchId = mDatabase.child("matches").push().getKey();

        // Create match data
        Map<String, Object> matchData = new HashMap<>();
        matchData.put("user1", currentUserId);
        matchData.put("user2", userId);
        matchData.put("timestamp", System.currentTimeMillis());
        matchData.put("lastMessage", null);
        matchData.put("lastMessageTimestamp", null);

        // Save match to database
        mDatabase.child("matches").child(matchId).setValue(matchData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Add match reference to both users
                        mDatabase.child("users").child(currentUserId).child("matches").child(matchId).setValue(true);
                        mDatabase.child("users").child(userId).child("matches").child(matchId).setValue(true);

                        // Show match notification
                        Toast.makeText(ViewProfileActivity.this, "You have a new match!", Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
