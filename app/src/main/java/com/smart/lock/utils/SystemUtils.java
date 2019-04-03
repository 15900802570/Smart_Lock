package com.smart.lock.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class SystemUtils {

    private static final String TAG = SystemUtils.class.getSimpleName();

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
        File file = new File(appDir, fileName + ".jpg");
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
     *
     * @param context Context
     * @return true 表示网络可用
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }

    //获取value
    public static String getMetaDataFromApp(Context context) {
        String value = "";
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            LogUtil.d(TAG,"APP_SN = "+ appInfo.metaData.getString("APP_SN"));
            value = appInfo.metaData.getString("APP_SN");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return value;
    }

    public static boolean firstStartApp = true;

    public static final int TID_NOT_EXISTS = -1;
    /**
     * 得到分辨率高度
     */
    public static int heightPs = -1;
    /**
     * 得到分辨率宽度
     */
    public static int widthPs = -1;
    /**
     * 得到屏幕密度
     */
    public static int densityDpi = -1;
    /**
     * 得到X轴密度
     */
    public static float Xdpi = -1;
    /**
     * 得到Y轴密度
     */
    public static float Ydpi = -1;

    private Context context;

    public SystemUtils(Context context) {
        this.context = context;
    }

    /***
     * 得到手机的屏幕基本信息
     *
     * @param context
     */
    public static void getScreen(Activity context) {
        DisplayMetrics metrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        heightPs = metrics.heightPixels;
        widthPs = metrics.widthPixels;
        densityDpi = metrics.densityDpi;
        Xdpi = metrics.xdpi;
        Ydpi = metrics.ydpi;
        LogUtil.i("手机分辨率", "分辨率：" + widthPs + "X" + heightPs + "    屏幕密度："
                + densityDpi + "    宽高密度：" + Xdpi + "X" + Ydpi);
    }

    //屏幕宽度（像素）
    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    /***
     * 获取客户端版本
     *
     * @return
     */
    public PackageInfo getVersion() {
        PackageInfo info = null;
        try {
            PackageManager manager = context.getPackageManager();
            info = manager.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            LogUtil.e("version", "获取版本失败");
            e.printStackTrace();
        }
        return info;
    }

    /***
     * 获取手机model
     */
    public static String getPhoneMode() {
        return android.os.Build.MODEL;
    }

    /**
     * 把密度dip单位转化为像数px单位
     *
     * @param context
     * @param dip
     * @return
     */
    public static int dipToPx(Context context, int dip) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dip * scale + 0.5f * (dip >= 0 ? 1 : -1));
    }

    /***
     * 把像数px转化为密度dip单位
     *
     * @param context
     * @param px
     * @return
     */
    public static int pxToDip(Context context, int px) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (px * scale + 0.5f * (px >= 0 ? 1 : -1));
    }


    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight
                + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    public static String getverson(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(),
                    0);
            String version = info.versionName;
            return version;
        } catch (Exception e) {
            e.printStackTrace();
            return "0";
        }
    }

    public static int getversonCode(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(),
                    0);

            return info.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
            return 11;
        }
    }

    public static String getIpAddr(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        WifiInfo info = wifiManager.getConnectionInfo();

        return Formatter.formatIpAddress(info.getIpAddress());
    }

}
