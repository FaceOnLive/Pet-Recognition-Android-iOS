package com.ttv.petrecog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ttv.facerecog.R;

import java.util.ArrayList;

public class UsersAdapter extends ArrayAdapter<FaceEntity> {
    public UsersAdapter(Context context, ArrayList<FaceEntity> users) {
        super(context, 0, users);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        FaceEntity user = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_user, parent, false);
        }
        // Lookup view for data population
        TextView tvName = (TextView) convertView.findViewById(R.id.txtName);
        ImageView faceView = (ImageView) convertView.findViewById(R.id.faceView);
        // Populate the data into the template view using the data object
        tvName.setText(user.userName);
        faceView.setImageBitmap(user.headImg);
        // Return the completed view to render on screen
        return convertView;
    }
}