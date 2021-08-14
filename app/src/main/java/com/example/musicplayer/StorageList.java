package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;

public class StorageList extends AppCompatActivity {

    private ArrayList<String> nameList, pathList;
    private ListView listview;
    private Uri filename;
    private String name, path;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_list);

        listview = findViewById(R.id.music_list);
        nameList = new ArrayList<>();
        pathList = new ArrayList<>();


        filename = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(filename, null, null, null, null);

        if (checkPermission()) {
            readDataFromStorage(cursor);
        }


        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                name = (String) parent.getItemAtPosition(position);
                String path = "";
                for (int i = 0; i < nameList.size(); i++) {
                    if (nameList.get(i).equals(name)) {
                        path = pathList.get(i);
                    }
                }
                startNewActivity(path);
            }
        });
    }

    private void readDataFromStorage(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            do {
                name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                if (path.endsWith(".mp3") || path.endsWith(".MP3")) {
                    pathList.add(path);
                    nameList.add(name);
                }
            } while (cursor.moveToNext());
        }
        ArrayAdapter adapter = new ArrayAdapter(StorageList.this, R.layout.item, nameList);
        listview.setAdapter(adapter);
    }

    private void startNewActivity(String playItemPath) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("paths", pathList);
        intent.putExtra("current", playItemPath);
        startActivity(intent);
    }

    private boolean checkPermission() {
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 0);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ContentResolver contentResolver = getContentResolver();
                    Cursor cursor = contentResolver.query(filename, null, null, null, null);
                    readDataFromStorage(cursor);
                }
                if (grantResults[1] == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 3);
                }
                break;
            case 3:
                break;

        }
    }
}