package com.smart.lock.ui.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.smart.lock.R;
import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.listener.ClientTransaction;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.SystemUtils;
import com.smart.lock.widget.CustomDialog;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Objects;

public abstract class BaseFragment extends Fragment {
    public Activity mActivity;

    public abstract View initView();

    protected static String TAG = "BaseFragment";
    /**
     * 等待框
     */
    protected Dialog mLoadDialog;

    protected Handler mHandler;

    protected String mNodeId;
    protected DeviceInfo mDefaultDevice; //默认设备
    protected DeviceUser mDefaultUser;//当前用户
    protected DeviceUser mTempUser;
    protected ClientTransaction mCt;
    private Context mCtx;

    /**
     * 超时提示框启动器
     */
    protected Runnable mRunnable = new Runnable() {
        public void run() {
            if (mLoadDialog != null && mLoadDialog.isShowing()) {
                DialogUtils.closeDialog(mLoadDialog);
                Toast.makeText(mCtx, mCtx.getResources().getString(R.string.plz_reconnect), Toast.LENGTH_LONG).show();
            }

        }
    };

    /**
     * 蓝牙
     */
    protected BleManagerHelper mBleManagerHelper;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mActivity = getActivity();
        mHandler = new Handler();
        mCtx = this.getContext();
    }

    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return initView();
    }

    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initDate();
    }

    public void initDate() {
    }

    /**
     * 新界面
     *
     * @param cls    新Activity
     * @param bundle 数据包
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected void startIntent(Class<?> cls, Bundle bundle) {
        Intent intent = new Intent();
        if (bundle != null) {
            intent.putExtras(bundle);
        }

        intent.setClass(Objects.requireNonNull(getView()).getContext(), cls);
        startActivity(intent);
    }

    /**
     * 创建用户
     *
     * @param userId 用户ID
     */
    protected synchronized DeviceUser createDeviceUser(short userId, String path, int permission) {
        DeviceUser user = new DeviceUser();
        user.setDevNodeId(mNodeId);
        user.setCreateTime(System.currentTimeMillis() / 1000);
        user.setUserId(userId);
        user.setUserPermission(permission);
        user.setUserStatus(ConstantUtil.USER_UNENABLE);
        if (permission == ConstantUtil.DEVICE_MASTER) {
            user.setUserName(getString(R.string.administrator) + userId);
        } else if (permission == ConstantUtil.DEVICE_MEMBER) {
            user.setUserName(getString(R.string.members) + userId);
        } else {
            user.setUserName(getString(R.string.tmp_user) + userId);
        }

        user.setQrPath(path);
        Log.d(TAG, "user = " + user.toString());
        DeviceUserDao.getInstance(mActivity).insert(user);
        return user;
    }


    /**
     * 创建二维码
     *
     * @param authBuf 授权码
     */
    protected String createQRcodeImage(byte[] authBuf) {

        int w = (int) mActivity.getResources().getDimension(R.dimen.qr_width);
        int h = (int) mActivity.getResources().getDimension(R.dimen.qr_height);
        try {
            if (authBuf == null || authBuf.length < 1) {
                return null;
            }
            Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            //图像数据转换，使用了矩阵转换
            Log.d(TAG, "authBuf = " + Arrays.toString(authBuf));
            String content = StringUtil.bytesToHexString(authBuf);
            Log.d(TAG, "content = " + content);
            BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, w, h, hints);
            int[] pixels = new int[w * h];
            //下面这里按照二维码的算法，逐个生成二维码的图片，
            //两个for循环是图片横列扫描的结果
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * w + x] = 0xff000000;
                    } else {
                        pixels[y * w + x] = 0xffffffff;
                    }
                }
            }
            //生成二维码图片的格式，使用ARGB_8888
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h);

            String fileName = String.valueOf(System.currentTimeMillis());
            Uri path = SystemUtils.saveImageToGallery(mActivity, bitmap, fileName);

            //显示到我们的ImageView上面
            CustomDialog dialog = DialogUtils.showQRDialog(mActivity, bitmap);
            ImageView qrIv = dialog.getCustomView().findViewById(R.id.iv_qr);
            qrIv.setImageBitmap(bitmap);

            return path.getPath();

        } catch (WriterException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 显示图片
     *
     * @param imagePath 图片路径
     */
    protected boolean displayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            CustomDialog dialog = DialogUtils.showQRDialog(mActivity, bitmap);
            ImageView qrIv = dialog.getCustomView().findViewById(R.id.iv_qr);

            qrIv.setImageBitmap(bitmap);
            return true;
        } else {
            return false;
        }
    }

    protected String createQr(DeviceUser user) {
        byte[] nodeId = StringUtil.hexStringToBytes(user.getDevNodeId());
        byte[] userId = StringUtil.hexStringToBytes(StringUtil.stringToAsciiString(String.valueOf(user.getUserId()), 4));
        DeviceInfo info = DeviceInfoDao.getInstance(mActivity).queryFirstData("device_nodeId", user.getDevNodeId());
        byte[] bleMac = StringUtil.hexStringToBytes(info.getBleMac());
        byte[] randCode = StringUtil.hexStringToBytes(info.getDeviceSecret());

        LogUtil.d(TAG, "randCode  : " + randCode.length);

        byte[] buf = new byte[64];
        byte[] authBuf = new byte[64];
        authBuf[0] = 0x01;
        System.arraycopy(userId, 0, authBuf, 1, 2);
        System.arraycopy(nodeId, 0, authBuf, 3, 8);
        System.arraycopy(bleMac, 0, authBuf, 11, 6);
        System.arraycopy(randCode, 0, authBuf, 17, 10);

        byte[] timeBuf = new byte[4];
        StringUtil.int2Bytes((int) (System.currentTimeMillis() / 1000 + 30 * 60), timeBuf);
        System.arraycopy(timeBuf, 0, authBuf, 27, 4);

        Arrays.fill(authBuf, 39, 64, (byte) 0x25);

        try {
            AES_ECB_PKCS7.AES256Encode(authBuf, buf, MessageCreator.mQrSecret);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String path = createQRcodeImage(buf);
        Log.d(TAG, "path = " + path);
        if (path != null) {
            user.setQrPath(path);
            DeviceUserDao.getInstance(mActivity).updateDeviceUser(user);
        }

        return path;
    }

    /**
     * 超时提醒
     *
     * @param seconds 秒
     */
    protected void closeDialog(final int seconds) {

        mHandler.removeCallbacks(mRunnable);

        mHandler.postDelayed(mRunnable, seconds * 1000);
    }

    /**
     * 吐司提示
     *
     * @param msg 提示信息
     */
    protected void showMessage(String msg) {
        Toast.makeText(mCtx, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}