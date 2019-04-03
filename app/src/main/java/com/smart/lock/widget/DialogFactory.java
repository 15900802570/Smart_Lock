package com.smart.dt.widget;



import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.smart.dt.R;

import java.util.Timer;
import java.util.TimerTask;


/***
 * 弹出框
 * @author smz
 */
public class DialogFactory {
	public static final int TOAST = 1;
	public static final int ALERT_DIALOG = 2;
	private YishuaDialog yishuaDialog;
	private Activity mContext;
	private Timer timer;
	
	private static DialogFactory factory;
	
	public DialogFactory(Activity c) {
		mContext = c;
		yishuaDialog = new YishuaDialog(mContext);
	}
	
	public Context getmContext() {
		return mContext;
	}

	/***
	 * 检验为空弹出框
	 * @param showType  弹出类型    Toast or  Dialog
	 * @param msg       弹出的内容
	 */
	public void showNullDialog(int showType,String msg)
	{
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
	 * @param ok
	 */
	public YishuaDialog getAlter(int title, int msg) {
		getAlter(getString(title), getString(msg));
		return yishuaDialog;
	}
	
	public YishuaDialog getAlter(String title, String msg) {
		if(yishuaDialog != null )
		{
			yishuaDialog = null;
		}
		yishuaDialog = new YishuaDialog(mContext);
		yishuaDialog
		.setTit(title)
		.setMessage(msg);
		return yishuaDialog;
	}
	
	public YishuaDialog getAlter() {
		if(yishuaDialog != null )
		{
			yishuaDialog = null;
		}
		yishuaDialog = new YishuaDialog(mContext);
		return yishuaDialog;
	}
	
	/***
	 * 得到一个对话框对象
	 * 
	 * @param title
	 * @param msg
	 * @param ok
	 */
	public YishuaDialog getAlter( int msg) {
		getAlter(getString(msg));
		return yishuaDialog;
	}
	
	public YishuaDialog getAlter( String msg) {
		if(yishuaDialog != null )
		{
			yishuaDialog = null;
		}
		yishuaDialog = new YishuaDialog(mContext);
		yishuaDialog.setMessage(msg);
		return yishuaDialog;
	}

	public void show(String msg) {
		show(msg, getString(R.string.ok));
	}
	public void show(int msg) {
		show(getString(msg));
	}
	
	public void show(String msg,long time) {
		if(yishuaDialog != null )
		{
			yishuaDialog = null;
		}
		yishuaDialog = new YishuaDialog(mContext);
		yishuaDialog
		.setButtonVisible(YishuaDialog.DIALOG_NO_BUTTON_VISIBLE)
		.setMessage(msg)
		.show();
		if(timer==null)
		{
			timer = new Timer();
		}
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				if(yishuaDialog != null)
				yishuaDialog.cancel();
				this.cancel();
				timer.cancel();
				timer = null;
			}
		}, time);
	}


	public void show(int title, int msg, int ok) {
		if(yishuaDialog != null )
		{
			yishuaDialog = null;
		}
		yishuaDialog = new YishuaDialog(mContext);
		yishuaDialog
		.setTit(title)
		.setOkButtonText(getString(ok))
		.setOkClick(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				yishuaDialog.cancel();
			}
		})
		.setMessage(msg)
		.show();
	}
	
	public void show(int msg, int ok) {
		show(getString(msg), getString(ok));
	}

	public void show(String msg, String ok) {
		if(yishuaDialog != null )
		{
			yishuaDialog = null;
		}
		yishuaDialog = new YishuaDialog(mContext);
		yishuaDialog
		.setOkButtonText(ok)
		.setOkClick(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				yishuaDialog.cancel();
			}
		})
		.setMessage(msg)
		.show();
		
	}
	
	/***
	 * YiShua弹出框消失
	 */
	public void cancelYiShuaDialog()
	{
		if(yishuaDialog!=null)
		{
			yishuaDialog.cancel();
		}
	}
	
	private String getString(int stringId) {
		if(mContext!=null)
		{
			return mContext.getString(stringId);
		}else{
			return null;
		}
	}
	
	public void setCancel(boolean flag)
	{
		yishuaDialog.setCancelable(flag);
	}

	public static DialogFactory getInstance(Activity context) {
		
		//if(factory==null) {
			factory=new DialogFactory(context);
//		}
//		if(context.isFinishing()){
//			factory=
//		}
		return factory;
	}

	
}
