/*
 * Copyright Notice:
 *      Copyright  1998-2008, Huawei Technologies Co., Ltd.  ALL Rights Reserved.
 *
 *      Warning: This computer software sourcecode is protected by copyright law
 *      and international treaties. Unauthorized reproduction or distribution
 *      of this sourcecode, or any portion of it, may result in severe civil and
 *      criminal penalties, and will be prosecuted to the maximum extent
 *      possible under the law.
 */

package com.smart.lock.utils;

import android.text.Editable;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Formatter;
import java.util.zip.CRC32;

public class StringUtil {

    public static boolean strIsNullOrEmpty(String s) {
        return (null == s || s.trim().length() < 1);
    }

    /**
     * 检验是否为空
     *
     * @param obj
     * @return boolean
     */
    public static boolean checkNotNull(Object obj) {
        if (null == obj || "".equals(obj)) {
            return false;
        } else {
            if (obj.toString().trim().length() <= 0) {
                return false;
            }
            return true;
        }
    }

    /**
     * 检验是否为空
     *
     * @param str
     * @return boolean
     */
    public static boolean checkNotNull(String str) {
        if (null == str || str.trim().equals("") || str.length() == 0
                || "null".equalsIgnoreCase(str)) {
            return false;
        } else {
            return true;
        }
    }
    /**
     * 检验MAC地址是否合规
     *
     * @param str MAC地址
     * @return boolean
     */
    public static String checkBleMac(String str) {
        if(str.length() == 12){
            return StringUtil.getMacAdr(str.toUpperCase());
        }else if(str.length() == 17){
            return str;
        }else {
            return "";
        }
    }

    public static boolean checkIsNull(String str) {
        if (null == str || str.trim().equals("") || str.length() == 0
                || "null".equalsIgnoreCase(str)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 检验输入框是否为空
     *
     * @param editable
     * @return
     */
    public static boolean checkEditableNotNull(Editable editable) {
        if (editable == null) {
            return false;
        } else if (!checkNotNull(editable.toString())) {
            return false;
        } else {
            return true;
        }
    }


    public static String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        hexChars = Arrays.copyOfRange(hexChars, 1, hexChars.length);
        return new String(hexChars);
    }

    /**
     * 16进制的字符串表示转成字节数组
     *
     * @param hexStr 16进制格式的字符串
     * @return 转换后的字节数组
     **/
    public static byte[] hexStr2Str(String hexStr) {
        String str = "0123456789ABCDEF";
        hexStr = "0" + hexStr;
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int n;
        for (int i = 0; i < bytes.length; i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
            Log.d("AutoDoorControlActivity", "bytes[" + i + "] = " + bytes[i]);
        }
        return bytes;
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }


    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }


    /**
     * Convert char to byte
     *
     * @param c char
     * @return byte
     */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /**
     * 数字字符串转ASCII码字符串
     *
     * @param content 字符串
     * @return ASCII字符串
     */
    public static String stringToAsciiString(String content, int len) {
        String result = "";
        int max = content.length();
        for (int i = 0; i < max; i++) {
            char c = content.charAt(i);
            String b = Integer.toHexString(Character.getNumericValue(c));
            result = result + (b.length() == 2 ? b : "0" + b);
        }
        while (result.length() < len) {
            result = "0" + result;
        }
        return result;
    }

