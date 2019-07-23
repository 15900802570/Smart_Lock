package com.smart.lock.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;


public class FileUtil {

    public static final String TAG = "FileUtil";

    // public static String filePath;


    /**
     * 检查SD卡是否存在
     *
     * @return boolean
     */
    public static boolean checkSDCard() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 检查文件是否存在
     */
    public static String checkDirPath(String dirPath) {
        if (TextUtils.isEmpty(dirPath)) {
            return "";
        }
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dirPath;
    }

    /**
     * 保存目录目录到目录
     *
     * @param context
     * @return 目录保存的目录
     */
    public static String createDir(Context context) {
        return createDir(context, null);
    }

    /**
     * 保存目录目录到目录
     *
     * @param context
     * @return 目录保存的目录
     */
    public static String createDir(Context context, String dirName) {
        if (dirName == null) {
            dirName = "";
        }
        String filePath;
        if (checkSDCard()) {
            filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator + ConstantUtil.LOCK_DIR_NAME + File.separator
                    + dirName;
        } else {
            filePath = context.getCacheDir().getAbsolutePath() + File.separator
                    + "realname" + File.separator + dirName;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
            LogUtil.e("file", "目录不存在  创建目录    ");
        } else {
            LogUtil.e("file", "目录存在");
        }
        LogUtil.i("file", "下载的路径：" + filePath);
        return filePath;
    }

