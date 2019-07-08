package com.smart.lock.ui.fragment;

import android.Manifest;
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
import android.support.v7.app.AppCompatActivity;
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
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.ClientTransaction;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
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
    public AppCompatActivity mActivity;

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
    private Context mCtx;
    protected final int REQUESTCODE = 0;
    protected final int REQUESTCODE_CROP_PHOTO = 1;
    protected String[] mExternalPermission = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    /**
     * 蓝牙
     */
    protected BleManagerHelper mBleManagerHelper;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mActivity = (AppCompatActivity) getActivity();
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
    protected synchronized DeviceUser createDeviceUser(short userId, String path, String authCode) {
        DeviceUser user = new DeviceUser();
        user.setDevNodeId(mNodeId);
        user.setCreateTime(System.currentTimeMillis() / 1000);
        user.setUserId(userId);
        user.setUserStatus(ConstantUtil.USER_UNENABLE);
        user.setAuthCode(authCode);
        if (userId < 101) {
            user.setUserPermission(ConstantUtil.DEVICE_MASTER);
            user.setUserName(mCtx.getString(R.string.administrator) + userId);
        } else if (userId < 201) {
            user.setUserPermission(ConstantUtil.DEVICE_MEMBER);
            user.setUserName(mCtx.getString(R.string.members) + userId);
        } else {
            user.setUserPermission(ConstantUtil.DEVICE_TEMP);
            user.setUserName(mCtx.getString(R.string.tmp_user) + userId);
        }

        user.setQrPath(path);
        DeviceUserDao.getInstance(mActivity).insert(user);
        return user;
    }


    /**
     * 创建二维码
     *
     * @param authBuf 授权码
     */
    protected String createQRcodeImage(byte[] authBuf, byte type) {

        int w = (int) mActivity.getResources().getDimension(R.dimen.qr_width);
        int h = (int) mActivity.getResources().getDimension(R.dimen.qr_height);
        try {
            if (authBuf == null || authBuf.length < 1) {
                return null;
            }
            Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            //图像数据转换，使用了矩阵转换
            String content = StringUtil.bytesToHexString(authBuf);
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
            if (type != ConstantUtil.DEVICE_TEMP) {
                dialog.show();
            }

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
            dialog.show();
            qrIv.setImageBitmap(bitmap);
            return true;
        } else {
            return false;
        }
    }

    protected String createQr(DeviceUser user) {
        byte[] buf = new byte[64];
        byte[] authBuf = new byte[64];
        authBuf[0] = user.getUserPermission();
        byte[] authCode = new byte[30];
        if (StringUtil.checkNotNull(user.getAuthCode())) {
            authCode = StringUtil.hexStringToBytes(user.getAuthCode());
        }

        byte[] timeQr = new byte[4];
        StringUtil.int2Bytes((int) (System.currentTimeMillis() / 1000 + 30 * 60), timeQr);
        System.arraycopy(timeQr, 0, authBuf, 1, 4); //二维码有效时间

        System.arraycopy(authCode, 0, authBuf, 5, 30); //鉴权码

        Arrays.fill(authBuf, 35, 64, (byte) 0x1d); //补充字节

        try {
            AES_ECB_PKCS7.AES256Encode(authBuf, buf, MessageCreator.mQrSecret);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String path = createQRcodeImage(buf, user.getUserPermission());
        if (path != null) {
            user.setQrPath(path);
            DeviceUserDao.getInstance(mActivity).updateDeviceUser(user);
        }

        return path;
    }

    protected void setAuthCode(byte[] authTime, DeviceInfo info, DeviceUser user) {
        byte[] authCode = new byte[30];
        if (StringUtil.checkNotNull(info.getDeviceNodeId())) {
            byte[] userId = new byte[2];
            StringUtil.short2Bytes(user.getUserId(), userId);
            System.arraycopy(userId, 0, authCode, 0, 2);

            byte[] nodeIdBuf = new byte[8];
            String nodeId = info.getDeviceNodeId();
            if (nodeId.getBytes().length == 15) {
                nodeId = "0" + nodeId;
            }
            nodeIdBuf = StringUtil.hexStringToBytes(nodeId);
            StringUtil.exchange(nodeIdBuf);
            System.arraycopy(nodeIdBuf, 0, authCode, 2, 8);

            byte[] bleMacBuf = new byte[6];
            String bleMac = info.getBleMac();
            if (StringUtil.checkNotNull(bleMac)) {
                bleMacBuf = StringUtil.hexStringToBytes(bleMac.replace(":", ""));
                System.arraycopy(bleMacBuf, 0, authCode, 10, 6);
            }

            byte[] randCodeBuf = new byte[10];
            String randCode = info.getDeviceSecret();
            if (StringUtil.checkNotNull(randCode)) {
                randCodeBuf = StringUtil.hexStringToBytes(randCode);
                System.arraycopy(randCodeBuf, 0, authCode, 16, 10);
            }

            System.arraycopy(authTime, 0, authCode, 26, 4);

            user.setAuthCode(StringUtil.bytesToHexString(authCode));
            DeviceUserDao.getInstance(mActivity).updateDeviceUser(user);
        }
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