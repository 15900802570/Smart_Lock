
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
    public int imageIds[];

    public ViewPagerAdapter(Context context) {

        imageIds = new int[] {
                R.mipmap.homepage_adv1,
                R.mipmap.homepage_adv2
        };

        images = new ArrayList<ImageView>();
        for (int i = 0; i < imageIds.length; i++) {
            ImageView imageView = new ImageView(context);
            imageView.setBackgroundResource(imageIds[i]);
            images.add(imageView);
        }
    }

    public void setImageIds(int[] imageIds) {
        this.imageIds = imageIds;
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

}
