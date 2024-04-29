package com.sh0zer.vpker;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class FileAdapter extends ArrayAdapter<Object> {
    private ArrayList<Object> fileList;
    private LayoutInflater inflater;
    private Context context;
    private static final int VIEW_TYPE_BACK_BUTTON = 0;
    private static final int VIEW_TYPE_FILE = 1;

    public FileAdapter(Context context, ArrayList<Object> fileList) {
        super(context, 0, fileList);
        this.fileList = fileList;
        this.context = context;
        inflater = LayoutInflater.from(context);

        Collections.sort(fileList, (o1, o2) -> {
            if(o1 instanceof File && o2 instanceof File)
                return ((File) o1).getName().compareToIgnoreCase(((File) o2 ).getName());
            else if(o1 instanceof VirualFile && o2 instanceof VirualFile)
                return ((VirualFile) o1).getName().compareToIgnoreCase(((VirualFile) o2 ).getName());
            else
                return 0;
        });

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

        Object object = fileList.get(position);
        TextView textView = convertView.findViewById(android.R.id.text1);
        ImageView imageView = convertView.findViewById(R.id.imageView);

        if(object instanceof File) {
            File file = (File) object;
            textView.setText(file.getName());
            if (file.isDirectory()) {
                imageView.setImageResource(R.drawable.folder);
            } else {
                imageView.setImageResource(R.drawable.file);
            }
        }else if(object instanceof VirualFile){
            VirualFile virualFile = (VirualFile) object;
            Log.d("DEBUG", virualFile.getName());
            textView.setText(virualFile.getName());
            if(virualFile.isDirectory())
                imageView.setImageResource(R.drawable.folder);
            else
                imageView.setImageResource(R.drawable.file);
        }
        return convertView;
    }
}