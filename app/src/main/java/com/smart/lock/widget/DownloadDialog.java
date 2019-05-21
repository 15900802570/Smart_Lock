
package com.smart.lock.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.entity.VersionModel;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.FileUtil;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.NetworkUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

public class DownloadDialog extends Dialog implements View.OnClickListener {
    public static final int DOWNLOAD_PREPARE = 0;
    public static final int DOWNLOAD_WORK = 1;
    public static final int DOWNLOAD_OK = 2;
    public static final int DOWNLOAD_ERROR = 3;
    public static final int NOT_NETWORK = 4;
    private static final String TAG = "DownloadDialog";
    private Activity mContext;
    private String path;
    private String tempPath;
    private String fileName;
    private int versionCode;
    private PercentageProgressBar pb;
    private boolean isMastDoanload = false;
    private boolean isHaveBackDownload = false;
    private String fileSizeText = null;
    private NotificationManager nm;
    private boolean isSuccess = true;
    private TextView mVerMsgTv;
    private VersionModel mModel;

    /**
     * 需要下载的文件
     */
    private String url;

    public DownloadDialog(Activity context, VersionModel model, boolean isMastdoanload) {
        super(context, R.style.AppDialog);
        this.isMastDoanload = isMastdoanload;
        mContext = context;
        mModel = model;
        this.url = ConstantUtil.BASE_URL + model.path;
        this.versionCode = model.versionCode;
        getPath();
        nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        setCancelable(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_layout);
        init();
    }

    @Override
    public void show() {
        super.show();
        downloadSize = 0;
        fileSize = 0;
        if (pb != null)
            pb.setProgress(0);
//        if (tv != null)
//            tv.setText("" + 0 + "%");
        toDoanload();
        listConn();
    }

