package com.sh0zer.vpker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int REQUEST_PERMISSION = 100;
    private ListView fileListView;
    private ArrayList<File> fileList;

    private File currentDirectory;
    private int retries = 0;

    private StringBuilder path = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileListView = findViewById(R.id.fileListView);
        fileList = new ArrayList<>();

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        else {
            File initialDirectory = Environment.getExternalStorageDirectory();
            displayFiles(initialDirectory);
            currentDirectory = initialDirectory;
        }

        fileListView.setOnItemClickListener((parent, view, position, id) -> {
            File selectedFile = fileList.get(position);
            if (selectedFile.isDirectory()) {
                displayFiles(selectedFile);
                currentDirectory = selectedFile;
            } else {
                Toast.makeText(MainActivity.this, "Выбран файл: " + selectedFile.getName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_menu:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.about_text)
                        .setTitle(R.string.action_about);
                AlertDialog dialog = builder.create();
                dialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                File initialDirectory = Environment.getExternalStorageDirectory();
                displayFiles(initialDirectory);
                currentDirectory = initialDirectory;
            } else {
                Toast.makeText(this, "No permissions", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(currentDirectory.isDirectory()) {
            if ("/storage/emulated".equals(currentDirectory.getParentFile().toString())) {
                if (retries == 1) {
                    super.onBackPressed();
                    retries = 0;
                } else {
                    retries++;
                    Toast.makeText(this, "Do it twice to exit", Toast.LENGTH_SHORT).show();
                }
            }
            else{
                try {
                    displayFiles(currentDirectory.getParentFile());
                    currentDirectory = currentDirectory.getParentFile();
                    retries = 0;
                }catch (Exception e) {
                    Log.e("ERROR", "Error has occurred " + e.getMessage());
                }
            }
        }
    }

    private void displayFiles(File directory) {
        fileList.clear();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() || file.isDirectory()) {
                    fileList.add(file);
                }
            }
            FileAdapter adapter = new FileAdapter(this, fileList);
            fileListView.setAdapter(adapter);
        }
    }
}