    /***
     * 转化文件大小
     *
     * @param fileSize
     * @return
     */
    public static String formatFileSize(long fileSize) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileSize < 1024) {
            fileSizeString = df.format((double) fileSize) + "B";
        } else if (fileSize < 1048576) {
            fileSizeString = df.format((double) fileSize / 1024) + "K";
        } else if (fileSize < 1073741824) {
            fileSizeString = df.format((double) fileSize / 1048576) + "M";
        } else {
            fileSizeString = df.format((double) fileSize / 1073741824) + "G";
        }
        return fileSizeString;
    }

    /***
     * 转化文件大小
     *
     * @param
     * @return fileSize
     */
    public static String formatFileSize(String filePath) {
        File file = new File(filePath);
        return formatFileSize(file.length());
    }

    public static String bitmapCompress(String fromPath, String toFileName) {
        String toFile = Environment.getExternalStorageDirectory()
                + File.separator + ".shouzhangbao" + File.separator + "temppic"
                + File.separator + "pic";
        File file = new File(toFile);
        if (!file.exists()) {
            file.mkdirs();
        }
        File fromFile = new File(fromPath);
        long fileSize = fromFile.length();
        LogUtil.i(TAG, "文件大小：" + fileSize + "   " + formatFileSize(fileSize));

        if (fileSize < 102400) // 当文件小于100K 则不需要压缩
        {
            if (fromPath.toLowerCase().endsWith(".png")) {
                toFile += toFileName + ".png";
            } else {
                toFile += toFileName + ".jpg";
            }
            copyFile(fromPath, toFile);
        } else {
            toFile += toFileName + ".jpg";
            fromatImage(fromPath, toFile);
        }
        return toFile;
    }

    public static void fromatImage(String fromPath, String toFile) {
        fromatImage(fromPath, toFile, 20);
    }

    public static void fromatImage(String fromPath, String toFile, int quality) {
        LogUtil.i(TAG, "开始压缩图片" + fromPath + " to " + toFile);
        Bitmap bitmap = null;
        // BitmapFactory.Options options = new BitmapFactory.Options();
        // options.inJustDecodeBounds = true;
        // bitmap = BitmapFactory.decodeFile(fromPath, options);
        // // Log.i(TAG,"初始化宽高："+bitmap.getWidth()+"x"+bitmap.getHeight());
        // options.inJustDecodeBounds = false;
        // int outSize = options.outHeight/200;
        // if(outSize <=0)
        // {
        // outSize = 1;
        // }
        // Log.i(TAG,
        // "options.outHeight="+options.outHeight+"  outSize="+outSize);
        // options.inSampleSize = outSize;
        // bitmap = zoomImage(fromPath);

        // BitmapFactory.Options options = new BitmapFactory.Options();
        // options.inJustDecodeBounds = true;
        // BitmapFactory.decodeFile(fromPath, options);
        // options.inSampleSize = computeSampleSize(options,-1, 128*128);
        // options.inJustDecodeBounds = false;

        FileOutputStream stream = null;
        try {
            bitmap = decodeBitmapFromPath(fromPath, 480, 800);
            // bitmap = BitmapFactory.decodeFile(fromPath,options);
            LogUtil.i(TAG, "压缩后宽高：" + bitmap.getWidth() + "x" + bitmap.getHeight());
            stream = new FileOutputStream(toFile);
            bitmap.compress(CompressFormat.JPEG, quality, stream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                stream = null;
            }
            if (bitmap != null) {
                try {
                    bitmap.recycle();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                bitmap = null;
            }
        }
    }

    private static final int COPY_BUFF = 1024 * 1024 * 10;

    public static void copyFile(String fromPath, String toPath) {
        LogUtil.i(TAG, "开始copy文件" + fromPath + " to " + toPath);
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(fromPath);
            outputStream = new FileOutputStream(toPath);
            byte[] bytes = new byte[COPY_BUFF];
            int len = -1;
            while ((len = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                outputStream = null;
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                inputStream = null;
            }
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        int max = (height > width ? height : width);
        if (max > reqWidth) {
            inSampleSize = Math.round(max / (float) reqWidth);
        }
        return inSampleSize;
    }

    public static Bitmap decodeBitmapFromPath(String pathName, int reqWidth,
                                              int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }

    public static Bitmap decodeBitmapFromPath(String pathName) {
        return decodeBitmapFromPath(pathName, 480, 800);
    }

    private static final int UPLOAD_SIZE = 1024 * 300;

    public static String decodePicFromPath(String pathName) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, 1500);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        //按照像数压缩
        String filepath = createTempFileName(pathName);
        try {
            LogUtil.d("fileLength before 111 ..length", "....." + new File(pathName).length());
            if (options.inSampleSize != 1) {
                if (!finishCompress(pathName, filepath, options, 90)) {
                    return null;
                }
            } else {
                filepath = pathName;
            }
            File tempFile = new File(filepath);
            long fileLength = tempFile.length();
            if (fileLength > UPLOAD_SIZE) {
                LogUtil.d("fileLength before ..length", "....." + fileLength);
                int quality = (int) Math.round(UPLOAD_SIZE * 1.0 / fileLength * 100);
                String desPath = createTempFileName(pathName);
                if (finishCompress(filepath, desPath, options, quality)) {
                    return desPath;
                }
            } else {
                //不要删除
                String result = filepath;
                filepath = null;
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //删除临时文件
            if (filepath != null) {
                File tempFile = new File(filepath);
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        }
        return null;
    }

    /**
     * 完成压缩
     */
    private static boolean finishCompress(String srcPath, String desPath, BitmapFactory.Options options, int quality) {
        Bitmap bimap = BitmapFactory.decodeFile(srcPath, options);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(desPath);
            bimap.compress(CompressFormat.JPEG, quality, out);
            LogUtil.i(TAG, "已经保存...." + quality);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (out != null) {
                try {
                    out.flush();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private static String createTempFileName(String pathName) {
        int index = pathName.lastIndexOf(".");
        String tempName = pathName.substring(0, index);
        return tempName + System.currentTimeMillis() + ".jpg";
    }

    public static void compressBmpToFile(Bitmap bmp, File file, int size) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int options = 100;//个人喜欢从80开始,
        bmp.compress(CompressFormat.JPEG, options, baos);
        //LogUtil.d("compressBmpToFile..111", (baos.toByteArray().length / 1024)+"....."+options);
        while (baos.toByteArray().length / 1024 > size) {
            baos.reset();
            options -= 10;
            //LogUtil.d("compressBmpToFile..", (baos.toByteArray().length / 1024)+"....."+options);
            bmp.compress(CompressFormat.JPEG, options, baos);
        }
        FileOutputStream fos = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void writeContentTofile(String filecontent) {

        final String LOG_PATH = "/mnt/sdcard/mylog/realname";
        FileOutputStream logFos = null;
        try {
            File file = new File(LOG_PATH);
            if (!file.exists()) {
                file.mkdirs();
            }
            file = new File(LOG_PATH + "/javalog_" + System.currentTimeMillis() + ".txt");
            if (!file.exists()) {
                file.createNewFile();
            }
            logFos = new FileOutputStream(file, true);
            logFos.write(filecontent.getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (logFos != null) {
                try {
                    logFos.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }
    }

//	public static Bitmap comp(Bitmap decodeFile) {
//		// TODO Auto-generated method stub
//		return null;
//	}


    public static Bitmap comp(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(CompressFormat.JPEG, 50, baos);
        if (baos.toByteArray().length / 1024 > 10) {  //判断如果图片大于1M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
            baos.reset();//重置baos即清空baos
            image.compress(CompressFormat.JPEG, 50, baos);//这里压缩50%，把压缩后的数据存放到baos中
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        float hh = 300f;//这里设置高度为800f
        float ww = 300f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;//设置缩放比例
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        isBm = new ByteArrayInputStream(baos.toByteArray());
        bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
        return compressImage(bitmap);//压缩好比例大小后再进行质量压缩
    }

    public static Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024 > 10) {    //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            image.compress(CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片
        return bitmap;
    }

    public static void clearFiles(String dir) {
        File file = new File(dir);
        if (file.isFile()) {
            file.delete();
        } else {
            File[] files = file.listFiles();
            LogUtil.d(TAG, "files = " + Arrays.toString(files));
            if (files != null) {
                for (File temp : files) {
                    temp.delete();
                }
            }
        }
    }

    public static void clearQr(Context context, String type) {
        String dir = FileUtil.createDir(context, ConstantUtil.QR_DIR_NAME) + File.separator;
        File file = new File(dir);
        if (file.isFile()) {
            file.delete();
        } else {
            File[] files = file.listFiles();
            if (files != null) {
                for (File temp : files) {
                    String fileName = temp.getName().trim().toLowerCase();
                    if (fileName.endsWith(type)) {
                        String name = fileName.substring(0, fileName.lastIndexOf("."));
                        LogUtil.d(TAG, "name : " + name);
                        if (isNumeric(name)) {
                            if (System.currentTimeMillis() - Long.parseLong(name) > 30 * 60 * 60) {
                                temp.delete();
                            }
                        } else temp.delete();

                    } else temp.delete();
                }
            }

        }
    }

    private static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        return isNum.matches();

    }

    public static byte[] loadFirmware(String filename) {
        File file = new File(filename);
        byte[] gCmdBytes = null;

        if (file.isDirectory()) {
            Log.d(TAG, "The File doesn't not exist.");
        } else {
            try {
                InputStream instream = new FileInputStream(file);
                if (instream != null) {

                    int length = instream.available();
                    Log.d(TAG, "openUpdateFile ------------- instream.length():" + length);
                    gCmdBytes = new byte[length];
                    instream.read(gCmdBytes);

                    instream.close();
                }
            } catch (FileNotFoundException e) {
                Log.d(TAG, "The File doesn't not exist.");
            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
            }
        }
        return gCmdBytes;
    }

    public static boolean fileExists(String file_path) {
        Log.d(TAG, "fileExists--exists000!!! file_path=" + file_path);
        try {
            File f = new File(file_path);
            if (f.exists()) {
                return true;
            }

            InputStream instream = new FileInputStream(f);
            int length = instream.available();
            Log.d(TAG, "fileExists--exists!!! length=" + length);
        } catch (Exception ex) {
            // TODO: handle exceptions
            Log.d(TAG, "fileExists--ex:" + ex);
        }

        return false;
    }

}
