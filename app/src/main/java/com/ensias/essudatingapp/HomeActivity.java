package com.ensias.essudatingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private TextView nameAgeTextView, courseTextView;
    private Button likeButton, skipButton, viewProfileButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;
    private User currentUser;
    private User displayedUser;
    private List<String> potentialMatches;
    private int currentIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Find Matches");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUserId = mAuth.getCurrentUser().getUid();

        // Initialize UI elements
        profileImageView = findViewById(R.id.profile_image_view);
        nameAgeTextView = findViewById(R.id.name_age_text_view);
        courseTextView = findViewById(R.id.course_text_view);
        likeButton = findViewById(R.id.like_button);
        skipButton = findViewById(R.id.skip_button);
        viewProfileButton = findViewById(R.id.view_profile_button);

        // Setup buttons
        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (displayedUser != null) {
                    likeUser(displayedUser.getId());
                }
            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (displayedUser != null) {
                    skipUser(displayedUser.getId());
                }
            }
        });

        viewProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (displayedUser != null) {
                    Intent intent = new Intent(HomeActivity.this, ViewProfileActivity.class);
                    intent.putExtra("userId", displayedUser.getId());
                    startActivity(intent);
                }
            }
        });

        // Initialize potential matches list
        potentialMatches = new ArrayList<>();

        // Load current user data
        loadCurrentUserData();
    }

    private void loadCurrentUserData() {
        mDatabase.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    currentUser = dataSnapshot.getValue(User.class);
                    currentUser.setId(currentUserId);

                    // Load potential matches
                    loadPotentialMatches();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(HomeActivity.this, "Failed to load user data: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPotentialMatches() {
        // Clear previous list
        potentialMatches.clear();
        currentIndex = -1;

        // Get user preferences
        mDatabase.child("users").child(currentUserId).child("preferences")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // Default preferences if none set
                        int minAge = 18;
                        int maxAge = 30;
                        String genderPreference = "Any";
                        Map<String, Boolean> coursePreferences = new HashMap<>();

                        if (dataSnapshot.exists()) {
                            if (dataSnapshot.child("minAge").exists()) {
                                minAge = dataSnapshot.child("minAge").getValue(Integer.class);
                            }

                            if (dataSnapshot.child("maxAge").exists()) {
                                maxAge = dataSnapshot.child("maxAge").getValue(Integer.class);
                            }

                            if (dataSnapshot.child("genderPreference").exists()) {
                                genderPreference = dataSnapshot.child("genderPreference").getValue(String.class);
                            }

                            if (dataSnapshot.child("coursePreferences").exists()) {
                                for (DataSnapshot courseSnapshot : dataSnapshot.child("coursePreferences").getChildren()) {
                                    String course = courseSnapshot.getKey();
                                    boolean selected = courseSnapshot.getValue(Boolean.class);
                                    if (selected) {
                                        coursePreferences.put(course, true);
                                    }
                                }
                            }
                        }

                        // Query users based on preferences
                        Query usersQuery = mDatabase.child("users");

                        usersQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                    String userId = userSnapshot.getKey();

                                    // Skip current user
                                    if (userId.equals(currentUserId)) {
                                        continue;
                                    }

                                    User user = userSnapshot.getValue(User.class);
                                    user.setId(userId);

                                    // Check if already interacted with this user
                                    mDatabase.child("users").child(currentUserId).child("interactions").child(userId)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot interactionSnapshot) {
                                                    if (!interactionSnapshot.exists()) {
                                                        // Check age preference
                                                        int userAge = user.getAge();
                                                        if (userAge >= minAge && userAge <= maxAge) {
                                                            // Check gender preference
                                                            if (genderPreference.equals("Any") || genderPreference.equals(user.getGender())) {
                                                                // Check course preference if any
                                                                if (coursePreferences.isEmpty() || coursePreferences.containsKey(user.getCourse())) {
                                                                    // Add to potential matches
                                                                    potentialMatches.add(userId);

                                                                    // If this is the first match, display it
                                                                    if (currentIndex == -1) {
                                                                        currentIndex = 0;
                                                                        displayUser(userId);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                                    // Do nothing
                                                }
                                            });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(HomeActivity.this, "Failed to load users: " + databaseError.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(HomeActivity.this, "Failed to load preferences: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayUser(String userId) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    displayedUser = dataSnapshot.getValue(User.class);
                    displayedUser.setId(userId);

                    // Set profile image
                    if (displayedUser.getProfileImage() != null && !displayedUser.getProfileImage().isEmpty()) {
                        Glide.with(HomeActivity.this)
                                .load(displayedUser.getProfileImage())
                                .placeholder(R.drawable.default_profile)
                                .into(profileImageView);
                    } else {
                        profileImageView.setImageResource(R.drawable.default_profile);
                    }

                    // Set user info
                    nameAgeTextView.setText(displayedUser.getFirstName() + " " + displayedUser.getLastName() + ", " + displayedUser.getAge());
                    courseTextView.setText(displayedUser.getCourse());

                    // Show buttons
                    likeButton.setVisibility(View.VISIBLE);
                    skipButton.setVisibility(View.VISIBLE);
                    viewProfileButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(HomeActivity.this, "Failed to load user data: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void likeUser(String userId) {
        // Save like to database
        Map<String, Object> likeData = new HashMap<>();
        likeData.put("type", "like");
        likeData.put("timestamp", System.currentTimeMillis());

        mDatabase.child("users").child(currentUserId).child("interactions").child(userId).setValue(likeData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Check if this is a mutual like (match)
                        checkForMatch(userId);

                        // Show next user
                        showNextUser();
                    } else {
                        Toast.makeText(HomeActivity.this, "Failed to save like: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void skipUser(String userId) {
        // Save skip to database
        Map<String, Object> skipData = new HashMap<>();
        skipData.put("type", "skip");
        skipData.put("timestamp", System.currentTimeMillis());

        mDatabase.child("users").child(currentUserId).child("interactions").child(userId).setValue(skipData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Show next user
                        showNextUser();
                    } else {
                        Toast.makeText(HomeActivity.this, "Failed to save skip: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkForMatch(String userId) {
        mDatabase.child("users").child(userId).child("interactions").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String interactionType = dataSnapshot.child("type").getValue(String.class);

                            if (interactionType != null && interactionType.equals("like")) {
                                // It's a match!
                                createMatch(userId);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Do nothing
                    }
                });
    }

    private void createMatch(String matchedUserId) {
        // Create a unique match ID
        String matchId = mDatabase.child("matches").push().getKey();

        // Create match data
        Map<String, Object> matchData = new HashMap<>();
        matchData.put("user1", currentUserId);
        matchData.put("user2", matchedUserId);
        matchData.put("timestamp", System.currentTimeMillis());
        matchData.put("lastMessage", null);
        matchData.put("lastMessageTimestamp", null);

        // Save match to database
        mDatabase.child("matches").child(matchId).setValue(matchData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Add match reference to both users
                        mDatabase.child("users").child(currentUserId).child("matches").child(matchId).setValue(true);
                        mDatabase.child("users").child(matchedUserId).child("matches").child(matchId).setValue(true);

                        // Show match notification
                        Toast.makeText(HomeActivity.this, "You have a new match!", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showNextUser() {
        // Increment index
        currentIndex++;

        // Check if there are more users
        if (currentIndex < potentialMatches.size()) {
            // Display next user
            displayUser(potentialMatches.get(currentIndex));
        } else {
            // No more users to display
            displayedUser = null;
            profileImageView.setImageResource(R.drawable.default_profile);
            nameAgeTextView.setText("No more matches");
            courseTextView.setText("Try again later or adjust your preferences");

            // Hide buttons
            likeButton.setVisibility(View.GONE);
            skipButton.setVisibility(View.GONE);
            viewProfileButton.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_matches) {
            Intent intent = new Intent(HomeActivity.this, MatchesActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_edit_profile) {
            Intent intent = new Intent(HomeActivity.this, EditProfileActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_preferences) {
            Intent intent = new Intent(HomeActivity.this, PreferencesActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}