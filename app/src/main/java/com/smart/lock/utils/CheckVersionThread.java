
package com.smart.dt.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import cc.cloudist.acplibrary.ACProgressFlower;

import com.smart.dt.R;
import com.smart.dt.action.AbstractTransaction.TransferPayResponse;
import com.smart.dt.action.CheckVersionAction;
import com.smart.dt.application.BaseApplication;
import com.smart.dt.db.bean.VersionModel;
import com.smart.dt.transfer.HttpCodeHelper;
import com.smart.dt.widget.DialogFactory;
import com.smart.dt.widget.DownloadDialog;
import com.smart.dt.widget.DownloadDialog.OnDownLoadListener;
import com.smart.dt.widget.LoadingDialog;
import com.smart.dt.widget.YishuaDialog;

import java.io.File;

public class CheckVersionThread {

    private String downloadUrl;
    private String updateMsg;
    private CheckVersionAction versionAction = null;
    private DialogFactory dialog;
    private Activity mContext;
    private ACProgressFlower mLoadingDialog;
    private boolean isShowLoadDialog = false;

    public CheckVersionThread(Activity context, DialogFactory dialog) {
        mContext = context;
        this.dialog = dialog;
        versionAction = new CheckVersionAction();
        mLoadingDialog = new ACProgressFlower.Builder(context).direction(100)
                .text(mContext.getString(R.string.checking_version))
                .themeColor(-1).fadeColor(0xff444444).build();
        mLoadingDialog.setCanceledOnTouchOutside(false);

        BaseApplication app = (BaseApplication) context.getApplication();
        if (app.getLoginInfo() == null) {
            Toast.makeText(context, "登陆异常，请重新登陆", Toast.LENGTH_LONG).show();
            app.gotoLoginActivity();
        } else {
            versionAction.setToken(app.getLoginInfo().getToken());
            versionAction.setFilename(ConstantUtil.SMART_DT);
            versionAction.setExtension(ConstantUtil.APK_EXTENSION);
            versionAction.setTransferPayResponse(tPayResponse);
        }
    }

    public void setShowLoadDialog(boolean isShowLoadDialog) {
        this.isShowLoadDialog = isShowLoadDialog;
    }

    public void run() {
        if (!FileUtil.checkSDCard()) // 内存卡如果不存在，就不检验版本
        {
            if (isShowLoadDialog) {
                Toast.makeText(mContext, mContext.getString(R.string.add_card), Toast.LENGTH_LONG).show();
            } else {
            }
            return;
        }
        if (isShowLoadDialog) {
            mLoadingDialog.show();
        }
        versionAction.transStart(mContext);
    }

    TransferPayResponse tPayResponse = new TransferPayResponse() {

        @Override
        public void transFailed(String httpCode, String errorInfo) {
            if (mLoadingDialog != null) {
                mLoadingDialog.cancel();
            }
            compareVersion(CheckVersionAction.NO_NEW_VERSION);
        }

        @Override
        public void transComplete() {
            if (HttpCodeHelper.RESPONSE_SUCCESS.equals(versionAction.respondData.respCode)) {
                VersionModel version = versionAction.respondData.model;
                if (mLoadingDialog != null) {
                    mLoadingDialog.cancel();
                }
                downloadUrl = version.path;
                updateMsg = version.msg;
                int code = SystemUtil.getversonCode(mContext);
                if (version.versionCode == code) {
                    compareVersion(CheckVersionAction.NO_NEW_VERSION);
                } else {
//                    String[] unUpdateCode = version.unUpdateCode.split(",");
//                    for (String temp : unUpdateCode) {
//                        if (code == Integer.parseInt(temp)) {
//                            compareVersion(CheckVersionAction.NO_NEW_VERSION);
//                            return;
//                        }
//                    }
                    if (version.forceUpdate == true) {
                        compareVersion(CheckVersionAction.MAST_UPDATE_VERSION);
                    } else {
                        compareVersion(CheckVersionAction.SELECT_VERSION_UPDATE);
                    }
                }

            } else {
                compareVersion(CheckVersionAction.NO_NEW_VERSION);
            }
        }
    };

    /**
     * 当发现已经是最新版本 删除之前的安装包
     */
    private void isNewVersion() {
        String filePath = "";
        if (FileUtil.checkSDCard()) {
            filePath = Environment.getExternalStorageDirectory() + File.separator + "SmartDT"
                    + File.separator + "downloads";
        } else {
            filePath = mContext.getCacheDir().getAbsolutePath() + File.separator + "SmartDT"
                    + File.separator + "downloads";
        }

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
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
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
                    dialog.show(mContext.getString(R.string.isnew_version) + "\n" + "当前版本号：" + "\n"
                            + getLocalVersionName(mContext));
                } else {
                }
                isNewVersion();
                LogUtil.e("version", "当前已经是最新的版本了");
                break;
            case CheckVersionAction.SELECT_VERSION_UPDATE:
                if (dialog != null) {
                    dialog = null;
                    dialog = new DialogFactory(mContext);
                }
                dialog
                        .getAlter(mContext.getString(R.string.update_title), updateMsg)
                        .setOkButtonText(mContext.getString(R.string.update))
                        .setYishuaCancelable(false)
                        .setButtonVisible(YishuaDialog.DIALOG_OK_AND_NO_BUTTON_VISIBLE)
                        .setOkClick(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.cancelYiShuaDialog();
                                toDownload(false);
                                LogUtil.i("version", "正在更新版本");
                            }
                        })
                        .setNoButtonText(mContext.getString(R.string.cancel_dialog))
                        .setNoClick(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (!isShowLoadDialog) {
                                }
                                dialog.cancelYiShuaDialog();
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
            downloadDialog = new DownloadDialog(mContext, downloadUrl,
                    versionAction.respondData.model.versionCode, mastDownload);
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

}
