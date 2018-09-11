package com.jgh.androidssh.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jgh.androidssh.R;

import java.io.File;
import java.util.ArrayList;


/**
 * Adapter for ListView. Holds current local directory
 * file list. Differentiates between directories and non-directories.
 *
 */
public class LocaleFileListAdapter extends BaseAdapter {

    private ArrayList<File> mFiles;
    private LayoutInflater mInflater;

    public LocaleFileListAdapter(Context context, ArrayList<File> files) {
        mFiles = files;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return mFiles.size();
    }

    public Object getItem(int arg0) {
        return mFiles.get(arg0);
    }

    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.listview_item, null);
            holder.textView = convertView.findViewById(R.id.textview_item);
            holder.imageView = convertView.findViewById(R.id.imageview_item);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.textView.setText(mFiles.get(position).getName());
        holder.textView.setTextColor(Color.WHITE);
        if (mFiles.get(position).isDirectory()) {
            holder.color = 0xff009999;
            holder.imageView.setImageResource(R.drawable.folder);
        }
        else{
            holder.color = 0xffff8888;
            holder.imageView.setImageResource(R.drawable.file);
        }
//        holder.textView.setTextColor(holder.color);
        return convertView;
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView textView;
        int color;
    }
}
