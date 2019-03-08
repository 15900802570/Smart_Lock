package com.smart.lock.ui.setting;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
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

import com.daimajia.swipe.SwipeLayout;
import com.smart.lock.R;
import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.ui.LockDetectingActivity;
import com.smart.lock.ui.LockSettingActivity;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
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

public class DeviceManagementActivity extends AppCompatActivity{

    private static final int REQUEST_CODE_SCAN = 1;

    private static String TAG = "DeviceManagementActivity";

    private RecyclerView mDevManagementRv;
    private DevManagementAdapter mDevManagementAdapter;

    private String mUserType;
    private String mUserNum;
    private String mDevImei;
    private String mBleMac;
    private String mTime;

    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode ==REQUEST_CODE_SCAN && resultCode == RESULT_OK){
            LogUtil.e(TAG,"这是一个寂寞的天");
            if(data != null){
                String content = data.getStringExtra(Constant.CODED_CONTENT);
                byte[] mByte;
                if(content.length()==63){
                    mByte=StringUtil.hexStringToBytes('0'+content);
                }else if(content.length()==64){
                    mByte=StringUtil.hexStringToBytes(content);
                }else {
                    DialogUtils.createAlertDialog(this,content);
                    return;
                }
                    LogUtil.e(TAG,"mByte="+Arrays.toString(mByte));
                byte[] devInfo = new byte[32];
                AES_ECB_PKCS7.AES256Decode(mByte,devInfo,MessageCreator.mQrSecret);
                    LogUtil.e(TAG,Arrays.toString(devInfo));
                byte[] temp2={25, 45, 92, -113, -63, 39, 81, 86, -36, 80, 102, 7, 33, -52, -116, 35, 89, 31, 121, 85, 8, -28, 50, -91, 17, -50, 57, -89, 45, -98, 86, -125};
                    LogUtil.e(TAG,"temp2="+StringUtil.bytesToHexString(temp2));
                getDevInfo(devInfo);
                addDev();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_manager);
        initView();
        initData();
        initEvent();
    }

    private void initView(){
        mDevManagementRv = findViewById(R.id.dev_management_list_view);
    }
    private void initData(){
            mDevManagementAdapter = new DevManagementAdapter(this);
            mDevManagementRv.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
            mDevManagementRv.setItemAnimator(new DefaultItemAnimator());
            mDevManagementRv.setAdapter(mDevManagementAdapter);
    }
    private void initEvent(){

    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.iv_dev_management_back:
                finish();
                break;
            case R.id.btn_dev_management_add_new_lock:
                scanQr();
                break;
            default:
                break;
        }
    }


    private void getDevInfo(byte[] devInfo){
        byte[] typeBytes= new byte[1];
        byte[] copyNumBytes= new byte[2];
        byte[] ImeiBytes= new byte[8];
        byte[] bleMACBytes= new byte[6];
        byte[] timeBytes= new byte[4];
        System.arraycopy(devInfo,0,typeBytes,0,1);
        System.arraycopy(devInfo,1,copyNumBytes,0,2);
        System.arraycopy(devInfo,3,ImeiBytes,0,8);
        System.arraycopy(devInfo,11,bleMACBytes,0,6);
        System.arraycopy(devInfo,17,timeBytes,0,4);
        mUserType = StringUtil.bytesToHexString(typeBytes);
        mUserNum = StringUtil.bytesToHexString(copyNumBytes);
        StringUtil.exchange(ImeiBytes);
        mDevImei = StringUtil.bytesToHexString(ImeiBytes);
        mBleMac = StringUtil.bytesToHexString(bleMACBytes);
        mTime = StringUtil.byte2Int(timeBytes);
//        LogUtil.e(TAG,"类型："+Arrays.toString(typeBytes)
//                +"\n"+mUserType);
//        LogUtil.e(TAG,"授权码："+Arrays.toString(copyNumBytes)
//                +"\n"+mUserNum);
//        LogUtil.e(TAG,"IMEI"+Arrays.toString(ImeiBytes)
//                +"\n"+mDevImei);
//        LogUtil.e(TAG,"MAC："+Arrays.toString(bleMACBytes)
//                +"\n"+mBleMac);
//        LogUtil.e(TAG,"Time："+Arrays.toString(timeBytes)
//                +"\n"+time);
    }
    private void addDev(){
//        if((Long.valueOf(mTime))<System.currentTimeMillis()/1000){
//            DialogUtils.createAlertDialog(this,"授权码已过期，请重新请求");
//            return;
//        }else {
            Bundle bundle = new Bundle();
            bundle.putString(BleMsg.KEY_BLE_MAC, mBleMac);
            bundle.putString(BleMsg.KEY_NODE_SN, "123");
            bundle.putString(BleMsg.KEY_NODE_ID, mDevImei);
            bundle.putInt(BleMsg.KEY_USER_TYPE,Integer.valueOf(mUserType));
            startIntent(LockDetectingActivity.class, bundle);
//        }

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
     * @param bytesToDecode 输入加密信息
     * @param secretKey byte[] 加密Secret
     * @return
     */
    private byte[] AES256Decode(byte[] bytesToDecode, byte[] secretKey){
        try{
            SecretKeySpec keySpec = new SecretKeySpec(secretKey,"AES256");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,keySpec);
            byte[] result = cipher.doFinal(bytesToDecode);
            return result;
        }catch (NoSuchPaddingException e){
            e.printStackTrace();
        }catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }catch (InvalidKeyException e){
            e.printStackTrace();
        }catch (IllegalBlockSizeException e){
            e.printStackTrace();
        }catch (BadPaddingException e){
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

    private class DevManagementAdapter extends RecyclerView.Adapter<DevManagementAdapter.MyViewHolder>{

        private Context mContext;
        private ArrayList<DeviceInfo> mDevList;
        private DeviceInfo mDefaultInfo;
        int mDefaultPosition;
        private DevManagementAdapter(Context context){
            mContext = context;
            mDevList = DeviceInfoDao.getInstance(DeviceManagementActivity.this).queryAll();
        }
        private void addItem(DeviceInfo deviceInfo){
            mDevList.add(0,deviceInfo);
        }
        private void unBind(int positionUnbind){
            mDevList.remove(positionUnbind);
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType){
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_recycler_dev_management,viewGroup,false);
            SwipeLayout swipeLayout = inflate.findViewById(R.id.item_ll_dev_management);
            swipeLayout.setClickToClose(true);
            swipeLayout.setRightSwipeEnabled(true);
            return new MyViewHolder(inflate);
        }

        @Override
        public void onBindViewHolder(MyViewHolder myViewHolder, final int position){
            final DeviceInfo deviceInfo = mDevList.get(position);
            if(deviceInfo != null){
                try {
                    myViewHolder.mLockName.setText(deviceInfo.getDeviceName());
                    myViewHolder.mLockUnm.setText(String.valueOf(deviceInfo.getDeviceIndex()));
                }catch (NullPointerException e){
                    LogUtil.d(TAG,deviceInfo.getDeviceName()+"  "+deviceInfo.getDeviceIndex());
                }
                if(deviceInfo.getDeviceDefault()){
                    myViewHolder.mDefaultFlag.setImageResource(R.drawable.ic_dev_management_square_full);
                    mDefaultInfo = deviceInfo;
                    mDefaultPosition = position;
                }else {
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
                        LogUtil.d(TAG,"设置为默认设备");
                    }
                });
                myViewHolder.mUnbind.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DeviceInfoDao.getInstance(DeviceManagementActivity.this).delete(deviceInfo);
                        mDevList.remove(position);
                        mDevManagementAdapter.notifyDataSetChanged();
                    }
                });

            }
        }

        @Override
        public int getItemCount(){
            return mDevList.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder{
            SwipeLayout mSwipeLayout;
            private TextView mLockName;
            private TextView mLockUnm;
            private ImageView mDefaultFlag;

            private LinearLayout mSetDefault;
            private LinearLayout mUnbind;

            private MyViewHolder(View itemView){
                super(itemView);
                mSwipeLayout = (SwipeLayout)itemView;
                mLockName = itemView.findViewById(R.id.tv_dev_management_dev_name);
                mLockUnm = itemView.findViewById(R.id.tv_dev_management_dev_num);
                mDefaultFlag = itemView.findViewById(R.id.iv_dev_management_default_flag);

                mSetDefault = itemView.findViewById(R.id.ll_set_default);
                mUnbind = itemView.findViewById(R.id.ll_unbind);
            }
        }
    }

}
