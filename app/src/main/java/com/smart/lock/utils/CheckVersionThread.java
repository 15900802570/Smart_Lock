
package com.smart.lock.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.action.AbstractTransaction.TransferPayResponse;
import com.smart.lock.action.CheckVersionAction;
import com.smart.lock.entity.VersionModel;
import com.smart.lock.transfer.HttpCodeHelper;
import com.smart.lock.ui.UserManagerActivity;
import com.smart.lock.widget.BaseDialog;
import com.smart.lock.widget.DialogFactory;
import com.smart.lock.widget.DownloadDialog.OnDownLoadListener;
import com.smart.lock.widget.DownloadDialog;

import java.io.File;
import java.io.IOException;

public class CheckVersionThread {

    private static final String TAG = CheckVersionThread.class.getSimpleName();
    private String downloadUrl;
    private String updateMsg;
    private String mVersionName; //门锁版本
    private CheckVersionAction mVersionAction = null;
    private DialogFactory dialog;
    private Activity mContext;
    private Dialog mLoadingDialog;
    private boolean isShowLoadDialog = false;
    private Handler mHandler;

    /**
     * 超时提示框启动器
     */
    private Runnable mRunnable = new Runnable() {
        public void run() {
            if (mLoadingDialog != null && mLoadingDialog.isShowing()) {

                DialogUtils.closeDialog(mLoadingDialog);

                Toast.makeText(mContext, mContext.getString(R.string.retry_connect), Toast.LENGTH_LONG).show();
            }

        }
    };

    public CheckVersionThread(Activity context, DialogFactory dialog) {
        mContext = context;
        this.dialog = dialog;
        mHandler = new Handler();
        mVersionAction = new CheckVersionAction();
        mLoadingDialog = DialogUtils.createLoadingDialog(context, context.getString(R.string.checking_version));
        mLoadingDialog.show();
        mVersionAction.setUrl(ConstantUtil.CHECK_APP_VERSION);
        mVersionAction.setDeviceSn(SystemUtils.getMetaDataFromApp(context));
//        mVersionAction.setDeviceSn("1586301");
        mVersionAction.setExtension(ConstantUtil.APPLICATION);
        mVersionAction.setTransferPayResponse(tcheckAppVerResponse);
    }

    public void setShowLoadDialog(boolean isShowLoadDialog) {
        this.isShowLoadDialog = isShowLoadDialog;
    }

