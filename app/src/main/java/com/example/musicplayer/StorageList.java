package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class StorageList extends AppCompatActivity {

    private ArrayList<String> nameList, pathList;
    private Map<String, String> songs, sortedMap;
    private ListView listview;
    private Uri filename;
    private String name, path;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_list);

        listview = findViewById(R.id.music_list);
        songs = new HashMap<>();

        filename = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(filename, null, null, null, null);

        if (checkStoragePermission()) {
            readDataFromStorage(cursor);
            checkAudioPermission();
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
                    //pathList.add(path);
                    //nameList.add(name);
                    songs.put(name, path);
                }
            } while (cursor.moveToNext());
        }
        sortedMap = new TreeMap<>(songs);
        nameList = new ArrayList<>(sortedMap.keySet());
        pathList = new ArrayList<>(sortedMap.values());

        ArrayAdapter adapter = new ArrayAdapter(StorageList.this, R.layout.item, nameList);
        listview.setAdapter(adapter);
    }

    private void startNewActivity(String playItemPath) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("paths", pathList);
        intent.putExtra("current", playItemPath);
        startActivity(intent);
    }

    private boolean checkStoragePermission() {
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return false;
        }
    }

    private boolean checkAudioPermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 2);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ContentResolver contentResolver = getContentResolver();
                    Cursor cursor = contentResolver.query(filename, null, null, null, null);
                    readDataFromStorage(cursor);
                    checkAudioPermission();
                } else {
                    showAlertDialog(1);
                }
                break;

            case 2:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // continue execution
                }
                else {
                    showAlertDialog(2);
                }

                break;
            default:
                return;
        }
    }
    public void showAlertDialog(int flag){

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setIcon(R.drawable.warning).setTitle(R.string.permissionTitle);
        if(flag==1){
            alertDialog.setMessage(R.string.storage);
        }else{
            alertDialog.setMessage(R.string.audio);
        }
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton(R.string.alertButton, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                finishAffinity();
            }
        }).show();
    }
}