    /**
     * 十进制码转CharString
     *
     * @param bytes 字节组
     * @return String
     */
    public static String asciiDeBytesToCharString(byte[] bytes) {
//        String res = "";
//        for (byte i : bytes) {
//            res = res +  (char)i;
//        }
//        try {
//            res = new String(res.getBytes(), "GBK");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        return res;
        String s = null;
        try {
            s = new String(bytes, "GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return s;
    }

    /**
     * ASCII码字符串转数字字符串
     *
     * @param content 字符串
     * @return String字符串
     */
    public static String asciisStringToString(String content) {
        StringBuffer sbu = new StringBuffer();
        int max = content.length();
        int index = 0;
        for (int i = 0; i < max / 2; i++) {
            String b = content.substring(index, index += 2);
            sbu.append((byte) Integer.parseInt(b, 16));
        }
        return sbu.toString();
    }

    /**
     * byte2Int().
     */
    public static String byte2Int(byte[] bytes) {
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        int b3 = bytes[3] & 0xFF;
        int result = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        return String.format("%04d", result);
    }

    /**
     * 不够位数的在前面补0，保留num的长度位数字
     *
     * @param code
     * @return
     */
    public static String autoGenericCode(String code, int num) {
        String result = "";
        result = String.format("%0" + num + "d", Integer.parseInt(code) + 1);

        return result;
    }

    /**
     * short2Bytes().
     */
    public static void short2Bytes(short s, byte[] target) {
        target[0] = (byte) (s >> 8 & 0xFF);
        target[1] = (byte) (s & 0xFF);
    }

    /**
     * int2Bytes().
     */
    public static void int2Bytes(int i, byte[] target) {
        target[3] = (byte) (i & 0xFF);
        target[2] = (byte) (i >> 8 & 0xFF);
        target[1] = (byte) (i >> 16 & 0xFF);
        target[0] = (byte) (i >> 24 & 0xFF);
    }

    /**
     * byte2short().
     */
    public static short byte2short(byte[] bytes) {
        byte high = bytes[0];
        byte low = bytes[1];

        return (short) ((high << 8 & 0xFF00) | (low & 0xFF));
    }

    /**
     * crc16(void)
     */
    public static short crc16(byte[] value, int len) {
        int index;
        short temp;
        short crc = (short) 0xFFFF;

        if (null == value || 0 == len)
            return (short) 0xFFFF;

        for (index = 0; index < len; index++) {
            crc = (short) (((crc & 0xFFFF) >>> 8) | (crc << 8));
            temp = (short) (value[index] & 0xFF);
            crc ^= temp;
            crc ^= (short) ((crc & 0xFF) >>> 4);
            crc ^= (short) ((crc << 8) << 4);
            crc ^= (short) (((crc & 0xFF) << 4) << 1);
        }

        return crc;
    }

    // utils
    public static byte[] getSubBytes(byte[] bytes, int start, int end) {
        byte[] sub = new byte[end - start];

        for (int i = start, j = 0; i < end; i++, j++) {
            sub[j] = bytes[i];
        }

        return sub;
    }

    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

    /**
     * CRC校验码
     *
     * @param bytes 输入字节串
     * @return long 输出字符
     */
    public static long getCRC32(byte[] bytes) {
        CRC32 crc32 = new CRC32();
        crc32.update(bytes);
        return crc32.getValue();
    }

    public static String Bytes2HexString(byte[] bytes) {
        final StringBuilder stringBuilder = new StringBuilder(bytes.length);
        for (byte byteChar : bytes) {
            stringBuilder.append(String.format("%02X ", byteChar));
        }

        return stringBuilder.toString();
    }

    /**
     * 将矩阵转置
     *
     * @param temp 转换数组
     */
    public static void exchange(byte temp[]) {
        int head = 0;
        int tail = temp.length - 1;
        for (int i = 0; i < temp.length / 2; i++) {
            int k = temp[head];
            temp[head] = temp[tail];
            temp[tail] = (byte) k;
            head++;
            tail--;
        }
    }

    public static String getFileName(String pathandname) {
        int start = pathandname.lastIndexOf("/");
        int end = pathandname.lastIndexOf(".");
        if (start != -1 && end != -1) {
            return pathandname.substring(start + 1, end);
        } else {
            return null;
        }
    }

    /**
     * 版本号比较
     *
     * @param newVer
     * @param oldVer
     * @return
     */
    public static int compareVersion(String newVer, String oldVer) {
        if (newVer.equals(oldVer)) {
            return 0;
        }
        String[] newVerArray = newVer.split("\\.");
        String[] oldVerArray = oldVer.split("\\.");
        int index = 0;
        // 获取最小长度值
        int minLen = Math.min(newVerArray.length, oldVerArray.length);
        int diff = 0;
        // 循环判断每位的大小
        while (index < minLen && (diff = Integer.parseInt(newVerArray[index])
                - Integer.parseInt(oldVerArray[index])) == 0) {
            index++;
        }
        if (diff == 0) {
            // 如果位数不一致，比较多余位数
            for (int i = index; i < newVerArray.length; i++) {
                if (Integer.parseInt(newVerArray[i]) > 0) {
                    return 1;
                }
            }

            for (int i = index; i < oldVerArray.length; i++) {
                if (Integer.parseInt(oldVerArray[i]) > 0) {
                    return -1;
                }
            }
            return 0;
        } else {
            return diff > 0 ? 1 : -1;
        }
    }

    public static String getBytes(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            int res = bytes[i];
            if (res < 0) {
                res = 256 + res;
            }
            sb.append(res);
            if (sb.length() > 0 && sb.length() != bytes.length) sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * 转成标准MAC地址
     *
     * @param str 未加：的MAC字符串
     * @return 标准MAC字符串
     */
    public static String getMacAdr(String str) {
        str = str.toUpperCase();
        StringBuilder result = new StringBuilder("");
        for (int i = 1; i <= 12; i++) {
            result.append(str.charAt(i - 1));
            if (i % 2 == 0) {
                result.append(":");
            }
        }
        return result.substring(0, 17);
    }

    /**
     * 删除指定字符
     * @param str
     * @param delChar
     * @return
     */
    public static String deleteString(String str, char delChar){
        String delStr = "";
        for (int i = 0; i < str.length(); i++) {
            if(str.charAt(i) != delChar){
                delStr += str.charAt(i);
            }
        }
        return delStr;
    }
    /**
     * 反转byte数组中的某一段
     *
     * @param arr
     * @param begin
     * @param end
     * @return
     */
    public static byte[] reverse(byte[] arr, int begin, int end) {

        while (begin < end) {
            byte temp = arr[end];
            arr[end] = arr[begin];
            arr[begin] = temp;
            begin++;
            end--;
        }

        return arr;
    }
    /**
     * byte数组转成十六进制字符串
     *
     * @param array     原数组
     * @param separator 分隔符
     * @return
     */
    public static String bytesToHexString(byte[] array, String separator) {
        if (array == null || array.length == 0)
            return "";

        StringBuilder sb = new StringBuilder();

        Formatter formatter = new Formatter(sb);
        formatter.format("%02X", array[0]);

        for (int i = 1; i < array.length; i++) {

            if (!Strings.isEmpty(separator))
                sb.append(separator);

            formatter.format("%02X", array[i]);
        }

        formatter.flush();
        formatter.close();

        return sb.toString();
    }
}
 final class Strings {

    private Strings() {
    }

    public static byte[] stringToBytes(String str, int length) {

        byte[] srcBytes;

        if (length <= 0) {
            return str.getBytes(Charset.defaultCharset());
        }

        byte[] result = new byte[length];

        srcBytes = str.getBytes(Charset.defaultCharset());

        if (srcBytes.length <= length) {
            System.arraycopy(srcBytes, 0, result, 0, srcBytes.length);
        } else {
            System.arraycopy(srcBytes, 0, result, 0, length);
        }

        return result;
    }

    public static byte[] stringToBytes(String str) {
        return stringToBytes(str, 0);
    }

    public static String bytesToString(byte[] data) {
        return data == null || data.length <= 0 ? null : new String(data, Charset.defaultCharset()).trim();
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}