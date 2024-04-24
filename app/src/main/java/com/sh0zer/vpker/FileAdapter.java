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
    private Context context;
    private static final int VIEW_TYPE_BACK_BUTTON = 0;
    private static final int VIEW_TYPE_FILE = 1;

    public FileAdapter(Context context, ArrayList<File> fileList) {
        super(context, 0, fileList);
        this.fileList = fileList;
        this.context = context;
        inflater = LayoutInflater.from(context);

        Collections.sort(fileList, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));

        fileList.add(0, null);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_BACK_BUTTON : VIEW_TYPE_FILE;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);

        if (convertView == null) {
            if (viewType == VIEW_TYPE_BACK_BUTTON) {
                convertView = inflater.inflate(R.layout.back_button_layout, parent, false);
            } else {
                convertView = inflater.inflate(R.layout.list_item, parent, false);
            }
        }

        if(viewType == VIEW_TYPE_BACK_BUTTON)
            return convertView;

        File file = fileList.get(position);
        TextView textView = convertView.findViewById(android.R.id.text1);
        textView.setText(file.getName());
        ImageView imageView = convertView.findViewById(R.id.imageView);
        if (file.isDirectory()) {
            imageView.setImageResource(R.drawable.folder);
        } else {
            imageView.setImageResource(R.drawable.file);
        }

        return convertView;
    }
}