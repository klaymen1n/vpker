package com.sh0zer.vpker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class FileAdapter extends ArrayAdapter<File> {
    private ArrayList<File> fileList;
    private LayoutInflater inflater;

    public FileAdapter(Context context, ArrayList<File> fileList) {
        super(context, 0, fileList);
        this.fileList = fileList;
        inflater = LayoutInflater.from(context);

        Collections.sort(fileList, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item, parent, false);
        }

        File file = fileList.get(position);

        TextView textView = convertView.findViewById(android.R.id.text1);
        textView.setText(file.getName());

        ImageView imageView = convertView.findViewById(R.id.imageView);

        if(file.isDirectory())
            imageView.setImageResource(R.drawable.folder);
        else
            imageView.setImageResource(R.drawable.file);

        return convertView;
    }

}