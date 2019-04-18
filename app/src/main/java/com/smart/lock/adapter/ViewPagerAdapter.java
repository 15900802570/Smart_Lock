
package com.smart.lock.adapter;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.smart.lock.R;

import java.util.ArrayList;

public class ViewPagerAdapter extends PagerAdapter {

    private ArrayList<ImageView> images;
    private Context mActivity;

    public ViewPagerAdapter(Context context,int[] imageIds) {

        mActivity = context;
        images = new ArrayList<ImageView>();
        for (int imageId : imageIds) {
            ImageView imageView = new ImageView(context);
            imageView.setBackgroundResource(imageId);
            images.add(imageView);
        }
    }

    public void setImageIds(int[] imageIds) {
        images = new ArrayList<ImageView>();
        for (int imageId : imageIds) {
            ImageView imageView = new ImageView(mActivity);
            imageView.setBackgroundResource(imageId);
            images.add(imageView);
        }
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub

        return images.size();
    }

    // 是否是同一张图片
    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        // TODO Auto-generated method stub
        return arg0 == arg1;
    }

    @Override
    public void destroyItem(ViewGroup view, int position, Object object) {
        // TODO Auto-generated method stub
        // super.destroyItem(container, position, object);
        // view.removeViewAt(position);
        view.removeView(images.get(position));

    }

    @Override
    public Object instantiateItem(ViewGroup view, int position) {
        // TODO Auto-generated method stub
        view.addView(images.get(position));

        return images.get(position);
    }

    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

}
