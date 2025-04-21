package com.ensias.essudatingapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText, ageEditText, birthdayEditText;
    private Spinner courseSpinner, interestedInSpinner;
    private RadioGroup genderRadioGroup;
    private Button uploadImageButton, registerButton;
    private ImageView profileImageView;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private Uri imageUri;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI elements
        firstNameEditText = findViewById(R.id.first_name);
        lastNameEditText = findViewById(R.id.last_name);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        ageEditText = findViewById(R.id.age);
        birthdayEditText = findViewById(R.id.birthday);
        courseSpinner = findViewById(R.id.course_spinner);
        genderRadioGroup = findViewById(R.id.gender_radio_group);
        interestedInSpinner = findViewById(R.id.interested_in_spinner);
        uploadImageButton = findViewById(R.id.upload_image_button);
        registerButton = findViewById(R.id.register_button);
        profileImageView = findViewById(R.id.profile_image);

        // Initialize calendar for birthday picker
        calendar = Calendar.getInstance();

        // Setup course spinner
        ArrayAdapter<CharSequence> courseAdapter = ArrayAdapter.createFromResource(this,
                R.array.courses_array, android.R.layout.simple_spinner_item);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        courseSpinner.setAdapter(courseAdapter);

        // Setup interested in spinner
        ArrayAdapter<CharSequence> interestedInAdapter = ArrayAdapter.createFromResource(this,
                R.array.interested_in_array, android.R.layout.simple_spinner_item);
        interestedInAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        interestedInSpinner.setAdapter(interestedInAdapter);

        // Setup birthday date picker
        birthdayEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        // Setup image upload
        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        // Setup register button
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateBirthdayField();
                        calculateAge();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set max date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updateBirthdayField() {
        String format = "MM/dd/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        birthdayEditText.setText(sdf.format(calendar.getTime()));
    }

    private void calculateAge() {
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - calendar.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < calendar.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        ageEditText.setText(String.valueOf(age));
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

    private void registerUser() {
        final String firstName = firstNameEditText.getText().toString().trim();
        final String lastName = lastNameEditText.getText().toString().trim();
        final String email = emailEditText.getText().toString().trim();
        final String password = passwordEditText.getText().toString().trim();
        final String age = ageEditText.getText().toString().trim();
        final String birthday = birthdayEditText.getText().toString().trim();
        final String course = courseSpinner.getSelectedItem().toString();
        final String interestedIn = interestedInSpinner.getSelectedItem().toString();

        // Get selected gender
        int selectedGenderId = genderRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedGender = findViewById(selectedGenderId);
        final String gender = selectedGender != null ? selectedGender.getText().toString() : "";

        // Validate inputs
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()
                || age.isEmpty() || birthday.isEmpty() || gender.isEmpty() || imageUri == null) {
            Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Upload image to ImgBB
                            uploadImageToImgBB();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void uploadImageToImgBB() {
        try {
            // Convert image to byte array
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageData = baos.toByteArray();

            // Upload to ImgBB using ImgBBUploader class
            ImgBBUploader.upload(this, imageData, new ImgBBUploader.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    // Save user data to Firebase
                    saveUserToFirebase(imageUrl);
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(RegisterActivity.this, "Image upload failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserToFirebase(String imageUrl) {
        String userId = mAuth.getCurrentUser().getUid();

        // Create user data map
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstNameEditText.getText().toString().trim());
        userData.put("lastName", lastNameEditText.getText().toString().trim());
        userData.put("email", emailEditText.getText().toString().trim());
        userData.put("age", Integer.parseInt(ageEditText.getText().toString().trim()));
        userData.put("birthday", birthdayEditText.getText().toString().trim());
        userData.put("course", courseSpinner.getSelectedItem().toString());

        int selectedGenderId = genderRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedGender = findViewById(selectedGenderId);
        userData.put("gender", selectedGender.getText().toString());

        userData.put("interestedIn", interestedInSpinner.getSelectedItem().toString());
        userData.put("profileImage", imageUrl);
        userData.put("createdAt", new Date().getTime());

        // Save to Firebase
        mDatabase.child("users").child(userId).setValue(userData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            // Redirect to profile setup
                            Intent intent = new Intent(RegisterActivity.this, ProfileSetupActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Failed to save user data: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
