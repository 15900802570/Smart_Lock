package com.smart.lock.utils;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.CountDownTimer;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.design.widget.BottomSheetDialog;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smart.lock.R;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.ui.SelfCheckActivity;
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

        LogUtil.d(TAG, "context instanceof SelfCheckActivity " + (context instanceof SelfCheckActivity));
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
        LinearLayout layout = v.findViewById(R.id.dialog_warning_view);  // 加载布局
        TextView tipTextView = v.findViewById(R.id.warning_tip_tv);          // 提示文字
        final Button confirm = v.findViewById(R.id.warning_confirm_btn);
        new CountDownTimer(5000, 1000) {              //确认按键倒计时
            @SuppressLint("SetTextI18n")
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

    public static Dialog createEditorDialog(final Context context, String title, String msg) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.dialog_editor, null);// 得到加载view
        LinearLayout layout = v.findViewById(R.id.dialog_editor_ll);  // 加载布局
        TextView titleTextView = v.findViewById(R.id.dialog_title_tv);
        final EditText editTextView = v.findViewById(R.id.editor_et);          // 提示文字
        final ImageView clearImageView = v.findViewById(R.id.dialog_clear_iv);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editTextView.getEditableText().length() >= 1) {
                    clearImageView.setVisibility(View.VISIBLE);
                } else {
                    clearImageView.setVisibility(View.GONE);
                }
            }
        };

        editTextView.addTextChangedListener(textWatcher);

        clearImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editTextView.setText("");
            }
        });

        titleTextView.setText(title);
        editTextView.setText(msg);// 设置加载信息

        final Dialog editorDialog = new Dialog(context, R.style.DialogStyle);// 创建自定义样式dialog
        editorDialog.setCancelable(true); // 是否可以按“返回键”消失
        editorDialog.setCanceledOnTouchOutside(true); // 点击加载框以外的区域
        editorDialog.setContentView(layout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));// 设置布局

        /**
         *将显示Dialog的方法封装在这里面
         */
        Window window = editorDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setGravity(Gravity.CENTER);
        window.setAttributes(lp);
        window.setWindowAnimations(R.style.PopWindowAnimStyle);

        v.findViewById(R.id.dialog_cancel_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorDialog.dismiss();
            }
        });

        return editorDialog;

    }

    public static Dialog createTipsDialogWithCancel(Context context, String msg) {
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_tips_with_cancel, null);// 得到加载view
        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_tips_with_cancel_ll);  // 加载布局
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

    public static Dialog createTipsDialogWithConfirm(Context context, String msg) {
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_tips_with_confirm, null);// 得到加载view
        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_tips_with_confirm_ll);  // 加载布局
        TextView tipTextView = (TextView) v.findViewById(R.id.tips_tv);          // 提示文字
        Button confirmButton = v.findViewById(R.id.dialog_confirm_btn);

        tipTextView.setText(msg);// 设置加载信息

        final Dialog confirmDialog = new Dialog(context, R.style.DialogStyle);// 创建自定义样式dialog
        confirmDialog.setContentView(layout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));// 设置布局
        /**
         *将显示Dialog的方法封装在这里面
         */
        Window window = confirmDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setGravity(Gravity.CENTER);
        window.setAttributes(lp);
        window.setWindowAnimations(R.style.PopWindowAnimStyle);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDialog.cancel();
            }
        });

        return confirmDialog;
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

    public static Dialog createTipsDialogWithConfirmAndCancel(Context context, String msg) {
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_tips_with_confirm_cancel, null);// 得到加载view
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
        v.findViewById(R.id.dialog_cancel_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tipsDialog.dismiss();
            }
        });

        return tipsDialog;
    }

