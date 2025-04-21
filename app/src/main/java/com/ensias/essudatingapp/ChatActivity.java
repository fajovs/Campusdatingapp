package com.ensias.essudatingapp;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView messagesRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;

    private MessageAdapter messageAdapter;
    private List<Message> messages;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;
    private String matchId;
    private String otherUserId;
    private String otherUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get intent extras
        matchId = getIntent().getStringExtra("matchId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        otherUserName = getIntent().getStringExtra("otherUserName");

        if (matchId == null || otherUserId == null || otherUserName == null) {
            Toast.makeText(this, "Error: Missing data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(otherUserName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUserId = mAuth.getCurrentUser().getUid();

        // Initialize UI elements
        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);

        // Setup RecyclerView
        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messages, currentUserId);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messageAdapter);

        // Setup send button
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        // Load messages
        loadMessages();
    }

    private void loadMessages() {
        mDatabase.child("matches").child(matchId).child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        messages.clear();

                        for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                            Message message = messageSnapshot.getValue(Message.class);
                            messages.add(message);
                        }

                        messageAdapter.notifyDataSetChanged();

                        // Scroll to bottom
                        if (messages.size() > 0) {
                            messagesRecyclerView.smoothScrollToPosition(messages.size() - 1);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ChatActivity.this, "Failed to load messages: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();

        if (messageText.isEmpty()) {
            return;
        }

        // Create message
        Message message = new Message(
                currentUserId,
                otherUserId,
                messageText,
                System.currentTimeMillis()
        );

        // Add message to database
        String messageId = mDatabase.child("matches").child(matchId).child("messages").push().getKey();
        mDatabase.child("matches").child(matchId).child("messages").child(messageId).setValue(message);

        // Update match with last message
        Map<String, Object> matchUpdates = new HashMap<>();
        matchUpdates.put("lastMessage", messageText);
        matchUpdates.put("lastMessageTimestamp", message.getTimestamp());
        mDatabase.child("matches").child(matchId).updateChildren(matchUpdates);

        // Clear input
        messageEditText.setText("");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
