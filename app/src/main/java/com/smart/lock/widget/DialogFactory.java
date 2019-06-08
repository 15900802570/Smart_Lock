package com.smart.lock.widget;


import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.smart.lock.R;

import java.util.Timer;
import java.util.TimerTask;


/***
 * 弹出框
 * @author smz
 */
public class DialogFactory {
    public static final int TOAST = 1;
    public static final int ALERT_DIALOG = 2;
    private BaseDialog mDownLoadDialog;
    private Activity mContext;
    private Timer timer;

    private static DialogFactory factory;

    public DialogFactory(Activity c) {
        mContext = c;
        mDownLoadDialog = new BaseDialog(mContext);
    }

    public Context getmContext() {
        return mContext;
    }

    /***
     * 检验为空弹出框
     * @param showType  弹出类型    Toast or  Dialog
     * @param msg       弹出的内容
     */
    public void showNullDialog(int showType, String msg) {
        switch (showType) {
            case TOAST:
                Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                break;
            case ALERT_DIALOG:
                show(msg);
                break;
            default:
                Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                break;
        }
    }


    /***
     * 得到一个对话框对象
     *
     * @param title
     * @param msg
     */
    public BaseDialog getAlter(int title, int msg) {
        getAlter(getString(title), getString(msg));
        return mDownLoadDialog;
    }

    public BaseDialog getAlter(String title, String msg) {
        if (mDownLoadDialog != null) {
            mDownLoadDialog = null;
        }
        mDownLoadDialog = new BaseDialog(mContext);
        mDownLoadDialog
                .setTit(title)
                .setMessage(msg);
        return mDownLoadDialog;
    }

    public BaseDialog getAlter() {
        if (mDownLoadDialog != null) {
            mDownLoadDialog = null;
        }
        mDownLoadDialog = new BaseDialog(mContext);
        return mDownLoadDialog;
    }

    /***
     * 得到一个对话框对象
     *
     * @param msg
     */
    public BaseDialog getAlter(int msg) {
        getAlter(getString(msg));
        return mDownLoadDialog;
    }

    public BaseDialog getAlter(String msg) {
        if (mDownLoadDialog != null) {
            mDownLoadDialog = null;
        }
        mDownLoadDialog = new BaseDialog(mContext);
        mDownLoadDialog.setMessage(msg);
        return mDownLoadDialog;
    }

    public void show(String msg) {
        show(msg, getString(R.string.confirm));
    }

    public void show(int msg) {
        show(getString(msg));
    }

    public void show(String msg, long time) {
        if (mDownLoadDialog != null) {
            mDownLoadDialog = null;
        }
        mDownLoadDialog = new BaseDialog(mContext);
        mDownLoadDialog
                .setButtonVisible(BaseDialog.DIALOG_NO_BUTTON_VISIBLE)
                .setMessage(msg)
                .show();
        if (timer == null) {
            timer = new Timer();
        }
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                if (mDownLoadDialog != null)
                    mDownLoadDialog.cancel();
                this.cancel();
                timer.cancel();
                timer = null;
            }
        }, time);
    }


    public void show(int title, int msg, int ok) {
        if (mDownLoadDialog != null) {
            mDownLoadDialog = null;
        }
        mDownLoadDialog = new BaseDialog(mContext);
        mDownLoadDialog
                .setTit(title)
                .setOkButtonText(getString(ok))
                .setOkClick(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mDownLoadDialog.cancel();
                    }
                })
                .setMessage(msg)
                .show();
    }

    public void show(int msg, int ok) {
        show(getString(msg), getString(ok));
    }

    public void show(String msg, String ok) {
        if (mDownLoadDialog != null) {
            mDownLoadDialog = null;
        }
        mDownLoadDialog = new BaseDialog(mContext);
        mDownLoadDialog
                .setButtonVisible(BaseDialog.DIALOG_OK_BUTTON_VISIBLE)
                .setOkButtonText(ok)
                .setOkClick(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mDownLoadDialog.cancel();
                    }
                })
                .setMessage(msg)
                .show();

    }

    public void cancelDownLoadDialog() {
        if (mDownLoadDialog != null) {
            mDownLoadDialog.cancel();
        }
    }

    private String getString(int stringId) {
        if (mContext != null) {
            return mContext.getString(stringId);
        } else {
            return null;
        }
    }

    public void setCancel(boolean flag) {
        mDownLoadDialog.setCancelable(flag);
    }

    public static DialogFactory getInstance(Activity context) {

        factory = new DialogFactory(context);
        return factory;
    }


}
