package com.smart.lock.ble;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AES_ECB_PKCS7 {
    public static boolean initialized = false;
    public static final String ALGORITHM = "AES/ECB/NoPadding";

    public static void initialize() {
        if (initialized)
            return;

        initialized = true;
        Security.addProvider(new BouncyCastleProvider());
    }

    public static int AES128Encode(byte[] plain, byte[] encrypt, byte[] key) {
        if (0 == plain.length || 0 == encrypt.length || 0 == key.length)
            return -1;

        if (16 != key.length) return -1;

        if (0 != (plain.length % 16)) return -1;

        initialize();

        try {
            byte[] result;

            Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            result = cipher.doFinal(plain);

            if (encrypt.length < result.length)
                return -1;

            for (int i = 0; i < result.length; i++)
                encrypt[i] = result[i];

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static int AES128Decode(byte[] decrypt, byte[] plain, byte[] key) {
        if (0 == plain.length || 0 == decrypt.length || 0 == key.length)
            return -1;

        if (16 != key.length) return -1;

        if (0 != (decrypt.length % 16)) return -1;

        initialize();

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            byte[] result = cipher.doFinal(decrypt);

            if (plain.length < result.length)
                return -1;

            for (int i = 0; i < result.length; i++)
                plain[i] = result[i];

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static int AES256Encode(byte[] plain, byte[] encrypt, byte[] key) {
        if (0 == plain.length || 0 == encrypt.length || 0 == key.length)
            return -1;

        if (32 != key.length) return -1;

        if (0 != (plain.length % 16)) return -1;

        initialize();

        try {
            byte[] result;

            Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            result = cipher.doFinal(plain);

            if (encrypt.length < result.length)
                return -1;

            for (int i = 0; i < result.length; i++)
                encrypt[i] = result[i];

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static int AES256Decode(byte[] decrypt, byte[] plain, byte[] key) {
        if (0 == plain.length || 0 == decrypt.length || 0 == key.length)
            return -1;

        if (32 != key.length) return -1;

        if (0 != (decrypt.length % 16)) return -1;

        initialize();

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            byte[] result = cipher.doFinal(decrypt);

            if (plain.length < result.length)
                return -1;

            for (int i = 0; i < result.length; i++)
                plain[i] = result[i];

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
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

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static String byteArr2HexStr(byte[] arr) throws Exception {

        int iLen = arr.length;
        StringBuffer sb = new StringBuffer(iLen * 2);
        for (int i = 0; i < iLen; i++) {
            int intTmp = arr[i];
            while (intTmp < 0) {
                intTmp = intTmp + 256;
            }
            if (intTmp < 16) {
                sb.append("0");
            }
            sb.append(Integer.toString(intTmp, 16));
        }
        return sb.toString();
    }
}
