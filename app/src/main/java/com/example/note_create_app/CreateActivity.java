package com.example.note_create_app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateActivity extends AppCompatActivity {

    private EditText mTitleEditText, mContentEditText;
    private Button mSaveButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private NotificationHandler mNotificationHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_note);

        mTitleEditText = findViewById(R.id.titleEditText);
        mContentEditText = findViewById(R.id.contentEditText);
        mSaveButton = findViewById(R.id.saveButton);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote(v);
            }
        });

        mNotificationHandler = new NotificationHandler(this);
    }

    public void saveNote(View view) {
        String title = mTitleEditText.getText().toString().trim();
        String content = mContentEditText.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            Toast.makeText(CreateActivity.this, "Minden mezőt ki kell tölteni!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        long currentDate = System.currentTimeMillis();

        Map<String, Object> note = new HashMap<>();
        note.put("userId", userId);
        note.put("title", title);
        note.put("content", content);
        note.put("date", currentDate);

        mFirestore.collection("Notes")
                .add(note)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(CreateActivity.this, "Jegyzet sikeresen mentve!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateActivity.this, "Hiba történt a mentés során", Toast.LENGTH_SHORT).show();
                });

        mNotificationHandler.send("Jegyzet sikeresen létrehozva!");
    }

    public void cancelNote(View view) {
        finish();
    }

}
