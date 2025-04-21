package com.ensias.essudatingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class HomeActivity extends AppCompatActivity {

    private TextView nameAgeTextView, courseTextView, bioTextView, hobbiesTextView, noMatchesTextView;
    private ImageView profileImageView;
    private Button likeButton, skipButton, viewProfileButton;
    private View userCardView;

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Find Matches");

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUserId = mAuth.getCurrentUser().getUid();

        // UI references
        profileImageView = findViewById(R.id.profile_image_view);
        nameAgeTextView = findViewById(R.id.name_age_text_view);
        courseTextView = findViewById(R.id.course_text_view);
        bioTextView = findViewById(R.id.bio_text_view);
        hobbiesTextView = findViewById(R.id.hobbies_text_view);
        viewProfileButton = findViewById(R.id.view_profile_button);
        likeButton = findViewById(R.id.like_button);
        skipButton = findViewById(R.id.skip_button);
        noMatchesTextView = findViewById(R.id.no_matches_text_view);
        userCardView = findViewById(R.id.user_card_view);

        // Buttons
        likeButton.setOnClickListener(v -> {
            if (displayedUser != null) likeUser(displayedUser.getId());
        });

        skipButton.setOnClickListener(v -> {
            if (displayedUser != null) skipUser(displayedUser.getId());
        });

        viewProfileButton.setOnClickListener(v -> {
            if (displayedUser != null) {
                Intent intent = new Intent(HomeActivity.this, ViewProfileActivity.class);
                intent.putExtra("userId", displayedUser.getId());
                startActivity(intent);
            }
        });

        potentialMatches = new ArrayList<>();
        loadCurrentUserData();
    }

    private void loadCurrentUserData() {
        mDatabase.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentUser = snapshot.getValue(User.class);
                    currentUser.setId(currentUserId);
                    loadPotentialMatches();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Error loading user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPotentialMatches() {
        potentialMatches.clear();
        currentIndex = -1;

        mDatabase.child("users").child(currentUserId).child("preferences")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int minAge = 18;
                        int maxAge = 30;
                        String genderPreference = "Any";
                        String coursePreference = "";

                        if (snapshot.exists()) {
                            Long minAgeLong = snapshot.child("minAge").getValue(Long.class);
                            if (minAgeLong != null) minAge = minAgeLong.intValue();

                            Long maxAgeLong = snapshot.child("maxAge").getValue(Long.class);
                            if (maxAgeLong != null) maxAge = maxAgeLong.intValue();

                            String genderPref = snapshot.child("genderPreference").getValue(String.class);
                            if (genderPref != null) genderPreference = genderPref;

                            String coursePref = snapshot.child("coursePreference").getValue(String.class);
                            if (coursePref != null) coursePreference = coursePref;
                        }

                        Query usersQuery = mDatabase.child("users");
                        int finalMinAge = minAge;
                        int finalMaxAge = maxAge;
                        String finalGenderPreference = genderPreference;
                        String finalCoursePreference = coursePreference;

                        usersQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                    String userId = userSnapshot.getKey();
                                    if (userId.equals(currentUserId)) continue;

                                    User user = userSnapshot.getValue(User.class);
                                    if (user == null) continue;
                                    user.setId(userId);

                                    mDatabase.child("users").child(currentUserId).child("interactions").child(userId)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot interactionSnapshot) {
                                                    if (!interactionSnapshot.exists()) {
                                                        int age = user.getAge();
                                                        String gender = user.getGender();
                                                        String course = user.getCourse();

                                                        boolean ageOK = age >= finalMinAge && age <= finalMaxAge;
                                                        boolean genderOK = finalGenderPreference.equalsIgnoreCase("Any") ||
                                                                finalGenderPreference.equalsIgnoreCase(gender);
                                                        boolean courseOK = finalCoursePreference.isEmpty() ||
                                                                finalCoursePreference.equalsIgnoreCase(course);

                                                        if (ageOK && genderOK && courseOK) {
                                                            potentialMatches.add(userId);
                                                            if (currentIndex == -1) {
                                                                currentIndex = 0;
                                                                displayUser(userId);
                                                            }
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {}
                                            });
                                }

                                if (potentialMatches.isEmpty()) {
                                    showNextUser(); // will trigger the "no users" view
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(HomeActivity.this, "Failed to load users.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(HomeActivity.this, "Failed to load preferences.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayUser(String userId) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    displayedUser = snapshot.getValue(User.class);
                    displayedUser.setId(userId);

                    // Load profile image
                    if (displayedUser.getProfileImage() != null && !displayedUser.getProfileImage().isEmpty()) {
                        Glide.with(HomeActivity.this)
                                .load(displayedUser.getProfileImage())
                                .placeholder(R.drawable.default_profile)
                                .into(profileImageView);
                    } else {
                        profileImageView.setImageResource(R.drawable.default_profile);
                    }

                    // Set basic info
                    nameAgeTextView.setText(displayedUser.getFirstName() + " " + displayedUser.getLastName() + ", " + displayedUser.getAge());
                    courseTextView.setText(displayedUser.getCourse());

                    // ✅ Get nested profile info
                    DataSnapshot profileSnapshot = snapshot.child("profile");

                    String bio = profileSnapshot.child("bio").getValue(String.class);
                    String hobbies = profileSnapshot.child("hobbies").getValue(String.class);

                    bioTextView.setText(bio != null ? bio : "No bio provided");
                    hobbiesTextView.setText(hobbies != null ? hobbies : "No hobbies listed");

                    // Show profile card and controls
                    userCardView.setVisibility(View.VISIBLE);
                    likeButton.setVisibility(View.VISIBLE);
                    skipButton.setVisibility(View.VISIBLE);
                    viewProfileButton.setVisibility(View.VISIBLE);
                    noMatchesTextView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Failed to load user profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showNextUser() {
        currentIndex++;

        if (currentIndex < potentialMatches.size()) {
            displayUser(potentialMatches.get(currentIndex));
        } else {
            displayedUser = null;

            // Hide all profile views
            userCardView.setVisibility(View.GONE);
            likeButton.setVisibility(View.GONE);
            skipButton.setVisibility(View.GONE);
            viewProfileButton.setVisibility(View.GONE);

            // Show message
            noMatchesTextView.setVisibility(View.VISIBLE);
        }
    }

    private void likeUser(String userId) {
        Map<String, Object> likeData = new HashMap<>();
        likeData.put("type", "like");
        likeData.put("timestamp", System.currentTimeMillis());

        mDatabase.child("users").child(currentUserId).child("interactions").child(userId)
                .setValue(likeData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkForMatch(userId);
                        showNextUser();
                    }
                });
    }

    private void skipUser(String userId) {
        Map<String, Object> skipData = new HashMap<>();
        skipData.put("type", "skip");
        skipData.put("timestamp", System.currentTimeMillis());

        mDatabase.child("users").child(currentUserId).child("interactions").child(userId)
                .setValue(skipData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showNextUser();
                    }
                });
    }

    private void checkForMatch(String userId) {
        mDatabase.child("users").child(userId).child("interactions").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String type = snapshot.child("type").getValue(String.class);
                            if ("like".equals(type)) {
                                createMatch(userId);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void createMatch(String matchedUserId) {
        String matchId = mDatabase.child("matches").push().getKey();

        Map<String, Object> matchData = new HashMap<>();
        matchData.put("user1", currentUserId);
        matchData.put("user2", matchedUserId);
        matchData.put("timestamp", System.currentTimeMillis());

        mDatabase.child("matches").child(matchId).setValue(matchData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mDatabase.child("users").child(currentUserId).child("matches").child(matchId).setValue(true);
                        mDatabase.child("users").child(matchedUserId).child("matches").child(matchId).setValue(true);
                        Toast.makeText(this, "You have a new match!", Toast.LENGTH_LONG).show();
                    }
                });
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
            startActivity(new Intent(this, MatchesActivity.class));
            return true;
        } else if (id == R.id.action_edit_profile) {
            startActivity(new Intent(this, EditProfileActivity.class));
            return true;
        } else if (id == R.id.action_preferences) {
            startActivity(new Intent(this, PreferencesActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
