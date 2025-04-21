package com.ensias.essudatingapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView profileImageView;
    private EditText firstNameEditText, lastNameEditText, bioEditText, hobbiesEditText;
    private Spinner courseSpinner, relationshipGoalsSpinner;
    private Button uploadImageButton, saveButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;
    private Uri imageUri;
    private String currentProfileImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Edit Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUserId = mAuth.getCurrentUser().getUid();

        // Initialize UI elements
        profileImageView = findViewById(R.id.profile_image_view);
        firstNameEditText = findViewById(R.id.first_name_edit_text);
        lastNameEditText = findViewById(R.id.last_name_edit_text);
        bioEditText = findViewById(R.id.bio_edit_text);
        hobbiesEditText = findViewById(R.id.hobbies_edit_text);
        courseSpinner = findViewById(R.id.course_spinner);
        relationshipGoalsSpinner = findViewById(R.id.relationship_goals_spinner);
        uploadImageButton = findViewById(R.id.upload_image_button);
        saveButton = findViewById(R.id.save_button);

        // Setup course spinner
        ArrayAdapter<CharSequence> courseAdapter = ArrayAdapter.createFromResource(this,
                R.array.courses_array, android.R.layout.simple_spinner_item);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        courseSpinner.setAdapter(courseAdapter);

        // Setup relationship goals spinner
        ArrayAdapter<CharSequence> relationshipAdapter = ArrayAdapter.createFromResource(this,
                R.array.relationship_goals_array, android.R.layout.simple_spinner_item);
        relationshipAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        relationshipGoalsSpinner.setAdapter(relationshipAdapter);

        // Setup upload image button
        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        // Setup save button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });

        // Load current profile data
        loadProfileData();
    }

    private void loadProfileData() {
        mDatabase.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);

                    // Set basic info
                    firstNameEditText.setText(user.getFirstName());
                    lastNameEditText.setText(user.getLastName());

                    // Set course
                    setSpinnerSelection(courseSpinner, user.getCourse());

                    // Set profile image
                    if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                        currentProfileImageUrl = user.getProfileImage();
                        Glide.with(EditProfileActivity.this)
                                .load(currentProfileImageUrl)
                                .placeholder(R.drawable.default_profile)
                                .into(profileImageView);
                    }

                    // Get profile info
                    DataSnapshot profileSnapshot = dataSnapshot.child("profile");
                    if (profileSnapshot.exists()) {
                        String bio = profileSnapshot.child("bio").getValue(String.class);
                        String hobbies = profileSnapshot.child("hobbies").getValue(String.class);
                        String relationshipGoals = profileSnapshot.child("relationshipGoals").getValue(String.class);

                        bioEditText.setText(bio != null ? bio : "");
                        hobbiesEditText.setText(hobbies != null ? hobbies : "");

                        if (relationshipGoals != null) {
                            setSpinnerSelection(relationshipGoalsSpinner, relationshipGoals);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(EditProfileActivity.this, "Failed to load profile data: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                profileImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveProfile() {
        final String firstName = firstNameEditText.getText().toString().trim();
        final String lastName = lastNameEditText.getText().toString().trim();
        final String bio = bioEditText.getText().toString().trim();
        final String hobbies = hobbiesEditText.getText().toString().trim();
        final String course = courseSpinner.getSelectedItem().toString();
        final String relationshipGoals = relationshipGoalsSpinner.getSelectedItem().toString();

        // Validate inputs
        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(EditProfileActivity.this, "Please fill in your name", Toast.LENGTH_SHORT).show();
            return;
        }

        // If image was changed, upload it
        if (imageUri != null) {
            uploadImage(firstName, lastName, bio, hobbies, course, relationshipGoals);
        } else {
            // Save profile without changing image
            saveProfileData(firstName, lastName, bio, hobbies, course, relationshipGoals, currentProfileImageUrl);
        }
    }

    private void uploadImage(final String firstName, final String lastName, final String bio,
                             final String hobbies, final String course, final String relationshipGoals) {
        try {
            // Convert image to byte array
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageData = baos.toByteArray();

            // Upload to ImgBB
            ImgBBUploader.upload(this, imageData, new ImgBBUploader.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    // Save profile with new image URL
                    saveProfileData(firstName, lastName, bio, hobbies, course, relationshipGoals, imageUrl);
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(EditProfileActivity.this, "Image upload failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfileData(String firstName, String lastName, String bio, String hobbies,
                                 String course, String relationshipGoals, String imageUrl) {
        // Update user data
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("firstName", firstName);
        userUpdates.put("lastName", lastName);
        userUpdates.put("course", course);
        if (imageUrl != null) {
            userUpdates.put("profileImage", imageUrl);
        }

        // Update profile data
        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("bio", bio);
        profileUpdates.put("hobbies", hobbies);
        profileUpdates.put("relationshipGoals", relationshipGoals);

        // Save to Firebase
        mDatabase.child("users").child(currentUserId).updateChildren(userUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Save profile data
                            mDatabase.child("users").child(currentUserId).child("profile").updateChildren(profileUpdates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                                                finish();
                                            } else {
                                                Toast.makeText(EditProfileActivity.this, "Failed to update profile: " + task.getException().getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        } else {
                            Toast.makeText(EditProfileActivity.this, "Failed to update user data: " + task.getException().getMessage(),
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
