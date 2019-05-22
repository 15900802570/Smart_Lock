package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import com.smart.lock.utils.SystemUtils;
import com.smart.lock.utils.ToastUtil;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import static com.smart.lock.ble.message.MessageCreator.mIs128Code;
import static com.smart.lock.ble.message.MessageCreator.mIsOnceForTempPwd;
import static com.smart.lock.utils.ConstantUtil.NUMBER_100;

public class TempPwdActivity extends Activity implements View.OnClickListener {

    private static String TAG = "TempPwdActivity";

    private TempPwdAdapter mTempPwdAdapter;
    private String mNodeId;
    private String mMac;
    private String mSecret;
    private List<String> mSecretList = new ArrayList<>();

    private RecyclerView mTempPwdListViewRv;
    private Set<Integer> mExistNum = new HashSet<>();
    private int mRandomNum = 101; // 零时密码随机数，有效值0-99,101为无效值

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

    private void initData() {
        DeviceInfo mDefaultDevice = (DeviceInfo) getIntent().getSerializableExtra(BleMsg.KEY_DEFAULT_DEVICE);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mMac = mDefaultDevice.getBleMac().replace(getString(R.string.colon), "");
        String tempSecret = mDefaultDevice.getTempSecret();
        if (StringUtil.checkNotNull(tempSecret)) {
            if (mIs128Code) {
                mSecretList.add(tempSecret.substring(0, 32));
                mSecretList.add(tempSecret.substring(32, 64));
                mSecretList.add(tempSecret.substring(64, 96));
                mSecretList.add(tempSecret.substring(96, 128));
            } else {
                mSecretList.add(tempSecret.substring(0, 64));
                mSecretList.add(tempSecret.substring(64, 128));
                mSecretList.add(tempSecret.substring(128, 192));
                mSecretList.add(tempSecret.substring(192, 256));
            }
            LogUtil.d(TAG, "mSecretList = " + mSecretList.toString());
        } else {
            Toast.makeText(this, "设备时间未校准！", Toast.LENGTH_LONG).show();
        }

        mTempPwdAdapter = new TempPwdAdapter(this);
        mTempPwdListViewRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
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
                if (createTempPwd()) {
                    saveTempPwd();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 创建临时密码
     *
     * @return bool 是否创建成功
     */
    private boolean createTempPwd() {
        if (mExistNum.size() >= 30) {
            ToastUtil.showShort(this, R.string.not_regenerate_temp_pwd);
            return false;
        } else {
            int mCurTime = (int) Math.ceil(System.currentTimeMillis() / 1800000.0) * 1800;

            LogUtil.d(TAG, "mCurTime=" + mCurTime + '\'' + System.currentTimeMillis());
            LogUtil.d(TAG, "NodeId=" + mNodeId + '\\' +
                    "                        mMac=" + mMac);
            LogUtil.d(TAG, "CurrentTimeHEX=" + intToHex(mCurTime));
            Long tempSecret;
            if (mIs128Code) {
                tempSecret = StringUtil.getCRC32(AES128Encode(intToHex(mCurTime) + "000000000000000000000000",
                        StringUtil.hexStringToBytes(mIsOnceForTempPwd ? getRandomSecret() : mSecretList.get(new Random().nextInt(4)))));
            } else {
                tempSecret = StringUtil.getCRC32(AES256Encode(intToHex(mCurTime) + "000000000000000000000000",
                        StringUtil.hexStringToBytes(mIsOnceForTempPwd ? getRandomSecret() : mSecretList.get(new Random().nextInt(4)))));
            }
            if (mIsOnceForTempPwd) {
                if (mRandomNum < 10) {
                    mSecret = "0" + mRandomNum + String.valueOf(tempSecret);
                } else {
                    mSecret = mRandomNum + String.valueOf(tempSecret);
                }
            } else {
                mSecret = String.valueOf(tempSecret);
            }
            showPwdDialog(String.valueOf(mSecret));
            LogUtil.d(TAG, "mSecret=" + mSecret);
            return true;
        }
    }

    /**
     * 存储临时密码
     */
    private void saveTempPwd() {
        mExistNum.add(mRandomNum);
        TempPwd lTempPwd = new TempPwd();
        lTempPwd.setDeviceNodeId(mNodeId);
        lTempPwd.setRandomNum(mRandomNum);
        lTempPwd.setPwdCreateTime(System.currentTimeMillis() / 1000);
        lTempPwd.setTempPwdUser(getResources().getString(R.string.temp_pwd_username));
        lTempPwd.setTempPwd(String.valueOf(mSecret));
        TempPwdDao.getInstance(this).insert(lTempPwd);
        mTempPwdAdapter.addItem(lTempPwd);
        mTempPwdAdapter.notifyDataSetChanged();
    }

    /**
     * 16进制的字符串表示转成字节数组
     *
     * @param hexString 16进制格式的字符串
     * @return 转换后的字节数组
     **/
    public byte[] toByteArray(String hexString) {
        if (hexString.isEmpty())
            LogUtil.d(TAG, getResources().getString(R.string.str_is_empty));
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
     * int 类型转化为hex
     *
     * @param n 整数
     * @return String
     */
    private String intToHex(int n) {
        StringBuffer s = new StringBuffer();
        StringBuilder a = new StringBuilder();
        char[] b = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        while (n != 0) {
            s = s.append(b[n % 16]);
            n = n / 16;
            if (s.length() % 2 == 0) {
                s.reverse();
                a.append(s.toString());
                s = new StringBuffer();
            }
        }
        return a.toString();
    }

    /**
     * AES256加密
     *
     * @param stringToEncode 输入加密信息
     * @param secretKey      byte[] 加密Secret
     * @return byte[]
     */
    private byte[] AES256Encode(String stringToEncode, byte[] secretKey) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey, "AES256");
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(toByteArray(stringToEncode));
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
     * AES128加密
     *
     * @param stringToEncode 输入加密信息
     * @param secretKey      byte[] 加密Secret
     * @return byte[]
     */
    private byte[] AES128Encode(String stringToEncode, byte[] secretKey) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey, "AES128");
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(toByteArray(stringToEncode));
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
     * Dialog显示临时密码
     *
     * @param string 临时密码
     */
    private void showPwdDialog(final String string) {
        final Dialog dialog = DialogUtils.createTipsDialogWithCancel(this, "*" + string);
        TextView tips = dialog.findViewById(R.id.tips_tv);
        tips.setTextSize(20);
        Button button = dialog.findViewById(R.id.dialog_cancel_btn);
        button.setText(getText(R.string.click_to_copy));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    ClipboardManager clipboardManager = (ClipboardManager) TempPwdActivity.this.
                            getSystemService(Context.CLIPBOARD_SERVICE);

                    ClipData clipData = ClipData.newPlainText(getResources().getString(R.string.temp_pwd), "*" + string);
                    clipboardManager.setPrimaryClip(clipData);
                    Toast.makeText(TempPwdActivity.this, getResources().getString(R.string.replicating_success), Toast.LENGTH_SHORT).show();

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                dialog.cancel();
            }
        });
        dialog.show();
    }

    /**
     * 获取随机Secret
     *
     * @return String
     */
    private String getRandomSecret() {
        String tempStr = mSecretList.get(new Random().nextInt(4));
        String mRandomStr = createRandomNum();
        LogUtil.d(TAG, "tempStr = " + tempStr + "\n" +
                "mRandomStr = " + mRandomStr + "  :  " + mRandomStr.length());
        return (tempStr.substring(0, tempStr.length() - 2)) + mRandomStr;
    }

    /**
     * 获取随机字符
     *
     * @return String 00 -- 99
     */
    private String createRandomNum() {
        do {
            mRandomNum = new Random().nextInt(100);
        } while (mExistNum.contains(mRandomNum));
        return NUMBER_100[mRandomNum];
    }

    public class TempPwdAdapter extends RecyclerView.Adapter<TempPwdAdapter.MyViewHolder> {

        private Context mContext;
        private ArrayList<TempPwd> mTempPwdList;

        private TempPwdAdapter(Context context) {
            mContext = context;
            mTempPwdList = TempPwdDao.getInstance(TempPwdActivity.this).queryAllByDevNodeId(mNodeId);
        }

        private void addItem(TempPwd tempPwd) {
            mTempPwdList.add(0, tempPwd);
        }

        private void deleteItem(int positionDelete) {
            mTempPwdList.remove(positionDelete);
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_recycler_temp_pwd,
                    parent,
                    false);
            SwipeLayout swipeLayout = inflate.findViewById(R.id.item_ll_temp_pwd);
            swipeLayout.setClickToClose(true);
            swipeLayout.setRightSwipeEnabled(true);
            return new MyViewHolder(inflate);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull MyViewHolder viewHolder, @SuppressLint("RecyclerView") final int position) {
            final TempPwd tempPwdInfo = mTempPwdList.get(position);
            long failureTime;
            final String tempPwd;
            if (tempPwdInfo != null) {
                tempPwd = tempPwdInfo.getTempPwd();
                failureTime = DateTimeUtil.getFailureTime(tempPwdInfo.getPwdCreateTime());
                viewHolder.mTempPwdTv.setText(
                        tempPwd.substring(0, 3) +
                                getResources().getString(R.string.temp_password) +
                                tempPwd.substring(tempPwd.length() - 3)
                );
                viewHolder.mTempPwdFailureTimeTv.setText(DateTimeUtil.timeStamp2Date(
                        String.valueOf(failureTime),
                        "yyyy-MM-dd HH:mm"));
                if (System.currentTimeMillis() / 1000 - failureTime >= 0) {
//                    viewHolder.mTempPwdValidIv.setText(getResources().getString(R.string.temp_pwd_invalid));
//                    viewHolder.mTempPwdValidIv.setTextColor(getResources().getColor(R.color.red));
                    viewHolder.mTempPwdValidIv.setImageResource(R.mipmap.icon_invalid);
                    viewHolder.mDelete.setVisibility(View.VISIBLE);
                    viewHolder.mShare.setVisibility(View.GONE);
                } else {
//                    viewHolder.mTempPwdValidIv.setText(getResources().getString(R.string.temp_pwd_valid));
//                    viewHolder.mTempPwdValidIv.setTextColor(getResources().getColor(R.color.light_black));
                    viewHolder.mTempPwdValidIv.setImageResource(R.mipmap.icon_valid);
                    viewHolder.mDelete.setVisibility(View.GONE);
                    viewHolder.mShare.setVisibility(View.VISIBLE);
                    mExistNum.add(tempPwdInfo.getRandomNum());
                }
                viewHolder.mTempPwdLl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showPwdDialog(String.valueOf(tempPwdInfo.getTempPwd()));
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
                        SystemUtils.shareText(TempPwdActivity.this, getString(R.string.share),"*" + tempPwd);
//                        Toast.makeText(TempPwdActivity.this, "还没有实现", Toast.LENGTH_SHORT).show();
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
            private ImageView mTempPwdValidIv;
            private LinearLayout mTempPwdLl;
            private LinearLayout mDelete;
            private LinearLayout mShare;

            private MyViewHolder(View itemView) {
                super(itemView);
                mSwipeLayout = (SwipeLayout) itemView;
                mTempPwdValidIv = itemView.findViewById(R.id.iv_temp_pwd_valid);
                mTempPwdTv = itemView.findViewById(R.id.tv_temp_pwd);
                mTempPwdFailureTimeTv = itemView.findViewById(R.id.tv_temp_pwd_failure_time);
                mTempPwdLl = itemView.findViewById(R.id.ll_temp_pwd);
                mDelete = itemView.findViewById(R.id.ll_delete);
                mShare = itemView.findViewById(R.id.ll_share);
            }
        }
    }
}