package com.jgh.androidssh.dialogs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.jgh.androidssh.R;
import com.jgh.androidssh.domain.SessionUserInfo;

import java.util.List;

public final class UserSpinnerAdapter extends BaseAdapter {

    List<SessionUserInfo> data;

    public UserSpinnerAdapter(List<SessionUserInfo> data) {
        this.data = data;
    }

    @Override
    public int getCount() {
        if(data!=null)
            return data.size();
        return 0;
    }

    @Override
    public Object getItem(int i) {
        if (data!=null)
            return data.get(i);
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view==null){
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.spinner_item,viewGroup,false);
        }
        TextView tv = view.findViewById(R.id.username);
        tv.setText(data.get(i).getUser());



        return view;
    }
}
