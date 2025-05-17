package com.example.note_create_app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class Note_Create_MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = Note_Create_MainActivity.class.getName();
    private static final int SECRET_KEY = 99;

    private FirebaseUser user;

    private RecyclerView mRecyclerView;
    private ArrayList<Notes> mNotesData;
    private NotesAdapter mAdapter;

    private int gridNumber = 1;

    private boolean viewRow = true;

    private FirebaseFirestore mFirestore;
    private CollectionReference mNotes;

    private NotificationHandler mNotificationHandler;
    private AlarmManager mAlarmManager;
    private PendingIntent pendingIntent;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_note_create_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d(LOG_TAG, "bejelentkezett felhasználó");
        } else {
            Log.d(LOG_TAG, "nincs bejelentkezett felhasználó");
            finish();
        }

        mRecyclerView = findViewById(R.id.recycleview);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this,gridNumber));
        mNotesData = new ArrayList<>();

        mAdapter = new NotesAdapter(this, mNotesData);
        mRecyclerView.setAdapter(mAdapter);

        mFirestore = FirebaseFirestore.getInstance();
        mNotes = mFirestore.collection("Notes");



        mNotificationHandler = new NotificationHandler(this);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        setAlarmManager();
    }

    private void queryData(){
        mNotesData.clear();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mNotes.whereEqualTo("userId", userId)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(5)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Notes note = document.toObject(Notes.class);
                note.setId(document.getId());
                mNotesData.add(note);
            }

            if (mNotesData.size() == 0) {
                //FIX ADATOK
                //intializeData();
                //queryData();
                Toast.makeText(this, "Nincs adat", Toast.LENGTH_SHORT).show();
            }

            mAdapter.notifyDataSetChanged();
        });
    }

//FIX ADATOK
//    private void intializeData() {
//        String[] noteTitles = getResources().getStringArray(R.array.note_titles);
//        String[] noteContents = getResources().getStringArray(R.array.note_contents);
//        String[] noteDates = getResources().getStringArray(R.array.note_dates);
//
//        for (int i = 0; i < noteTitles.length; i++) {
//            mNotes.add(new Notes(noteTitles[i], noteDates[i], noteContents[i]));
//        }
//
//    }

    public void deleteNote(Notes note) {
        DocumentReference noteRef = mNotes.document(note._getId());

        noteRef.delete().addOnSuccessListener(success -> {
            Log.d(LOG_TAG, "Sikeres törlés" + note._getId());
        })
        .addOnFailureListener(failure -> {
            Toast.makeText(this, "Sikertelen törlés" + note._getId(), Toast.LENGTH_SHORT).show();
        });

        queryData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.note_main_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.search_bar);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(LOG_TAG, newText);
                mAdapter.getFilter().filter(newText);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.log_out) {
            Log.d(LOG_TAG, "kijelentkezés");
            FirebaseAuth.getInstance().signOut();
            if (pendingIntent != null) {
                mAlarmManager.cancel(pendingIntent);
            }
            finish();
            return true;

        }
        else if (itemId == R.id.view_selector) {
            Log.d(LOG_TAG, "nézet");
            if (viewRow) {
                changeSpanCount(item, R.drawable.view_grid, 1);
            } else {
                changeSpanCount(item, R.drawable.view_row, 2);
            }
            return true;

        }
        else if (itemId == R.id.profile) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.isAnonymous()) {
                Toast.makeText(this, "Profilja megtekintéséhez jelentkezzen be!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.putExtra("SECRET_KEY", SECRET_KEY);
                if (pendingIntent != null) {
                    mAlarmManager.cancel(pendingIntent);
                }
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, ProfileActivity.class);
                if (pendingIntent != null) {
                    mAlarmManager.cancel(pendingIntent);
                }
                startActivity(intent);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void changeSpanCount(MenuItem item, int viewGrid, int i) {
        viewRow = !viewRow;
        item.setIcon(viewGrid);
        GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
        layoutManager.setSpanCount(i);
        mAdapter.notifyDataSetChanged();
    }

    public void create(View view) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.isAnonymous()) {
            Toast.makeText(this, "Jegyzet létrehozásához jelentkezzen be!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("SECRET_KEY", SECRET_KEY);
            if (pendingIntent != null) {
                mAlarmManager.cancel(pendingIntent);
            }
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, CreateActivity.class);
            if (pendingIntent != null) {
                mAlarmManager.cancel(pendingIntent);
            }
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart(){
        super.onStart();
        queryData();
    }

    private void setAlarmManager(){
        long repeatInterval = 30*1000; //AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        long triggerTime = SystemClock.elapsedRealtime()+repeatInterval;
        Intent intent = new Intent(this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, repeatInterval, pendingIntent);
    }
}