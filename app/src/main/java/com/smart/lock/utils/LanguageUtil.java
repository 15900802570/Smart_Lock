package com.smart.lock.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.Locale;

/**
 *
 */
public class LanguageUtil {

    private static final String TAG = "LanguageUtil";

    /**
     * @param context
     * @param newLanguage 想要切换的语言类型 比如 "en" ,"zh"
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("deprecation")
    public static void changeAppLanguage(Context context, String newLanguage) {
        if (TextUtils.isEmpty(newLanguage)) {
            return;
        }
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        //获取想要切换的语言类型
        Locale locale = getLocaleByLanguage(newLanguage);
        //区别17版本（其实在17以上版本通过 config.locale设置也是有效的，不知道为什么还要区别）
        //在这里设置需要转换成的语言，也就是选择用哪个values目录下的strings.xml文件
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);//设置简体中文
            //config.setLocale(Locale.ENGLISH);//设置英文
        } else {
            configuration.locale = locale;//设置简体中文
            //config.locale = Locale.ENGLISH;//设置英文
        }
        // updateConfiguration
        DisplayMetrics dm = resources.getDisplayMetrics();
        resources.updateConfiguration(configuration, dm);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static Locale getLocaleByLanguage(String language) {
        Locale locale = Locale.SIMPLIFIED_CHINESE;
        if (language.equals(LanguageType.CHINESE.getLanguage())) {
            locale = Locale.SIMPLIFIED_CHINESE;
        } else if (language.equals(LanguageType.ENGLISH.getLanguage())) {
            locale = Locale.ENGLISH;
        }
        Log.d(TAG, "getLocaleByLanguage: " + locale.getDisplayName());
        return locale;
    }

    public static Context attachBaseContext(Context context, String language) {
        Log.d(TAG, "attachBaseContext: " + Build.VERSION.SDK_INT);
        Log.d(TAG, "attachBaseContext: " + Build.VERSION_CODES.N);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResources(context, language);
        } else {
            return context;
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Context updateResources(Context context, String language) {
        Resources resources = context.getResources();
        Locale locale = LanguageUtil.getLocaleByLanguage(language);

        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        configuration.setLocales(new LocaleList(locale));
        return context.createConfigurationContext(configuration);
    }
}
