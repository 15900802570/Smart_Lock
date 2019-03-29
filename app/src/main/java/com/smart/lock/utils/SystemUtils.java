package com.smart.lock.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class SystemUtils {

    /**
     * 获取屏幕的宽和高
     *
     * @param context 参数为上下文对象Context
     * @return 返回值为长度为2int型数组, 其中
     * int[0] -- 表示屏幕的宽度
     * int[1] -- 表示屏幕的高度
     */
    public static int[] getSystemDisplay(Context context) {
        //创建保存屏幕信息类
        DisplayMetrics dm = new DisplayMetrics();
        //获取窗口管理类
        WindowManager wm = (WindowManager) context.getSystemService(
                Context.WINDOW_SERVICE);
        //获取屏幕信息并保存到DisplayMetrics中
        wm.getDefaultDisplay().getMetrics(dm);
        //声明数组保存信息
        int[] displays = new int[2];
        displays[0] = dm.widthPixels;//屏幕宽度(单位:px)
        displays[1] = dm.heightPixels;//屏幕高度
        return displays;
    }

    /**
     * 保存图片到指定路径
     *
     * @param context
     * @param bitmap   要保存的图片
     * @param fileName 自定义图片名称
     * @return
     */
    public static Uri saveImageToGallery(Context context, Bitmap bitmap, String fileName) {
        // 保存图片至指定路径
        String storePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "SmartLock_DT";
        File appDir = new File(storePath);
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        File file = new File(appDir, fileName+".jpg");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            //通过io流的方式来压缩保存图片(80代表压缩20%)
            boolean isSuccess = bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();

            //发送广播通知系统图库刷新数据
            Uri uri = Uri.fromFile(file);
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            if (isSuccess) {
                return uri;
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 分享图片(直接将bitamp转换为Uri)
     *
     * @param bitmap
     */
    public static void shareImg(Bitmap bitmap, Context context) {
        Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, null, null));
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("image/*");//设置分享内容的类型
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent = Intent.createChooser(intent, "分享");
        context.startActivity(intent);
    }

    private static void save(Context context, Bitmap bmp, File appDir) {
        //命名文件名称
        String fileName = System.currentTimeMillis() + ".jpg";
        //创建图片文件，传入文件夹和文件名
        File imagePath = new File(appDir, fileName);
        try {
            //创建文件输出流，传入图片文件，用于输入bitmap
            FileOutputStream fos = new FileOutputStream(imagePath);
            //将bitmap压缩成png，并保存到相应的文件夹中
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            //冲刷流
            fos.flush();
            //关闭流
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    imagePath.getAbsolutePath(), fileName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 最后通知图库更新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + imagePath.getAbsolutePath())));
    }


    /**
     * 检测当的网络（WLAN、3G/2G）状态
     * @param context Context
     * @return true 表示网络可用
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected())
            {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED)
                {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }


}
