package com.smart.lock.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.CountDownTimer;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.design.widget.BottomSheetDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.Device;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.ui.login.LockScreenActivity;
import com.smart.lock.widget.CustomDialog;

public class DialogUtils {
    private static final String TAG = "DialogUtils";

    /**
     * 显示Dialog
     *
     * @param context      上下文
     * @param msg          显示内容
     * @param isTransBg    是否透明
     * @param isCancelable 是否可以点击取消
     * @return
     */
    public static Dialog showWaitDialog(Context context, String msg, boolean isTransBg, boolean isCancelable) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.data_dialog_loading, null);             // 得到加载view
        RelativeLayout layout = (RelativeLayout) v.findViewById(R.id.dialog_view);// 加载布局

        // main.xml中的ImageView
        ImageView spaceshipImage = (ImageView) v.findViewById(R.id.data_iv);
        TextView tipTextView = (TextView) v.findViewById(R.id.tip_tv);   // 提示文字
        // 加载动画
        Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(context, R.anim.rotate_loading);
        // 使用ImageView显示动画
        spaceshipImage.startAnimation(hyperspaceJumpAnimation);
        tipTextView.setText(msg);// 设置加载信息

        Dialog loadingDialog = new Dialog(context, isTransBg ? R.style.TransDialogStyle : R.style.WhiteDialogStyle);    // 创建自定义样式dialog
        loadingDialog.setContentView(layout);
        loadingDialog.setCancelable(isCancelable);
        loadingDialog.setCanceledOnTouchOutside(false);

        Window window = loadingDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setGravity(Gravity.CENTER);
        window.setAttributes(lp);
        window.setWindowAnimations(R.style.PopWindowAnimStyle);
        loadingDialog.show();
        return loadingDialog;
    }

    public static Dialog createLoadingDialog(Context context, String msg) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.dialog_loading, null);// 得到加载view
        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_loading_view);// 加载布局
        TextView tipTextView = (TextView) v.findViewById(R.id.tip_tv);// 提示文字
        tipTextView.setText(msg);// 设置加载信息

        Dialog loadingDialog = new Dialog(context, R.style.DialogStyle);// 创建自定义样式dialog
        loadingDialog.setCancelable(true); // 是否可以按“返回键”消失
        loadingDialog.setCanceledOnTouchOutside(false); // 点击加载框以外的区域
        loadingDialog.setContentView(layout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));// 设置布局
        /**
         *将显示Dialog的方法封装在这里面
         */
        Window window = loadingDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setGravity(Gravity.CENTER);
        window.setAttributes(lp);
        window.setWindowAnimations(R.style.PopWindowAnimStyle);

        loadingDialog.setCancelable(false);

        return loadingDialog;
    }

    public static Dialog createWarningDialog(final Context context, String msg) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.dialog_warning, null);// 得到加载view
        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_warning_view);  // 加载布局
        TextView tipTextView = (TextView) v.findViewById(R.id.warning_tip_tv);          // 提示文字
        final Button confirm = v.findViewById(R.id.warning_confirm_btn);
        new CountDownTimer(10000, 1000) {              //确认按键倒计时
            @Override
            public void onTick(long millisUntilFinished) {
                confirm.setText(context.getResources().getString(R.string.confirm) + "(" +
                        String.valueOf(millisUntilFinished / 1000 + 1) + ")" +
                        context.getResources().getString(R.string.s));
            }

            @Override
            public void onFinish() {
                confirm.setText(R.string.confirm);
                confirm.setTextColor(context.getResources().getColor(R.color.white));
                confirm.setEnabled(true);
            }
        }.start();
        tipTextView.setText(msg);// 设置加载信息

        Dialog warningDialog = new Dialog(context, R.style.DialogStyle);// 创建自定义样式dialog
        warningDialog.setCancelable(true); // 是否可以按“返回键”消失
        warningDialog.setCanceledOnTouchOutside(false); // 点击加载框以外的区域
        warningDialog.setContentView(layout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));// 设置布局
        /**
         *将显示Dialog的方法封装在这里面
         */
        Window window = warningDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setGravity(Gravity.CENTER);
        window.setAttributes(lp);
        window.setWindowAnimations(R.style.PopWindowAnimStyle);

        return warningDialog;
    }

    public static Dialog createPromptDialog(final Activity mActivity, String msg, final Class<?> cls) {

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("提示")
                .setMessage(msg)
                .setCancelable(true)
                .setPositiveButton(mActivity.getResources().getString(R.string.confirm),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (cls != null) {

                                }
                            }
                        })
                .setNegativeButton(mActivity.getResources().getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
        Dialog promptDialog = builder.create();
        /**
         *将显示Dialog的方法封装在这里面
         */
        Window window = promptDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setGravity(Gravity.CENTER);
        window.setAttributes(lp);
        window.setWindowAnimations(R.style.PopWindowAnimStyle);
        promptDialog.show();

        return promptDialog;

    }

    public static Dialog createAlertDialog(Context context, String msg) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.dialog_tips, null);// 得到加载view
        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_tips_ll);  // 加载布局
        TextView tipTextView = (TextView) v.findViewById(R.id.tips_tv);          // 提示文字
        Button cancelButton = v.findViewById(R.id.dialog_cancel_btn);

        tipTextView.setText(msg);// 设置加载信息

        final Dialog alertDialog = new Dialog(context, R.style.DialogStyle);// 创建自定义样式dialog
        alertDialog.setCancelable(true); // 是否可以按“返回键”消失
        alertDialog.setCanceledOnTouchOutside(true); // 点击加载框以外的区域
        alertDialog.setContentView(layout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));// 设置布局
        /**
         *将显示Dialog的方法封装在这里面
         */
        Window window = alertDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setGravity(Gravity.CENTER);
        window.setAttributes(lp);
        window.setWindowAnimations(R.style.PopWindowAnimStyle);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });

        return alertDialog;
    }

    public static BottomSheetDialog createBottomSheetDialog(Context context, @LayoutRes int layoutResId, @IdRes int bottom_sheet) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(context);    //实例化
        bottomSheet.setCancelable(true);    //设置点击外部是否可以取消
        bottomSheet.setContentView(layoutResId);   //设置对框框中的布局
        if (bottom_sheet != 0) {
            bottomSheet.getDelegate().findViewById(bottom_sheet).setBackgroundColor(
                    context.getResources().getColor(R.color.transparent)    //设置背景为透明
            );
        }
        bottomSheet.setCanceledOnTouchOutside(true);
        return bottomSheet;
    }

    public static Dialog createTipsDialog(Context context, String msg) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.dialog_tips_with_confirm_cancel, null);// 得到加载view
        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_tips_with_confirm_cancel_ll);  // 加载布局
        TextView tipTextView = (TextView) v.findViewById(R.id.tips_tv);          // 提示文字

        tipTextView.setText(msg);// 设置加载信息

        final Dialog tipsDialog = new Dialog(context, R.style.DialogStyle);// 创建自定义样式dialog
        tipsDialog.setCancelable(true); // 是否可以按“返回键”消失
        tipsDialog.setCanceledOnTouchOutside(true); // 点击加载框以外的区域
        tipsDialog.setContentView(layout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));// 设置布局
        /**
         *将显示Dialog的方法封装在这里面
         */
        Window window = tipsDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setGravity(Gravity.CENTER);
        window.setAttributes(lp);
        window.setWindowAnimations(R.style.PopWindowAnimStyle);

        return tipsDialog;
    }

    public static Dialog createTempPwdDialog(final Context context, final String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.temp_pwd))
                .setMessage(msg)
                .setNeutralButton(R.string.click_to_copy, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            ClipboardManager clipboardManager = (ClipboardManager) context.
                                    getSystemService(Context.CLIPBOARD_SERVICE);

                            ClipData clipData = ClipData.newPlainText(context.getResources().getString(R.string.temp_pwd), msg);
                            clipboardManager.setPrimaryClip(clipData);
                            Toast.makeText(context, context.getResources().getString(R.string.replicating_success), Toast.LENGTH_SHORT).show();

                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        dialog.dismiss();
                    }
                });
        Dialog mTempPwdDialog = builder.create();

        Window window = mTempPwdDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setGravity(Gravity.CENTER);
        window.setAttributes(lp);
        window.setWindowAnimations(R.style.PopWindowAnimStyle);

        mTempPwdDialog.show();
        return mTempPwdDialog;
    }

    public static CustomDialog showQRDialog(final Context context, final Bitmap bitmap) {

        CustomDialog.Builder builder = new CustomDialog.Builder(context);
        final CustomDialog dialog = builder.cancelTouchout(false)
                .view(R.layout.dialog_create_qr)
                .heightDimenRes(R.dimen.dialog_height)
                .widthDimenRes(R.dimen.dialog_width)
                .style(R.style.CustomDialog)
                .addViewOnclick(R.id.share_btn, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SystemUtils.shareImg(bitmap, context);
                    }
                }).build();

        dialog.show();
        return dialog;
    }

    public static AlertDialog showEditDialog(final Context context, String msg, final DeviceUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final EditText editText = new EditText(context);
        builder.setTitle(msg)
                .setView(editText)
                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setPositiveButton(context.getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = editText.getText().toString().trim();
                user.setUserName(name);
                DeviceUserDao.getInstance(context).updateDeviceUser(user);
            }
        });
        AlertDialog dialog = builder.create();
        return dialog;
    }

    public static AlertDialog showEditKeyDialog(final Context context, String msg, final DeviceKey key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final EditText editText = new EditText(context);
        builder.setTitle(msg)
                .setView(editText)
                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setPositiveButton(context.getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = editText.getText().toString().trim();
                key.setKeyName(name);
                DeviceKeyDao.getInstance(context).updateDeviceKey(key);
            }
        });
        AlertDialog dialog = builder.create();
        return dialog;
    }

    /**
     * 关闭dialog
     *
     * @param dialog
     */
    public static void closeDialog(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
