package com.smart.lock.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.smart.lock.R;
import com.smart.lock.utils.ConstantUtil;

import java.util.ArrayList;


public class LockManagerAdapter extends BaseAdapter {

    private Context mContext;
    private GridView mGridView;
    private ArrayList<Integer> mIcons = new ArrayList();
    private ArrayList<Integer> mNames = new ArrayList();

    public LockManagerAdapter(Context context, GridView gridView,int permission) {
        mContext = context;
        mGridView = gridView;

        mNames.add(Integer.valueOf(R.string.password_manager));
        mIcons.add(Integer.valueOf(R.mipmap.icon_password));

        mNames.add(Integer.valueOf(R.string.fingerprint_manager));
        mIcons.add(Integer.valueOf(R.mipmap.icon_fingerprint));

        mNames.add(Integer.valueOf(R.string.card_manager));
        mIcons.add(Integer.valueOf(R.mipmap.icon_nfc));

        mNames.add(Integer.valueOf(R.string.event_manager));
        mIcons.add(Integer.valueOf(R.mipmap.icon_events));

        if(permission == ConstantUtil.DEVICE_MASTER) {
            mNames.add(Integer.valueOf(R.string.token_manager));
            mIcons.add(Integer.valueOf(R.mipmap.icon_temporarypassword));

            mNames.add(Integer.valueOf(R.string.permission_manager));
            mIcons.add(Integer.valueOf(R.mipmap.icon_userguanl));
        }

    }

    @Override
    public int getCount() {
        return mIcons.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewGroup.LayoutParams layoutParams = new AbsListView.LayoutParams(mGridView.getWidth() / 3, mGridView.getHeight() / 2);
        View inflate = View.inflate(mContext, R.layout.item_lock_manager, null);
        inflate.setLayoutParams(layoutParams);
        View findViewById = inflate.findViewById(R.id.grid_item);
        ImageView imageView = inflate.findViewById(R.id.image);
        ((TextView) inflate.findViewById(R.id.content)).setText(((Integer) mNames.get(position)).intValue());
        imageView.setBackgroundResource(((Integer) mIcons.get(position)).intValue());
        findViewById.setTag(mIcons.get(position));
        return inflate;
    }

}