    @Override
    public void cancel() {
        try {

            if (thread != null) {
                thread.interrupt();
            }
            thread = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (listNetworkTimer != null) {
            listNetworkTimer.cancel();
            listNetworkTimer.purge();
            listNetworkTimer = null;
        }
        super.cancel();
    }

    private void init() {
        pb = this.findViewById(R.id.down_pb);
        mVerMsgTv = this.findViewById(R.id.dialog_msg);
        mVerMsgTv.setText(mModel.msg);
    }

    private Timer listNetworkTimer = null;

    private void listConn() {
        if (listNetworkTimer != null) {
            listNetworkTimer.cancel();
            listNetworkTimer.purge();
            listNetworkTimer = null;
        }
        listNetworkTimer = new Timer();
        listNetworkTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                if (NetworkUtil.getNetworkType(mContext) == NetworkUtil.NO_NET_CONNECT) {
                    sendMessage(NOT_NETWORK);
                    cancel();
                }
            }
        }, 0, 1000);
    }

    /**
     * 文件下载
     */
    private void downloadFile() {
        isSuccess = true;
        FileOutputStream fos = null;
        InputStream is = null;
        long time1 = System.currentTimeMillis() / 1000;
        try {
            URL u = new URL(url);
            URLConnection conn = u.openConnection();
            conn.connect();
            is = conn.getInputStream();
            conn.setConnectTimeout(20 * 1000);
            conn.setReadTimeout(30 * 1000);
            fileSize = conn.getContentLength();
            if (fileSize < 1 || is == null) {
                sendMessage(DOWNLOAD_ERROR);
            } else {
                sendMessage(DOWNLOAD_PREPARE);
                File downFile = new File(tempPath);
                if (downFile.exists()) {
                    downFile.delete();
                }
                fos = new FileOutputStream(downFile);
                byte[] bytes = new byte[1024 * 1000];
                int len = -1;

                while ((len = is.read(bytes)) != -1) {
                    // if(NetworkUtil.getNetworkType(mContext) ==
                    // NetworkUtil.NO_NET_CONNECT)
                    // {
                    // sendMessage(NOT_NETWORK);
                    // isSuccess = false;
                    // break;
                    // }
                    fos.write(bytes, 0, len);
                    fos.flush();
                    downloadSize += len;
                    sendMessage(DOWNLOAD_WORK);
                }
                if (isSuccess) {
                    copyFile();
                    sendMessage(DOWNLOAD_OK);
                }
            }
        } catch (Exception e) {
            sendMessage(DOWNLOAD_ERROR);
            long time2 = System.currentTimeMillis() / 1000;
            Log.d(TAG, "time out : " + (time2 - time1));
            e.printStackTrace();
        } finally {
            try {
                is.close();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void copyFile() {
        File tempFile = new File(tempPath);
        if (!tempFile.exists()) {
            return;
        }
        File newFile = new File(path);
        if (!newFile.exists()) {
            try {
                newFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileInputStream tempInputStream = null;
        FileOutputStream newOutPutStream = null;
        try {
            tempInputStream = new FileInputStream(tempFile);
            newOutPutStream = new FileOutputStream(newFile);
            byte[] buff = new byte[1024 * 200];
            int len = -1;
            while ((len = tempInputStream.read(buff)) != -1) {
                newOutPutStream.write(buff, 0, len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (newOutPutStream != null) {
                try {
                    newOutPutStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (tempInputStream != null) {
                try {
                    tempInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        tempFile.delete();
    }

    /**
     * 文件一共的大小
     */
    int fileSize = 0;
    /**
     * 已经下载的大小
     */
    int downloadSize = 0;
    /**
     * handler处理消息
     */
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DOWNLOAD_PREPARE:
                    Toast.makeText(mContext, mContext.getString(R.string.ready_download),
                            Toast.LENGTH_SHORT).show();
                    pb.setVisibility(ProgressBar.VISIBLE);
                    pb.setProgress(0);
                    LogUtil.i(TAG, "一共:" + fileSize);
//                    pb.setMax(100);
                    fileSizeText = FileUtil.formatFileSize(fileSize);
                    break;
                case DOWNLOAD_WORK:
                    LogUtil.i(TAG, "已下载:" + downloadSize);
//                    pb.setProgress(downloadSize);
                    if (fileSize < 1) {
                        fileSize = 1;
                    }
                    int res = downloadSize * 100 / fileSize;
//                    tv.setText("" + res + "%");
                    pb.setProgress(res);
                    break;
                case DOWNLOAD_OK:
                    File downFile = new File(path);
                    if (!downFile.exists()) {
                        Toast.makeText(mContext, mContext.getString(R.string.download_error),
                                Toast.LENGTH_SHORT).show();
                        cancel();
                        return;
                    }
                    showNotification();
                    downloadSize = 0;
                    fileSize = 0;
                    Toast.makeText(mContext, mContext.getString(R.string.down_finish),
                            Toast.LENGTH_SHORT).show();

                    // Intent intent1 = new Intent(Intent.ACTION_DELETE);
                    // Uri packageURI =
                    // Uri.parse("package:"+mContext.getPackageName());
                    // intent1.setData(packageURI);
                    // mContext.startActivity(intent1);

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    File updateFile = new File(path);
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
                        Uri uriForFile = FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".provider", updateFile);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setDataAndType(uriForFile, mContext.getContentResolver().getType(uriForFile));
                    }else{
                        intent.setDataAndType(Uri.parse("file://" + path),
                                "application/vnd.android.package-archive");
                    }
                    mContext.startActivity(intent);
                    // nm.cancel(1); //关闭通知
                    System.exit(0);
                    break;
                case DOWNLOAD_ERROR:
                    isSuccess = false;
                    Toast.makeText(mContext, mContext.getString(R.string.download_error),
                            Toast.LENGTH_SHORT).show();
                    downloadSize = 0;
                    fileSize = 0;
                    File file = new File(tempPath);
                    file.delete();
                    break;
                case NOT_NETWORK:
                    Toast.makeText(mContext, mContext.getString(R.string.net_error),
                            Toast.LENGTH_SHORT).show();
                    isSuccess = false;
                    // downloadSize = 0;
                    // fileSize = 0;
                    // File file1 = new File(tempPath);
                    // file1.delete();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    /**
     * 得到文件的保存路径
     *
     * @return
     * @throws IOException
     */
    private void getPath() {
        fileName = url.substring(url.lastIndexOf("/") + 1);
        try {
            String dir = FileUtil.createDir(mContext, "app") + File.separator;
            path = dir + versionCode + fileName;
            tempPath = path + ".temp";
            FileUtil.clearFiles(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 给hand发送消息
     *
     * @param what
     */
    private void sendMessage(int what) {
        Message m = new Message();
        m.what = what;
        handler.sendMessage(m);
        if (onDownLoadListener != null) {
            onDownLoadListener.onDownLoadStatus(what);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
//        if (i == R.id.down_bt) {
//            if (!isHaveBackDownload) {
//                mContext.moveTaskToBack(true);
//            }
//        }
    }

    private Thread thread;

    public void setHaveBackDownload(boolean isHaveBackDownload) {
        this.isHaveBackDownload = isHaveBackDownload;
    }

    private void toDoanload() {
        File file = new File(path);
        if (file.exists()) {
            sendMessage(DOWNLOAD_OK);
        } else {
            if (thread == null) {
                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        downloadFile();
                    }
                });
            }
            thread.start();
        }
    }

    public static interface OnDownLoadListener {
        void onDownLoadStatus(int downloadStatus);
    }

    private OnDownLoadListener onDownLoadListener;

    public void setOnDownLoadListener(OnDownLoadListener onDownLoadListener) {
        this.onDownLoadListener = onDownLoadListener;
    }

    private void showNotification() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File file = new File(path);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            Uri uriForFile = FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".provider", file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(uriForFile, mContext.getContentResolver().getType(uriForFile));
        }else{
            intent.setDataAndType(Uri.parse("file://" + path),
                    "application/vnd.android.package-archive");
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0,
                intent, 0);
        String fixMessage = mContext.getString(R.string.app_name)
                + mContext.getString(R.string.fix_alert);

        Notification notification = new Notification.Builder(mContext)
                .setContentTitle(mContext.getString(R.string.down_alert))
                .setContentText(fixMessage)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.icon)
                .build();
        notification.defaults |= Notification.DEFAULT_SOUND; // 声音通知
        notification.defaults |= Notification.DEFAULT_VIBRATE; // 颤动通知
        notification.icon = R.mipmap.icon;
        notification.tickerText = mContext.getString(R.string.app_name)
                + mContext.getString(R.string.down_finish);
        notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_NO_CLEAR; // 不可清除的通知

        nm.notify(R.string.app_name, notification);
    }

}