//    public static Dialog createTimePickerDialog(final Context context,
//                                                final int startHour, final int startMin,
//                                                int endHour, int endMin) {
//        LayoutInflater inflater = LayoutInflater.from(context);
//        @SuppressLint("InflateParams") final View v = inflater.inflate(R.layout.dialog_set_time_period, null);// 得到加载view
//        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_time_picker_ll);  // 加载布局
//        final NumberPicker startHourNP = v.findViewById(R.id.start_hour_np);
//        final NumberPicker startMinuteNP = v.findViewById(R.id.start_minute_np);
//        final NumberPicker endHourNP = v.findViewById(R.id.end_hour_np);
//        final NumberPicker endMinuteNP = v.findViewById(R.id.end_minute_np);
//        String[] Hour = {
//                "00", "01", "02", "03", "04", "05", "06", "07", "08", "09",
//                "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
//                "20", "21", "22", "23"
//        };
//        String[] Minute = {
//                "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10",
//                "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
//                "21", "22", "23", "24", "25", "26", "27", "28", "29", "30",
//                "31", "32", "33", "34", "35", "36", "37", "38", "39", "40",
//                "41", "42", "43", "44", "45", "46", "47", "48", "49", "50",
//                "51", "52", "53", "54", "55", "56", "57", "58", "59"
//        };
//        startHourNP.setDisplayedValues(Hour);
//        startMinuteNP.setDisplayedValues(Minute);
//        endHourNP.setDisplayedValues(Hour);
//        endMinuteNP.setDisplayedValues(Minute);
//
//        startHourNP.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
//        startMinuteNP.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
//        endHourNP.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
//        endMinuteNP.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
//
//        startHourNP.setMinValue(1);
//        startMinuteNP.setMinValue(1);
//        endHourNP.setMinValue(1);
//        endMinuteNP.setMinValue(1);
//
//        startHourNP.setMaxValue(Hour.length);
//        startMinuteNP.setMaxValue(Minute.length);
//        endHourNP.setMaxValue(Hour.length);
//        endMinuteNP.setMaxValue(Minute.length);
//
//        startHourNP.setValue(startHour);
//        startMinuteNP.setValue(startMin);
//        endHourNP.setValue(endHour);
//        endMinuteNP.setValue(endMin);
//        final int[] value = {0, 1, 2, 3};
//
//        startHourNP.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
//            @Override
//            public void onValueChange(NumberPicker numberPicker, int oldValue, int newValue) {
//                value[0] = startHourNP.getValue();
//            }
//        });
//
//        startMinuteNP.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
//
//            @Override
//            public void onValueChange(NumberPicker numberPicker, int oldValue, int newValue) {
//                value[1] = startMinuteNP.getValue();
//            }
//        });
//
//        endHourNP.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
//            @Override
//            public void onValueChange(NumberPicker numberPicker, int oldValue, int newValue) {
//                value[2] = endHourNP.getValue();
//            }
//        });
//
//        endMinuteNP.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
//
//            @Override
//            public void onValueChange(NumberPicker numberPicker, int oldValue, int newValue) {
//                value[3] = endMinuteNP.getValue();
//            }
//        });
//
//
////        tipTextView.setText(msg);// 设置加载信息
//
//        final Dialog timePickerDialog = new Dialog(context, R.style.DialogStyle);// 创建自定义样式dialog
//        timePickerDialog.setCancelable(true); // 是否可以按“返回键”消失
//        timePickerDialog.setCanceledOnTouchOutside(true); // 点击加载框以外的区域
//        timePickerDialog.setContentView(layout, new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.MATCH_PARENT));// 设置布局
//
//        Window window = timePickerDialog.getWindow();
//        WindowManager.LayoutParams lp = window.getAttributes();
//        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
//        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
//        window.setGravity(Gravity.CENTER);
//        window.setAttributes(lp);
//        window.setWindowAnimations(R.style.PopWindowAnimStyle);
//
//        timePickerDialog.findViewById(R.id.dialog_cancel_btn).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                LogUtil.d(TAG, "LOG = " + value[0]+"\n"+
//                        value[1]+"\n"+
//                        value[2]+"\n"+
//                        value[3]);
//                timePickerDialog.dismiss();
//            }
//        });
//
//        return timePickerDialog;
//    }

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
