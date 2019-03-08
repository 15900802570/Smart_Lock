package com.smart.lock.ui;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;
import com.smart.lock.R;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.TempPwd;
import com.smart.lock.db.dao.TempPwdDao;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class TempPwdActivity extends Activity implements View.OnClickListener {

    private static String TAG = "TempPwdActivity";

    private DeviceInfo mDefaultDevice;
    private TempPwd mTempPwd;

    private TempPwdAdapter mTempPwdAdapter;
    private String mNodeId;
    private String mMac;
    private long mSecret;
    private static final List<String> mList = new ArrayList<>(
            Arrays.asList("01","02","03","04"));
    private static  final List<String> tList = new ArrayList<>(
            Arrays.asList("^3139313832353432353438303436393139323636353539323137313532353638" ,
                    "^3034343633373830373835383338333330373836303335303430343936313733" ,
                    "3132393439303838393533313433303237333034383230383930343730353130" ,
                    "3534353237373138393132313933373134393735313834363732373539383930")
    );
    private int mCurTime;

    private RecyclerView mTempPwdListViewRv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_pwd);
        initView();
        initData();
    }

    protected void initView() {
        mTempPwdListViewRv = findViewById(R.id.temp_pwd_list_view);
    }

    private void initData(){
        mDefaultDevice = (DeviceInfo)getIntent().getSerializableExtra(BleMsg.KEY_DEFAULT_DEVICE);
        mNodeId=mDefaultDevice.getDeviceNodeId();
        mMac = mDefaultDevice.getBleMac().replace(":","");

        mTempPwdAdapter = new TempPwdAdapter(this);
        mTempPwdListViewRv.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
        mTempPwdListViewRv.setItemAnimator(new DefaultItemAnimator());
        mTempPwdListViewRv.setAdapter(mTempPwdAdapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_temp_pwd_back:
                finish();
                break;
            case R.id.btn_create_temp_pwd:
                if(createTempPwd()){
                 saveTempPwd();
                }

                break;
            default:
                break;
        }
    }

    /**
     * 创建临时密码
     * @return bool 是否创建成功
     */
    private boolean createTempPwd(){
        byte[] lKey;
        mCurTime = (int)Math.ceil(System.currentTimeMillis()/1800000.0)*1800;
        byte[] mNodeIdBytes = StringUtil.hexStringToBytes(mNodeId);
        StringUtil.exchange(mNodeIdBytes);
        lKey = StringUtil.byteMerger(mNodeIdBytes,
                StringUtil.hexStringToBytes( mMac+
                        mList.get(new Random().nextInt(4))+  //随机序列
                        "0000000000000000000000000000000000"));     //17字节补码

        LogUtil.d(TAG,"mCurTime="+mCurTime+'\''+System.currentTimeMillis());
        LogUtil.d(TAG,"NodeId="+mNodeId+'\\' +
                "                        mMac="+mMac);
        LogUtil.d(TAG,"CurrentTimeHEX="+intToHex(mCurTime));
        LogUtil.d(TAG, "Key="+byteArrayToHexString(lKey));

        if(lKey.length == 32){
            mSecret= StringUtil.getCRC32(AES256Encode(
                    intToHex(mCurTime)+"000000000000000000000000",
                    StringUtil.hexStringToBytes(tList.get(new Random().nextInt(4)))));
            showPwdDialog(String.valueOf(mSecret));
            LogUtil.d(TAG,"mSecret="+mSecret);
            return true;
        }else {
            LogUtil.d(TAG,"mKey="+StringUtil.bytesToHexString(lKey)+"   "+lKey.length);
            return false;
        }

    }

    /**
     * 存储临时密码
     */
    private void saveTempPwd(){
        TempPwd lTempPwd= new TempPwd();
        lTempPwd.setDeviceNodeId(mNodeId);
        lTempPwd.setPwdCreateTime(System.currentTimeMillis()/1000);
        lTempPwd.setTempPwdUser(getResources().getString(R.string.temp_pwd_username));
        lTempPwd.setTempPwd(String.valueOf(mSecret));
        TempPwdDao.getInstance(this).insert(lTempPwd);
        mTempPwdAdapter.addItem(lTempPwd);
        mTempPwdAdapter.notifyDataSetChanged();
    }

    /**
     * 16进制的字符串表示转成字节数组
     *
     * @param hexString
     *            16进制格式的字符串
     * @return 转换后的字节数组
     **/
    public  byte[] toByteArray(String hexString) {
        if (hexString.isEmpty())
            LogUtil.d(TAG,getResources().getString(R.string.str_is_empty));
        hexString = hexString.toLowerCase();
        final byte[] byteArray = new byte[hexString.length() / 2];
        int k = 0;
        for (int i = 0; i < byteArray.length; i++) {//因为是16进制，最多只会占用4位，转换成字节需要两个16进制的字符，高位在先
            byte high = (byte) (Character.digit(hexString.charAt(k), 16) & 0xff);
            byte low = (byte) (Character.digit(hexString.charAt(k + 1), 16) & 0xff);
            byteArray[i] = (byte) (high << 4 | low);
            k += 2;
        }
        return byteArray;
    }

    /**
     * 字节数组转十六进制
     * @param data 输入字节串
     * @return String 16进制对应的字符串
     */
    private String byteArrayToHexString(byte[] data) {
        StringBuilder sBuilder = new StringBuilder();
        for(int i = 0;i < data.length;i++) {
            String str1 = Integer.toHexString(data[i]&0xFF);
                sBuilder.append(str1);
            }
        return sBuilder.toString();
    }

    /**
     *  int 类型转化为hex
     * @param n 整数
     * @return String
     */
    private String intToHex(int n) {
        StringBuffer s = new StringBuffer();
        String a="";
        char []b = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        while(n != 0){
            s = s.append(b[n%16]);
            n = n/16;
            if(s.length()%2 ==0){
                s.reverse();
                a += s.toString();
                s = new StringBuffer();
            }
        }
        return a;
    }

    /**
     * AES256加密
     * @param stringToEncode 输入加密信息
     * @param secretKey byte[] 加密Secret
     * @return
     */
    private byte[] AES256Encode(String stringToEncode, byte[] secretKey){
        try{
            SecretKeySpec keySpec = new SecretKeySpec(secretKey,"AES256");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,keySpec);
            byte[] result = cipher.doFinal(toByteArray(stringToEncode));
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
     * Dialog显示临时密码
     * @param string 临时密码
     */
    private void showPwdDialog(String string){
        DialogUtils.createTempPwdDialog(this,"*"+string);
    }


    public class TempPwdAdapter extends RecyclerView.Adapter<TempPwdAdapter.MyViewHolder> {

        private Context mContext;
        private ArrayList<TempPwd> mTempPwdList;

        private TempPwdAdapter(Context context) {
            mContext = context;
            mTempPwdList = TempPwdDao.getInstance(TempPwdActivity.this).queryAllByDevNodeId(mNodeId);
        }

        private void addItem(TempPwd tempPwd) {
            mTempPwdList.add(0,tempPwd);
        }
        private void deleteItem(int positionDelete){
            mTempPwdList.remove(positionDelete);
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_recycler_temp_pwd,
                    parent,
                    false);
            SwipeLayout swipeLayout = inflate.findViewById(R.id.item_ll_temp_pwd);
            swipeLayout.setClickToClose(true);
            swipeLayout.setRightSwipeEnabled(true);
            return new MyViewHolder(inflate);
        }

        @Override
        public void onBindViewHolder(@NonNull  MyViewHolder viewHolder, final int position) {
            final TempPwd tempPwdInfo = mTempPwdList.get(position);
            long failureTime;
            if (tempPwdInfo != null) {
                failureTime =DateTimeUtil.getFailureTime(tempPwdInfo.getPwdCreateTime());
                viewHolder.mTempPwdTv.setText(getResources().getString(R.string.temp_password));
                viewHolder.mTempPwdFailureTimeTv.setText(DateTimeUtil.timeStamp2Date(
                        String.valueOf(failureTime),
                        "yyyy-MM-dd HH:mm"));
                if(System.currentTimeMillis()/1000 - failureTime >= 0){
                    viewHolder.mTempPwdValidTv.setText(getResources().getString(R.string.temp_pwd_invalid));
                    viewHolder.mTempPwdValidTv.setTextColor(getResources().getColor(R.color.red));
                    viewHolder.mDelete.setVisibility(View.VISIBLE);
                    viewHolder.mShare.setVisibility(View.GONE);
                }else {
                    viewHolder.mTempPwdValidTv.setText(getResources().getString(R.string.temp_pwd_valid));
                    viewHolder.mTempPwdValidTv.setTextColor(getResources().getColor(R.color.light_black));
                    viewHolder.mDelete.setVisibility(View.GONE);
                    viewHolder.mShare.setVisibility(View.VISIBLE);
                }
                viewHolder.mTempPwdLl.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        showPwdDialog(String.valueOf(tempPwdInfo.getTempPwd()));
                        return true;
                    }
                });
                viewHolder.mDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TempPwdDao.getInstance(TempPwdActivity.this).delete(tempPwdInfo);
                        deleteItem(position);
                        mTempPwdAdapter.notifyDataSetChanged();
                    }
                });
                viewHolder.mShare.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mTempPwdAdapter.notifyDataSetChanged();
                        Toast.makeText(TempPwdActivity.this,"还没有实现",Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }

        @Override
        public int getItemCount() {
            return mTempPwdList.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {

            SwipeLayout mSwipeLayout;
            private TextView mTempPwdTv;
            private TextView mTempPwdFailureTimeTv;
            private TextView mTempPwdValidTv;
            private LinearLayout mTempPwdLl;
            private LinearLayout mDelete;
            private LinearLayout mShare;

            private MyViewHolder(View itemView) {
                super(itemView);
                mSwipeLayout = (SwipeLayout) itemView;
                mTempPwdValidTv = itemView.findViewById(R.id.tv_temp_pwd_valid);
                mTempPwdTv = itemView.findViewById(R.id.tv_temp_pwd);
                mTempPwdFailureTimeTv = itemView.findViewById(R.id.tv_temp_pwd_failure_time);
                mTempPwdLl = itemView.findViewById(R.id.ll_temp_pwd);
                mDelete = itemView.findViewById(R.id.ll_delete);
                mShare = itemView.findViewById(R.id.ll_share);
            }
        }
    }
}