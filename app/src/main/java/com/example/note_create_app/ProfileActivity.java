package com.example.note_create_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 200;
    private static final int TAG = 1;

    private ImageView ivProfile;
    private TextView tvUsername, tvEmail, tvPhoneType, tvPhone, tvNoteCount;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private String uid;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap bmp = (Bitmap) result.getData().getExtras().get("data");
                    ivProfile.setImageBitmap(bmp);
                    uploadBitmap(bmp);
                }
            });

    private final ActivityResultLauncher<String> pickLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    ivProfile.setImageURI(uri);
                    try {
                        Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        uploadBitmap(bmp);
                    } catch (Exception e) {
                        Toast.makeText(this, "Kép feldolgozási hiba", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ivProfile = findViewById(R.id.profileImage);
        tvUsername = findViewById(R.id.textUsername);
        tvEmail = findViewById(R.id.textEmail);
        tvPhoneType = findViewById(R.id.textPhoneType);
        tvPhone = findViewById(R.id.textPhoneNumber);
        tvNoteCount = findViewById(R.id.textNoteCount);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        uid = auth.getCurrentUser().getUid();
        storageRef = FirebaseStorage.getInstance().getReference("profile_images/" + uid + ".jpg");

        findViewById(R.id.btnCamera).setOnClickListener(v -> openCamera(v));
        findViewById(R.id.btnUpload).setOnClickListener(v -> upload());

        loadProfile();
        countNotes();
    }

    public void openCamera(View view) {
        checkUserPermission();
    }

    public void checkUserPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CODE);
                return;
            }
        }
        takePicture();
    }

    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAG);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture();
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAG && resultCode == RESULT_OK) {
            Bitmap img = (Bitmap) data.getExtras().get("data");
            ivProfile.setImageBitmap(img);
            uploadBitmap(img);
        }
    }

    public void upload() {
        pickLauncher.launch("image/*");
    }

    private void loadProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getEmail() != null) {
            String currentUserEmail = user.getEmail();

            db.collection("users")
                    .whereEqualTo("email", currentUserEmail)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);

                            String username = document.getString("username");
                            String email = document.getString("email");
                            String phoneNumber = document.getString("phone");
                            String phoneType = document.getString("phoneType");

                            tvUsername.setText(username != null ? username : "N/A");
                            tvEmail.setText(email != null ? email : "N/A");
                            tvPhone.setText(phoneNumber != null ? phoneNumber : "N/A");
                            tvPhoneType.setText(phoneType != null ? phoneType : "N/A");

                            // Ha a képet is meg akarod jeleníteni:
                            String imageUrl = document.getString("profileImageUrl");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.profile)
                                        .into(ivProfile);
                            }
                        } else {
                            Toast.makeText(this, "Felhasználó nem található.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Hiba a profil betöltésekor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Felhasználói adatok nem elérhetők.", Toast.LENGTH_SHORT).show();
        }
    }


    private void uploadBitmap(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] data = baos.toByteArray();

        storageRef.putBytes(data)
                .continueWithTask((Continuation<UploadTask.TaskSnapshot, Task<Uri>>) task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return storageRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String url = task.getResult().toString();
                        Map<String, Object> upd = new HashMap<>();
                        upd.put("profileImageUrl", url);
                        db.collection("users").document(uid).update(upd);
                        Toast.makeText(this, "Profilkép frissítve", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Feltöltési hiba", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void countNotes() {
        db.collection("Notes")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvNoteCount.setText("Jegyzeteim száma: " + count);
                })
                .addOnFailureListener(e -> {
                    tvNoteCount.setText("Jegyzetek száma: hiba");
                    Toast.makeText(this, "Nem sikerült lekérni a jegyzetek számát.", Toast.LENGTH_SHORT).show();
                });
    }

    public void cancelProfile(View view) {
        finish();
    }

}
