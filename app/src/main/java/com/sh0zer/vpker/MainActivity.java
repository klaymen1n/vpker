package com.sh0zer.vpker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
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
            currentDirectory = initialDirectory;
            displayFiles(initialDirectory);
        }

        ImageView menuButton = findViewById(R.id.menuIcon);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu();
            }
        });

        fileListView.setOnItemClickListener((parent, view, position, id) -> {
            Object selectedFile = fileList.get(position);
            String selectedFileString = "";
            if (position == 0) {
                goBack();
                return;
            }
            if(selectedFile instanceof File) {
                File selecedFileT = (File) selectedFile;

                selectedFileString = selectedFile.toString();

                if (selecedFileT.isDirectory()) {
                    displayFiles(selecedFileT);
                    currentDirectory = selecedFileT;
                } else if (selectedFileString.substring(selectedFileString.lastIndexOf('.') + 1).compareTo("vpk") == 0) {
                    try {
                        manageVpk(selecedFileT, "");
                    } catch (ArchiveException | IOException | EntryException e) {
                        Log.e("ERROR", "Error has occurred " + Arrays.toString(e.getStackTrace()));
                    }finally {
                        currentArchive = selecedFileT;
                    }
                } else
                    Toast.makeText(MainActivity.this, "Выбран файл: " + selecedFileT.getName(), Toast.LENGTH_SHORT).show();
            }
            if(selectedFile instanceof VirualFile) {
                VirualFile selecedFileV = (VirualFile) selectedFile;
                if (selecedFileV.isDirectory()) {
                    try {
                        manageVpk(currentArchive, selecedFileV.toString() + "/");
                    } catch (ArchiveException | IOException | EntryException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
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

    @Override
    public void onBackPressed() {
        if (currentDirectory.isDirectory()) {
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
                if (file.isFile() || file.isDirectory()) {
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

    void goBack() {
        try {
            displayFiles(currentDirectory.getParentFile());
            currentDirectory = currentDirectory.getParentFile();
            retries = 0;
        } catch (Exception e) {
            Log.e("ERROR", "Error has occurred " + e.getMessage());
        }
    }

    private void manageVpk(File vpkFile, String targetDirectory) throws ArchiveException, IOException, EntryException {
        Archive vpkArchive = new Archive(vpkFile);
        fileList.clear();
        FileAdapter fileAdapter = new FileAdapter(this, fileList);
        fileListView.setAdapter(fileAdapter);

        vpkArchive.load();

        Set<String> printedDirectories = new HashSet<>();

        for (Directory directory : vpkArchive.getDirectories()) {
            String directoryPath = directory.getPath();
            int index = directoryPath.indexOf(targetDirectory);
            if (printedDirectories.contains(directoryPath)) {
                continue;
            }
            if (!directoryPath.contains("/") && targetDirectory.isEmpty()) {
                Log.d("DEBUGDEBUG!", ""+ directoryPath);
                fileList.add(new VirualFile(directoryPath, true));
                printEntries(directory, targetDirectory, fileAdapter);
            } else if (index != -1) {
                String relativePath = directoryPath.substring(index + targetDirectory.length());
                int index2 = relativePath.indexOf("/");
                if (index2 != -1) {
                    String subDirectory = relativePath.substring(0, index2);
                     Log.d("DEBUGDEBUG!", ""+ subDirectory);
                    fileList.add(new VirualFile(subDirectory, true));
                } else {
                     Log.d("DEBUGDEBUG!", ""+ relativePath);
                    fileList.add(new VirualFile(relativePath, true));
                }
                printEntries(directory, targetDirectory, fileAdapter);
            } else {
                continue;
            }
            printedDirectories.add(directoryPath);
            fileAdapter.notifyDataSetChanged();
            Log.d("DEBUGDEBUG!", "fileList size: "+ fileList.size());
        }
    }

    private void printEntries(Directory directory, String path, FileAdapter fileAdapter) {
        for (Entry entry : directory.getEntries()) {
            Log.d("DEBUG DEBUG", directory.getPathFor(entry) + (path + "/" + entry.getFullName()));
            if (directory.getPathFor(entry).equals(path + "/" + entry.getFullName())) {
                fileList.add(new VirualFile(entry.getFullName(), false));
            }
        }
        fileAdapter.notifyDataSetChanged();
    }
}