package com.ensias.essudatingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MatchesActivity extends AppCompatActivity implements MatchAdapter.OnMatchClickListener {

    private RecyclerView matchesRecyclerView;
    private TextView noMatchesTextView;
    private MatchAdapter matchAdapter;
    private List<Match> matches;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matches);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Your Matches");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUserId = mAuth.getCurrentUser().getUid();

        // Initialize UI elements
        matchesRecyclerView = findViewById(R.id.matches_recycler_view);
        noMatchesTextView = findViewById(R.id.no_matches_text_view);

        // Setup RecyclerView
        matches = new ArrayList<>();
        matchAdapter = new MatchAdapter(this, matches, this);
        matchesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        matchesRecyclerView.setAdapter(matchAdapter);

        // Load matches
        loadMatches();
    }

    private void loadMatches() {
        mDatabase.child("users").child(currentUserId).child("matches")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                            matches.clear();

                            for (DataSnapshot matchSnapshot : dataSnapshot.getChildren()) {
                                String matchId = matchSnapshot.getKey();

                                mDatabase.child("matches").child(matchId)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot matchDataSnapshot) {
                                                if (matchDataSnapshot.exists()) {
                                                    Match match = matchDataSnapshot.getValue(Match.class);
                                                    match.setId(matchId);

                                                    // Get the other user's ID
                                                    String otherUserId;
                                                    if (match.getUser1().equals(currentUserId)) {
                                                        otherUserId = match.getUser2();
                                                    } else {
                                                        otherUserId = match.getUser1();
                                                    }

                                                    // Get the other user's info
                                                    mDatabase.child("users").child(otherUserId)
                                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                                                    if (userSnapshot.exists()) {
                                                                        User user = userSnapshot.getValue(User.class);
                                                                        user.setId(otherUserId);
                                                                        match.setOtherUser(user);

                                                                        // Add to list and update adapter
                                                                        matches.add(match);
                                                                        matchAdapter.notifyDataSetChanged();

                                                                        // Show/hide no matches text
                                                                        updateUI();
                                                                    }
                                                                }

                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                                                    Toast.makeText(MatchesActivity.this, "Failed to load user data: " + databaseError.getMessage(),
                                                                            Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                Toast.makeText(MatchesActivity.this, "Failed to load match data: " + databaseError.getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            matches.clear();
                            matchAdapter.notifyDataSetChanged();
                            updateUI();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(MatchesActivity.this, "Failed to load matches: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI() {
        if (matches.isEmpty()) {
            noMatchesTextView.setVisibility(View.VISIBLE);
            matchesRecyclerView.setVisibility(View.GONE);
        } else {
            noMatchesTextView.setVisibility(View.GONE);
            matchesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMatchClick(int position) {
        Match match = matches.get(position);

        // Open chat activity
        Intent intent = new Intent(MatchesActivity.this, ChatActivity.class);
        intent.putExtra("matchId", match.getId());
        intent.putExtra("otherUserId", match.getOtherUser().getId());
        intent.putExtra("otherUserName", match.getOtherUser().getFirstName() + " " + match.getOtherUser().getLastName());
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
