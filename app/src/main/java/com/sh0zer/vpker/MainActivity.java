package com.sh0zer.vpker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.connorhaigh.javavpk.core.Archive;
import com.connorhaigh.javavpk.core.Directory;
import com.connorhaigh.javavpk.core.Entry;
import com.connorhaigh.javavpk.exceptions.ArchiveException;
import com.connorhaigh.javavpk.exceptions.EntryException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private static final int REQUEST_PERMISSION = 100;
    private ListView fileListView;
    private ArrayList<Object> fileList;

    private File currentDirectory;
    private File currentArchive;
    private String fullArchivePath = "";
    private String currentArchiveDir = "";

    private int retries = 0;

    private StringBuilder path = new StringBuilder();

    @TargetApi(Build.VERSION_CODES.R)
    private void requestAllFilesAccess()
    {
        if(!Environment.isExternalStorageManager()){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Permission Required")
                .setMessage("The application needs permission to access all files.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileListView = findViewById(R.id.fileListView);
        fileList = new ArrayList<>();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestAllFilesAccess();
        }
        else if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        else {
            File initialDirectory = Environment.getExternalStorageDirectory();
            currentDirectory = initialDirectory;
            displayFiles(initialDirectory);
        }

        ImageView menuButton = findViewById(R.id.menuIcon);
        menuButton.setOnClickListener(v -> showMenu());

        fileListView.setOnItemClickListener((parent, view, position, id) -> {
            Object selectedFile = fileList.get(position);
            String selectedFileString = "";
            if (position == 0) {
                goBack();
                return;
            }
            if(selectedFile instanceof File) {
                File selectedFileT = (File) selectedFile;

                selectedFileString = selectedFile.toString();

                if (selectedFileT.isDirectory()) {
                    displayFiles(selectedFileT);
                    currentDirectory = selectedFileT;
                } else if (selectedFileString.endsWith("vpk")) {
                    try {
                        manageVpk(selectedFileT, "");
                    } catch (ArchiveException | IOException | EntryException e) {
                        Log.e("ERROR", "Error has occurred " + Arrays.toString(e.getStackTrace()));
                    }finally {
                        currentArchive = selectedFileT;
                    }
                } else
                    Toast.makeText(MainActivity.this, "File selected: " + selectedFileT.getName(), Toast.LENGTH_SHORT).show();
            }
            if(selectedFile instanceof VirtualFile) {
                VirtualFile selectedFileV = (VirtualFile) selectedFile;
                if (selectedFileV.isDirectory()) {
                    try {
                        Log.d("DBG!", ""+ selectedFileV.getName() );
                        manageVpk(currentArchive, selectedFileV.getName() + "/");
                    } catch (ArchiveException | IOException | EntryException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if(Environment.isExternalStorageManager()){
                if(currentDirectory == null) {
                    File initalDirectory = Environment.getExternalStorageDirectory();
                    currentDirectory = initalDirectory;
                    displayFiles(initalDirectory);
                }
            } else {
                Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                File initialDirectory = Environment.getExternalStorageDirectory();
                currentDirectory = initialDirectory;
                displayFiles(initialDirectory);
            } else {
                Toast.makeText(this, "No permissions", Toast.LENGTH_SHORT).show();
            }
        }
    }

    void goBack() {
        if(currentArchive == null) {
            try {
                displayFiles(currentDirectory.getParentFile());
                currentDirectory = currentDirectory.getParentFile();
                retries = 0;
            } catch (Exception e) {
                Log.e("ERROR", "Error has occurred " + e.getMessage());
            }
        }
        else {
            String prevDir = fullArchivePath.replace(currentArchiveDir, "");
            Log.d("prevdir", "" + prevDir + " " + fullArchivePath + " " + currentArchiveDir);
            if(currentArchiveDir == "")
            {
                closeArchive();
                displayFiles(currentDirectory.getParentFile());
            }
            else {
                try {
                    manageVpk(currentArchive, prevDir);
                } catch (ArchiveException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (EntryException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (currentDirectory.isDirectory() && currentArchive == null) {
            if ("/storage/emulated".equals(currentDirectory.getParentFile().toString())) {
                if (retries == 1) {
                    super.onBackPressed();
                    retries = 0;
                } else {
                    retries++;
                    Toast.makeText(this, "Do it twice to exit", Toast.LENGTH_SHORT).show();
                }
            } else
                goBack();
        }
    }

    @SuppressLint("SetTextI18n")
    private void displayFiles(File directory) {
        File[] files = directory.listFiles();

        if (files != null) {
            fileList.clear();
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".vpk") || file.isDirectory()) {
                    fileList.add(file);
                }
            }
            FileAdapter adapter = new FileAdapter(this, fileList);
            fileListView.setAdapter(adapter);

            TextView emptyText = findViewById(R.id.emptyText);
            if (fileList.size() == 1)
                emptyText.setVisibility(View.VISIBLE);
            else
                emptyText.setVisibility(View.GONE);


            TextView directoryText = findViewById(R.id.directoryText);
            String text = directory.toString();
            if(currentArchive != null)
                text += currentArchive.toString();
            Toast.makeText(this, "" + text, Toast.LENGTH_SHORT).show();
            if (text.length() > 50) {
                String temp1 = text.substring(text.length() - 50);
                String temp2 = temp1.substring(temp1.indexOf("/") + 1);
                directoryText.setText("Current directory is ../" + temp2);
            } else
                directoryText.setText("Current directory is " + text);
        }
    }

    void showMenu() {
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, findViewById(R.id.menuIcon));
        popupMenu.getMenuInflater().inflate(R.menu.menu_main, popupMenu.getMenu());
        popupMenu.show();


        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_menu) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(R.string.about_text)
                            .setTitle(R.string.action_about);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return true;
                } else
                    return false;
            }
        });
    }

    private Archive vpkArchive = null;
    private FileAdapter fileAdapter = null;
    private void manageVpk(File vpkFile, String targetDirectory) throws ArchiveException, IOException, EntryException {
        if(vpkArchive == null) {
            vpkArchive = new Archive(vpkFile);
            vpkArchive.load();
        }
        if(fileAdapter == null) {
            fileAdapter = new FileAdapter(this, fileList);
            fileListView.setAdapter(fileAdapter);
        }

        fileList.clear();
        fullArchivePath += targetDirectory;
        currentArchiveDir = targetDirectory;

        Set<String> printedDirectories = new HashSet<>();

        for (Directory directory : vpkArchive.getDirectories()) {
            String directoryPath = directory.getPath();
            int index = directoryPath.indexOf(targetDirectory);
            if (printedDirectories.contains(directoryPath)) {
                continue;
            }
            if (!directoryPath.contains("/")) {
                //Log.d("DEBUGDEBUG!", ""+ directoryPath);
                fileList.add(new VirtualFile(directoryPath, true));
                printEntries(directory, targetDirectory, fileAdapter);
            } else if (index != -1) {
                String relativePath = directoryPath.substring(index + targetDirectory.length());
                int index2 = relativePath.indexOf("/");
                if (index2 != -1) {
                    String subDirectory = relativePath.substring(0, index2);
                  //   Log.d("DEBUGDEBUG!", ""+ subDirectory);
                    fileList.add(new VirtualFile(subDirectory, true));
                } else {
                    // Log.d("DEBUGDEBUG!", ""+ relativePath);
                    fileList.add(new VirtualFile(relativePath, true));
                }
                printEntries(directory, targetDirectory, fileAdapter);
            } else {
                continue;
            }
            printedDirectories.add(directoryPath);
            fileAdapter.notifyDataSetChanged();
            //Log.d("DEBUGDEBUG!", "fileList size: "+ fileList.size());
        }
    }

    private void closeArchive()
    {
        currentArchive = null;
        currentArchiveDir = "";
        fullArchivePath = "";
        vpkArchive = null;
        fileAdapter = null;
    }

    private void printEntries(Directory directory, String path, FileAdapter fileAdapter) {
        for (Entry entry : directory.getEntries()) {
            //Log.d("DEBUG DEBUG", directory.getPathFor(entry) + (path + "/" + entry.getFullName()));
            if (directory.getPathFor(entry).equals(path + "/" + entry.getFullName())) {
                fileList.add(new VirtualFile(entry.getFullName(), false));
            }
        }
        fileAdapter.notifyDataSetChanged();
    }
}