package com.smart.lock.widget;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.smart.lock.R;

import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

public class Controller {
    private static Controller controller;
    private Activity activity;
    private List<Activity> activitys = new LinkedList();

    class WaitForReleaseEngineTask extends TimerTask {
        private Activity context;

        public WaitForReleaseEngineTask(Activity context) {
            this.context = context;
        }

        public void run() {
            if (activitys != null && activitys.size() > 0) {
                for (Activity activity : activitys) {
                    activity.finish();
                }
            }
            if (context != null) {
                context.finish();
            }
            if (activity != null) {
                ActivityManager am = (ActivityManager) activity.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
                am.restartPackage(activity.getParent().getParent().getPackageName());
                am.restartPackage(activity.getPackageName());
            }
            System.exit(0);
        }
    }

    public void setAcitivty(Activity activity) {
        this.activity = activity;
    }

    public static Controller getInstants() {
        if (controller == null) {
            controller = new Controller();
        }
        return controller;
    }

    public Dialog alertDialog(String str) {
        Dialog dialog = createAppDialog(R.layout.dialog);
        dialog.show();
        ((TextView) dialog.findViewById(R.id.contentView)).setText(str);
        return dialog;
    }

    public Dialog alertFailDialog(String str) {
        Dialog dialog = createAppDialog(R.layout.dialog_fail);
        dialog.show();
        ((TextView) dialog.findViewById(R.id.contentView)).setText(str);
        return dialog;
    }

    public Dialog createAppDialog(int layout) {
        AppDialog dialog = new AppDialog(activity, layout, -1, -2);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public Dialog loadDialog(String str) {
        Dialog dialog = createAppDialog(R.layout.login_load);
        dialog.show();
        ((TextView) dialog.findViewById(R.id.contentView)).setText(str);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }


    public void setUHomeMain(Activity activity) {
        activitys.add(activity);
    }
}