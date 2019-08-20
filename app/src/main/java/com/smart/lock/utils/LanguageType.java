package com.smart.lock.utils;

/**
 * Created by dumingwei on 2018/5/31 0031.
 */
public enum LanguageType {

    CHINESE("简体中文"),
    ENGLISH("English");

    private String language;

    LanguageType(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language == null ? "" : language;
    }
}
