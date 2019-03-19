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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
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
     * @param bytes 字节组
     * @return String
     */
    public static String AsciiDeBytesToCharString(byte[] bytes){
        String string = "";
        for (int i : bytes){
            string = string + (char)i;
        }
        return string;
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
}
