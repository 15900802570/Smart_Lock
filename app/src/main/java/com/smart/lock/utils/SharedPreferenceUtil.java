package com.smart.lock.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;


public class SharedPreferenceUtil {
	private static SharedPreferenceUtil myPrefs;//私有化
	private SharedPreferences sp;
	//提供私有的构造方法
	private SharedPreferenceUtil(Context context){
		initSharedPreferences(context);
	}
	/**
	 * 对外提供的初始化方法
	 * @return
	 */
	public static SharedPreferenceUtil getInstance(Context context){
		//初始化自身对象
		if(myPrefs == null){
			myPrefs = new SharedPreferenceUtil(context);
		}
		return myPrefs;
	}

	/**
	 * 初始化SharedPreferences对象
	 * @param context
	 */
	public SharedPreferenceUtil initSharedPreferences(Context context){
		//获取SharedPreferences对象
		if(sp == null){
			sp = context.getSharedPreferences(ConstantUtil.PREF_NAME,
					Context.MODE_PRIVATE);
		}
		return myPrefs;
	}

	/**
	 * 向SharedPreferences中写入String类型的数据
	 * @param key
	 * @param value
	 */
	public void writeString(String key, String value){
		//获取编辑器对象
		Editor editor = sp.edit();
		//写入数据
		editor.putString(key, value);
		editor.commit();
//		editor.apply();//提交写入的数据
	}
	/**
	 * 向SharedPreferences中写入Bool类型的数据
	 * @param key
	 * @param value
	 */
	public void writeBoolean(String key, Boolean value){
		//获取编辑器对象
		Editor editor = sp.edit();
		//写入数据
		editor.putBoolean(key, value);
//		editor.commit();
		editor.apply();//提交写入的数据
	}

	public void writeInt(String key, int value){
		Editor editor = sp.edit();
		editor.putInt(key,value);
		editor.apply();
	}

	public void writeLong(String key, long value){
		Editor editor = sp.edit();
		editor.putLong(key,value);
		editor.apply();
	}

	/**
	 * 根据key读取SharedPreferences中的String类型的数据
	 * @param key
	 * @return
	 */
	public String readString(String key){
		return sp.getString(key, "");
	}
	/**
	 * 根据key读取SharedPreferences中的Bool类型的数据
	 * @param key
	 * @return
	 */
	public Boolean readBoolean(String key){
		return sp.getBoolean(key, false);
	}

	public int readInt(String key){
		return sp.getInt(key, 0);
	}

	public long readLong(String key){
		return sp.getLong(key, 0);
	}
}
