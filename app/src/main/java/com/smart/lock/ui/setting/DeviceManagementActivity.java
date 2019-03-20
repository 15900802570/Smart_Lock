package com.smart.lock.ui.setting;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;
import com.smart.lock.R;
import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.ToastUtil;
import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.bean.ZxingConfig;
import com.yzq.zxinglibrary.common.Constant;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class DeviceManagementActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SCAN = 1;

    private static String TAG = "DeviceManagementActivity";

    private RecyclerView mDevManagementRv;
    private DevManagementAdapter mDevManagementAdapter;

    private String mUserType;
    private String mUserId;
    private String mNodeId;
    private String mBleMac;
    private String mRandCode;
    private String mTime;
    private DeviceInfo mNewDevice;

    private Dialog mAddNewDevDialog;

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {
            if (data != null) {
                String content = data.getStringExtra(Constant.CODED_CONTENT);
                LogUtil.d(TAG, "content = " + content);
                byte[] mByte;
                if (content.length() == 127) {
                    mByte = StringUtil.hexStringToBytes('0' + content);
                } else if (content.length() == 128) {
                    mByte = StringUtil.hexStringToBytes(content);
                } else {
                    Dialog alterDialog = DialogUtils.createAlertDialog(this, content);
                    alterDialog.show();
                    return;
                }
                LogUtil.d(TAG, "mByte=" + Arrays.toString(mByte));
                byte[] devInfo = new byte[64];
                AES_ECB_PKCS7.AES256Decode(mByte, devInfo, MessageCreator.mQrSecret);
                LogUtil.d(TAG, Arrays.toString(devInfo));
                getDevInfo(devInfo);
                addDev();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d(TAG,"Device");
        setContentView(R.layout.activity_device_manager);
        initView();
        initData();
        initEvent();
    }

    private void initView() {
        mDevManagementRv = findViewById(R.id.dev_management_list_view);
    }

    private void initData() {
        mDevManagementAdapter = new DevManagementAdapter(this);
        mDevManagementRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mDevManagementRv.setItemAnimator(new DefaultItemAnimator());
        mDevManagementRv.setAdapter(mDevManagementAdapter);
    }

    private void initEvent() {

    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_dev_management_back:
                finish();
                break;
            case R.id.btn_dev_management_add_new_lock:
                mAddNewDevDialog = DialogUtils.createTipsDialog(this, getString(R.string.disconnect_ble_first));
                mAddNewDevDialog.show();
                break;
            default:
                break;
        }
    }

    public void tipsOnClick(View view) {
        switch (view.getId()) {
            case R.id.dialog_cancel_btn:
                mAddNewDevDialog.cancel();
                break;
            case R.id.dialog_confirm_btn:
                mAddNewDevDialog.cancel();
                scanQr();
                break;
        }
    }


    private void getDevInfo(byte[] devInfo) {
        byte[] typeBytes = new byte[1];
        byte[] copyNumBytes = new byte[2];
        byte[] ImeiBytes = new byte[8];
        byte[] bleMACBytes = new byte[6];
        byte[] timeBytes = new byte[4];
        byte[] randCodeBytes = new byte[18];
        System.arraycopy(devInfo, 0, typeBytes, 0, 1);
        System.arraycopy(devInfo, 1, copyNumBytes, 0, 2);
        System.arraycopy(devInfo, 3, ImeiBytes, 0, 8);
        System.arraycopy(devInfo, 11, bleMACBytes, 0, 6);
        System.arraycopy(devInfo, 17, randCodeBytes, 0, 18);
        System.arraycopy(devInfo, 35, timeBytes, 0, 4);
        mUserType = StringUtil.bytesToHexString(typeBytes);
        mUserId = StringUtil.bytesToHexString(copyNumBytes);
        StringUtil.exchange(ImeiBytes);
        mNodeId = StringUtil.bytesToHexString(ImeiBytes);
        mBleMac = StringUtil.bytesToHexString(bleMACBytes).toUpperCase();
        mRandCode = StringUtil.bytesToHexString(randCodeBytes);
        mTime = StringUtil.byte2Int(timeBytes);

        LogUtil.d(TAG, "New Device : " + '\n' +
                "userType = " + mUserType + '\n' +
                "UserId = " + mUserId + '\n' +
                "NodeId = " + mNodeId + '\n' +
                "Time = " + mTime);
    }

    private void addDev() {
        if ((Long.valueOf(mTime)) < System.currentTimeMillis() / 1000) {
            Dialog alterDialog = DialogUtils.createAlertDialog(this, "授权码已过期，请重新请求");
            alterDialog.show();
        } else if (DeviceInfoDao.getInstance(this).queryByField(DeviceInfoDao.NODE_ID, mNodeId) != null) {
            ToastUtil.show(this, getString(R.string.device_has_been_added), Toast.LENGTH_LONG);
        } else {
            if (getIntent().getExtras() != null) {
                DeviceInfo deviceDev = (DeviceInfo) getIntent().getExtras().getSerializable(BleMsg.KEY_DEFAULT_DEVICE);
                BleManagerHelper.getInstance(this, deviceDev.getBleMac(), false).stopService();
            }
            LocalBroadcastManager.getInstance(this).registerReceiver(devCheckReceiver, intentFilter());
            BleManagerHelper.setSk(mBleMac, mNodeId, mRandCode);
            BleManagerHelper.getInstance(this, getMacAdr(mBleMac), false).connectBle((byte) 1, Short.parseShort(mUserId, 16));
        }
    }

    /**
     * 创建用户
     *
     * @param userId
     */
    private void createDeviceUser(short userId) {
        DeviceUser user = new DeviceUser();
        int userIdInt = Integer.valueOf(userId);
        user.setDevNodeId(mNodeId);
        user.setCreateTime(System.currentTimeMillis() / 1000);
        user.setUserId(userId);
        user.setUserPermission(ConstantUtil.DEVICE_MASTER);
        if (userIdInt < 101) {
            user.setUserPermission(ConstantUtil.DEVICE_MASTER);
            user.setUserName(getString(R.string.administrator) + userId);
        } else if (userIdInt < 201) {
            user.setUserPermission(ConstantUtil.DEVICE_MEMBER);
            user.setUserName(getString(R.string.members) + userId);
        } else {
            user.setUserPermission(ConstantUtil.DEVICE_MEMBER);
            user.setUserName(getString(R.string.members) + userId);
        }

        user.setUserStatus(ConstantUtil.USER_UNENABLE);

        DeviceUserDao.getInstance(this).insert(user);
    }

    /**
     * 创建设备
     */
    private void createDevice() {
        DeviceInfo oldDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_nodeId", mNodeId);
        if (oldDevice == null) {
            DeviceInfo defaultDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
            LogUtil.d(TAG, "newDevice: " + "BLEMAC =" + mBleMac);
            mNewDevice = new DeviceInfo();
            mNewDevice.setActivitedTime(Long.valueOf(mTime));
            mNewDevice.setBleMac(getMacAdr(mBleMac));
            mNewDevice.setConnectType(false);
            mNewDevice.setUserId(Short.parseShort(mUserId, 16));
            mNewDevice.setDeviceNodeId(mNodeId);
            mNewDevice.setNodeType(ConstantUtil.SMART_LOCK);
            mNewDevice.setDeviceDate(System.currentTimeMillis() / 1000);
            if (defaultDevice != null) mNewDevice.setDeviceDefault(false);
            else mNewDevice.setDeviceDefault(true);
            mNewDevice.setDeviceSn("");
            mNewDevice.setDeviceName(ConstantUtil.LOCK_DEFAULT_NAME);
            mNewDevice.setDeviceSecret(mRandCode);
            DeviceInfoDao.getInstance(this).insert(mNewDevice);
        }
    }

    protected static IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.STR_RSP_SECURE_CONNECTION);
        intentFilter.addAction(BleMsg.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleMsg.STR_RSP_SET_TIMEOUT);
        return intentFilter;
    }

    /**
     * 广播接受
     */
    private final BroadcastReceiver devCheckReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //4.2.3 MSG 04
            if (action.equals(BleMsg.STR_RSP_SECURE_CONNECTION)) {
                LogUtil.d(TAG, "array = " + Arrays.toString(intent.getByteArrayExtra(BleMsg.KEY_STATUS)));
                createDeviceUser(Short.parseShort(mUserId, 16));
                createDevice();
                mDevManagementAdapter.addItem(mNewDevice);
                mDevManagementAdapter.notifyDataSetChanged();
            }
        }
    };

    /**
     * 转成标准MAC地址
     *
     * @param str 未加：的MAC字符串
     * @return 标准MAC字符串
     */
    protected static String getMacAdr(String str) {
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
     * 新界面
     *
     * @param cls    新Activity
     * @param bundle 数据包
     */
    protected void startIntent(Class<?> cls, Bundle bundle) {
        Intent intent = new Intent();
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        intent.setClass(this, cls);
        startActivity(intent);
    }

    /**
     * AES256解密
     *
     * @param bytesToDecode 输入加密信息
     * @param secretKey     byte[] 加密Secret
     * @return
     */
    private byte[] AES256Decode(byte[] bytesToDecode, byte[] secretKey) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey, "AES256");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] result = cipher.doFinal(bytesToDecode);
            return result;
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 扫描二维码
     */
    private void scanQr() {
        Intent newIntent = new Intent(this, CaptureActivity.class);
        ZxingConfig config = new ZxingConfig();
        config.setPlayBeep(true);//是否播放扫描声音 默认为true
        config.setShake(true);//是否震动  默认为true
        config.setDecodeBarCode(false);//是否扫描条形码 默认为true
//        config.setReactColor(R.color.colorAccent);//设置扫描框四个角的颜色 默认为淡蓝色
//        config.setFrameLineColor(R.color.colorAccent);//设置扫描框边框颜色 默认无色
        config.setFullScreenScan(false);//是否全屏扫描  默认为true  设为false则只会在扫描框中扫描
        newIntent.putExtra(Constant.INTENT_ZXING_CONFIG, config);
        startActivityForResult(newIntent, REQUEST_CODE_SCAN);
    }

    private class DevManagementAdapter extends RecyclerView.Adapter<DevManagementAdapter.MyViewHolder> {

        private Context mContext;
        private ArrayList<DeviceInfo> mDevList;
        private DeviceInfo mDefaultInfo;
        int mDefaultPosition;

        private DevManagementAdapter(Context context) {
            mContext = context;
            mDevList = DeviceInfoDao.getInstance(DeviceManagementActivity.this).queryAll();
        }

        private void addItem(DeviceInfo deviceInfo) {
            mDevList.add(0, deviceInfo);
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_recycler_dev_management, viewGroup, false);
            SwipeLayout swipeLayout = inflate.findViewById(R.id.item_ll_dev_management);
            swipeLayout.setClickToClose(true);
            swipeLayout.setRightSwipeEnabled(true);
            return new MyViewHolder(inflate);
        }

        @Override
        public void onBindViewHolder(MyViewHolder myViewHolder, final int position) {
            final DeviceInfo deviceInfo = mDevList.get(position);
            if (deviceInfo != null) {
                try {
                    myViewHolder.mLockName.setText(deviceInfo.getDeviceName());
                    myViewHolder.mLockUnm.setText(String.valueOf(deviceInfo.getBleMac()));
                } catch (NullPointerException e) {
                    LogUtil.d(TAG, deviceInfo.getDeviceName() + "  " + deviceInfo.getDeviceIndex());
                }
                if (deviceInfo.getDeviceDefault()) {
                    myViewHolder.mDefaultFlag.setImageResource(R.drawable.ic_dev_management_square_full);
                    mDefaultInfo = deviceInfo;
                    mDefaultPosition = position;
                } else {
                    myViewHolder.mDefaultFlag.setImageResource(R.drawable.ic_dev_management_square_null);
                }

                myViewHolder.mSetDefault.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDefaultInfo.setDeviceDefault(false);
                        DeviceInfoDao.getInstance(DeviceManagementActivity.this).updateDeviceInfo(mDefaultInfo);
                        deviceInfo.setDeviceDefault(true);
                        DeviceInfoDao.getInstance(DeviceManagementActivity.this).updateDeviceInfo(deviceInfo);
                        mDevList = DeviceInfoDao.getInstance(DeviceManagementActivity.this).queryAll();
                        mDevManagementAdapter.notifyDataSetChanged();
                        LogUtil.d(TAG, "设置为默认设备");
                    }
                });
                myViewHolder.mUnbind.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (deviceInfo.getDeviceDefault()) {
                            BleManagerHelper.getInstance(DeviceManagementActivity.this, deviceInfo.getBleMac(), false).stopService();
                        }
                        DeviceInfoDao.getInstance(DeviceManagementActivity.this).delete(deviceInfo);
                        mDevList.remove(position);
                        mDevManagementAdapter.notifyDataSetChanged();
                    }
                });

            }
        }

        @Override
        public int getItemCount() {
            return mDevList.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            SwipeLayout mSwipeLayout;
            private TextView mLockName;
            private TextView mLockUnm;
            private ImageView mDefaultFlag;

            private LinearLayout mSetDefault;
            private LinearLayout mUnbind;

            private MyViewHolder(View itemView) {
                super(itemView);
                mSwipeLayout = (SwipeLayout) itemView;
                mLockName = itemView.findViewById(R.id.tv_dev_management_dev_name);
                mLockUnm = itemView.findViewById(R.id.tv_dev_management_dev_num);
                mDefaultFlag = itemView.findViewById(R.id.iv_dev_management_default_flag);

                mSetDefault = itemView.findViewById(R.id.ll_set_default);
                mUnbind = itemView.findViewById(R.id.ll_unbind);
            }
        }
    }

}