    public void run() {
        if (!FileUtil.checkSDCard()) // 内存卡如果不存在，就不检验版本
        {
            if (isShowLoadDialog) {
                Toast.makeText(mContext, mContext.getString(R.string.add_card), Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (isShowLoadDialog) {
            mLoadingDialog.show();
        }
        mVersionAction.transStart(mContext);
    }

    TransferPayResponse tcheckAppVerResponse = new TransferPayResponse() {

        @Override
        public void transFailed(String httpCode, String errorInfo) {
            if (mLoadingDialog != null) {
                mLoadingDialog.cancel();
            }
            compareVersion(CheckVersionAction.NO_NEW_VERSION);
        }

        @Override
        public void transComplete() {
            if (HttpCodeHelper.RESPONSE_SUCCESS.equals(mVersionAction.respondData.respCode)) {
                VersionModel version = mVersionAction.respondData.model;
                if (mLoadingDialog != null) {
                    mLoadingDialog.cancel();
                }
                downloadUrl = version.path;
                updateMsg = version.msg;
                mVersionName = version.versionName;

                String dir = FileUtil.createDir(mContext, ConstantUtil.APP_DIR_NAME) + File.separator;
                String path = dir + version.fileName;

                int code = StringUtil.compareVersion(version.versionName, getLocalVersionName(mContext));
                if (0 == code || code == -1) {
                    compareVersion(CheckVersionAction.NO_NEW_VERSION);
                } else {
                    File file = new File(path);
                    if (file.exists()) {
                        updateAppVersion(path);
                    }else {
                        if (version.forceUpdate) {
                            compareVersion(CheckVersionAction.MAST_UPDATE_VERSION);
                        } else {
                            compareVersion(CheckVersionAction.SELECT_VERSION_UPDATE);
                        }
                    }
                }

            } else {
                compareVersion(CheckVersionAction.NO_NEW_VERSION);
            }
        }
    };

    private void updateAppVersion(final String path) {
        if (dialog != null) {
            dialog = null;
            dialog = new DialogFactory(mContext);
        }
        dialog.getAlter(mContext.getString(R.string.update_title), /*mContext.getString(R.string.current_version)
                + getLocalVersionName(mContext) + "\n"
                + mContext.getString(R.string.update_version)
                + mVersionName + "\n"
                +*/ mContext.getString(R.string.check_sd_has_new_version))
                .setOkButtonText(mContext.getString(R.string.update))
                .setDownloadCancelable(false)
                .setButtonVisible(BaseDialog.DIALOG_OK_AND_NO_BUTTON_VISIBLE)
                .setOkClick(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancelDownLoadDialog();
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        File updateFile = new File(path);
                        Uri apkUri;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            // 授予文件操作的临时权限,根据需求设定，一般安装只需要READ权限
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            // 获取配置的FileProvider的 Content Uri的值
                            apkUri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".FileProvider", updateFile);
                        } else {
                            apkUri = Uri.fromFile(updateFile);
                        }
                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                        mContext.startActivity(intent);
                        // nm.cancel(1); //关闭通知
                        System.exit(0);
                    }
                })
                .setNoButtonText(mContext.getString(R.string.cancel))
                .setNoClick(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancelDownLoadDialog();
                    }
                })
                .show();
    }

    /**
     * 当发现已经是最新版本 删除之前的安装包
     */
    private void isNewVersion() {
        String filePath = FileUtil.createDir(mContext, ConstantUtil.APP_DIR_NAME) + File.separator;
        File file = new File(filePath);
        if (file.exists() && file.canWrite() && file.isDirectory()) {
            LogUtil.i("file", "删除已有文件");
            File[] tempFiles = file.listFiles();
            if (tempFiles.length > 0) {
                for (int i = 0; i < tempFiles.length; i++) {
                    File tempFile = tempFiles[i];
                    if (tempFile.canRead() && tempFile.canWrite()) {
                        LogUtil.i("file", "删除文件:" + tempFile.getName());
                        tempFile.delete();
                    }
                }
            }
        }
    }

    /**
     * 获取本地软件版本号名称
     */
    public static String getLocalVersionName(Context ctx) {
        String localVersion = "";
        try {
            PackageInfo packageInfo = ctx.getApplicationContext().getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionName;
            LogUtil.d("TAG", "本软件的版本号。。" + localVersion);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }

    private void compareVersion(int type) {
        switch (type) {
            case CheckVersionAction.NO_NEW_VERSION:
                if (isShowLoadDialog) {
                    if (dialog != null) {
                        dialog = null;
                        dialog = new DialogFactory(mContext);
                    }
                    dialog.show(mContext.getString(R.string.isnew_version) + "\n" + mContext.getString(R.string.current_version)
                            + getLocalVersionName(mContext));
                }
                isNewVersion();
                LogUtil.e("version", "当前已经是最新的版本了");
                break;
            case CheckVersionAction.SELECT_VERSION_UPDATE:
                if (dialog != null) {
                    dialog = null;
                    dialog = new DialogFactory(mContext);
                }
                dialog.getAlter(mContext.getString(R.string.update_title), /*mContext.getString(R.string.current_version)
                        + getLocalVersionName(mContext) + "\n"
                        + mContext.getString(R.string.update_version)
                        + mVersionName + "\n"
                        +*/ updateMsg)
                        .setOkButtonText(mContext.getString(R.string.update))
                        .setDownloadCancelable(false)
                        .setButtonVisible(BaseDialog.DIALOG_OK_AND_NO_BUTTON_VISIBLE)
                        .setOkClick(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.cancelDownLoadDialog();
                                toDownload(false);
                                LogUtil.i("version", "正在更新版本");
                            }
                        })
                        .setNoButtonText(mContext.getString(R.string.cancel))
                        .setNoClick(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (!isShowLoadDialog) {
                                }
                                dialog.cancelDownLoadDialog();
                            }
                        })
                        .show();
                break;
            case CheckVersionAction.MAST_UPDATE_VERSION:
                toDownload(true);
                break;
        }
    }

    private DownloadDialog downloadDialog;

    public void toDownload(boolean mastDownload) {
        if (downloadDialog == null) {
            downloadDialog = new DownloadDialog(mContext,
                    mVersionAction.respondData.model, mastDownload);
            downloadDialog.setHaveBackDownload(isShowLoadDialog);
            downloadDialog.setOnDownLoadListener(onDownLoadListener);
        }
        try {
            downloadDialog.show();
        } catch (Exception e) {
            downloadDialog.cancel();
            e.printStackTrace();
        }
    }

    OnDownLoadListener onDownLoadListener = new OnDownLoadListener() {
        @Override
        public void onDownLoadStatus(int downloadStatus) {
            switch (downloadStatus) {
                case DownloadDialog.DOWNLOAD_ERROR:
                    downloadError(mContext.getString(R.string.download_error));
                    break;
                case DownloadDialog.NOT_NETWORK:
                    downloadError(mContext.getString(R.string.net_error));
                    break;
                default:
                    break;
            }
        }
    };

    private void downloadError(String msg) {

        if (downloadDialog != null) {
            downloadDialog.cancel();
        }
    }

    /**
     * 超时提醒
     *
     * @param seconds
     */
    protected void closeDialog(final int seconds) {

        mHandler.removeCallbacks(mRunnable);

        mHandler.postDelayed(mRunnable, seconds * 1000);
    }
}
