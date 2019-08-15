package com.smart.lock.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.smart.lock.R;
import com.smart.lock.entity.LangnageModel;

import java.util.List;

public class LangnageAdapter extends BaseAdapter {
    private Context mCtx;
    private final int mLayoutId;
    private List<LangnageModel> mModels;

    public LangnageAdapter(Context context, int resource, List<LangnageModel> models) {
        mCtx = context;
        mLayoutId = resource;
        mModels = models;
    }


    @Override
    public int getCount() {
        return mModels.size();
    }

    @Override
    public Object getItem(int position) {
        return mModels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void selectLangnage(int position) {

        for (int i = 0; i < mModels.size(); i++) {
            if (i == position)
                mModels.get(i).setDefaultLangnage(true);
            else mModels.get(i).setDefaultLangnage(false);
        }

        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LangnageModel model = mModels.get(position);
        View view = LayoutInflater.from(mCtx).inflate(mLayoutId, parent, false);
        TextView langnage = view.findViewById(R.id.tv_language);
        ImageView selectIv = view.findViewById(R.id.iv_select);

        langnage.setText(model.getLangnage());
        if (model.isDefaultLangnage()) {
            selectIv.setVisibility(View.VISIBLE);
            selectIv.setImageResource(R.mipmap.ic_select);
        } else selectIv.setVisibility(View.GONE);

        return view;
    }

}
