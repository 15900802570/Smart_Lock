package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.smart.lock.widget.CreateTmpPwdDialog;

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

public class TempPwdActivity extends AppCompatActivity implements View.OnClickListener, CreateTmpPwdDialog.OnCreateTmpPwdListener {

    private static String TAG = "TempPwdActivity";

    private TempPwdAdapter mTempPwdAdapter;
    private String mNodeId;
    private String mMac;
    private String mSecret;
    private List<String> mSecretList = new ArrayList<>();
    private DeviceInfo mDefaultDevice;

    private RecyclerView mTempPwdListViewRv;
    private Set<Integer> mExistNum = new HashSet<>();
    private int mRandomNum = Integer.MAX_VALUE; // 零时密码随机数，有效值0-99,其他值无效
    private String mRandomStr = "";

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
        mDefaultDevice = (DeviceInfo) getIntent().getSerializableExtra(BleMsg.KEY_DEFAULT_DEVICE);
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
        } else {
            Toast.makeText(this, getString(R.string.time_not_calibrated), Toast.LENGTH_LONG).show();
        }

        mTempPwdAdapter = new TempPwdAdapter(this);
        mTempPwdListViewRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mTempPwdListViewRv.setItemAnimator(new DefaultItemAnimator());
        mTempPwdListViewRv.setAdapter(mTempPwdAdapter);

        for (TempPwd tempPwd : mTempPwdAdapter.getTempPwdList()) {
            if (System.currentTimeMillis() / 1000 - DateTimeUtil.getFailureTime(tempPwd.getPwdCreateTime(), tempPwd.getRandomNum()) < 0) {
                mExistNum.add(tempPwd.getRandomNum());
            }
        }
        LogUtil.d(TAG, "ExistNum = " + mExistNum +
                '\n' + "Size = " + mExistNum.size());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_temp_pwd_back:
                finish();
                break;
            case R.id.btn_create_temp_pwd:
                if (StringUtil.checkIsNull(mDefaultDevice.getTempSecret())) {
                    Toast.makeText(this, getString(R.string.time_not_calibrated), Toast.LENGTH_LONG).show();
                    return;
                } else {
                    CreateTmpPwdDialog createTmpPwdDialog = new CreateTmpPwdDialog(1);
                    createTmpPwdDialog.show(this.getSupportFragmentManager(), "timePicker");
                }
//                if (createTempPwd()) {
//                    saveTempPwd();
//                }
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
    private boolean createTempPwd(boolean once, int period) {
        if (mExistNum.size() >= 20) {
            ToastUtil.showShort(this, R.string.not_regenerate_temp_pwd);
            return false;
        } else {
            int mCurTime = ((int) Math.ceil(System.currentTimeMillis() / 1800000.0) + period - 1) * 1800;
            LogUtil.d(TAG, "mCurTime = " + mCurTime);

            Long tempSecret;
            if (mIs128Code) {
                tempSecret = (long) StringUtil.getCRC16(AES128Encode(intToHex(mCurTime) + "000000000000000000000000",
                        StringUtil.hexStringToBytes(mIsOnceForTempPwd ? getRandomSecret(once, period) : mSecretList.get(new Random().nextInt(4)))));
            } else {
                tempSecret = (long) StringUtil.getCRC16(AES256Encode(intToHex(mCurTime) + "000000000000000000000000",
                        StringUtil.hexStringToBytes(mIsOnceForTempPwd ? getRandomSecret(once, period) : mSecretList.get(new Random().nextInt(4)))));
            }
            if (mIsOnceForTempPwd) {
                mSecret = getThreeStr(mRandomNum) + tempSecret;
            } else {
                mSecret = String.valueOf(tempSecret);
            }
            showPwdDialog(mSecret, true);
            return true;
        }
    }

    /**
     * 存储临时密码
     */
    private void saveTempPwd() {
        if (!mExistNum.contains(mRandomNum) && mSecret != null) {
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
    private void showPwdDialog(final String string, boolean valid) {
        final Dialog dialog = DialogUtils.createTipsDialogWithCancel(this, "*" + string);
        TextView tips = dialog.findViewById(R.id.tips_tv);
        tips.setTextSize(20);
        Button button = dialog.findViewById(R.id.dialog_cancel_btn);
        if (valid) {
            button.setText(getText(R.string.share));
            button.setBackground(getResources().getDrawable(R.drawable.selector_button_dialog_confirm));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        SystemUtils.shareText(TempPwdActivity.this, getString(R.string.share), "*" + string);

                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                    dialog.cancel();
                }
            });
        } else {
            button.setText(getText(R.string.cancel));
            button.setBackground(getResources().getDrawable(R.drawable.selector_button_dialog_cancel));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.cancel();
                }
            });
        }
        dialog.show();
    }

    /**
     * 获取随机Secret
     *
     * @return String
     */
    private String getRandomSecret(boolean once, int period) {
        String tempStr = mSecretList.get(new Random().nextInt(4));
        mRandomStr = createRandomNum(once, period);
        String temp = (tempStr.substring(0, tempStr.length() - 4)) + "0" + mRandomStr;
        LogUtil.d(TAG, "tempStr = " + tempStr + "\n" +
                "mRandomStr = " + mRandomStr + "  :  " + mRandomStr.length() + "\n" + "str = " +
                temp);
        return temp;
    }

    /**
     * 获取随机字符
     *
     * @return String 000 -- 999
     */
    private String createRandomNum(boolean once, int period) {
        if (once) {
            do {
                mRandomNum = Math.abs(new Random().nextInt(40) + 1 + (period - 1) * 40);
            } while (mExistNum.contains(mRandomNum) || mRandomNum % 2 == 0);
        } else {
            do {
                mRandomNum = Math.abs(new Random().nextInt(40) + 1 + (period - 1) * 40);
            } while (mExistNum.contains(mRandomNum) || mRandomNum % 2 == 1);
        }
        StringBuilder threeStr = new StringBuilder(Integer.toHexString(mRandomNum));
        LogUtil.d(TAG, "str = " + threeStr);
        while (threeStr.length() < 3) {
            threeStr.insert(0, "0");
        }
        return threeStr.toString();
    }

    private String getThreeStr(int randomNum) {
        String str;
        if (randomNum < 10) {
            str = "00" + randomNum;
        } else if (randomNum < 100) {
            str = "0" + randomNum;
        } else {
            str = String.valueOf(randomNum);
        }
        return str;
    }

    @Override
    public void onCreateTmpPwdConfirm(boolean once, int period, int requestCode) {
        LogUtil.d(TAG, "one + " + once + '\n' + "period = " + period);
        if (createTempPwd(once, period)) {
            saveTempPwd();
        }
    }

    public class TempPwdAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEAD = 0;
        private static final int TYPE_BODY = 1;
        private static final int TYPE_FOOT = 2;
        private int countHead = 0;
        private int countFoot = 2;
        private Context mContext;
        private ArrayList<TempPwd> mTempPwdList;

        private int getBodySize() {
            return mTempPwdList.size();
        }

        private boolean isHead(int position) {
            return countHead != 0 && position < countHead;
        }

        private boolean isFoot(int position) {
            return countFoot != 0 && (position >= (getBodySize() + countHead));
        }

        public int getItemViewType(int position) {
            if (isHead(position)) {
                return TYPE_HEAD;
            } else if (isFoot(position)) {
                return TYPE_FOOT;
            } else {
                return TYPE_BODY;
            }
        }

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

        private ArrayList<TempPwd> getTempPwdList() {
            return mTempPwdList;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case TYPE_HEAD:
                    return new FootViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_recycle_foot, parent, false));
                case TYPE_BODY:
                    View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_recycler_temp_pwd,
                            parent,
                            false);
                    SwipeLayout swipeLayout = inflate.findViewById(R.id.item_ll_temp_pwd);
                    swipeLayout.setClickToClose(true);
                    swipeLayout.setRightSwipeEnabled(true);
                    return new MyViewHolder(inflate);
                case TYPE_FOOT:
                    return new FootViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_recycle_foot, parent, false));
                default:
                    return new FootViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_recycle_foot, parent, false));
            }

        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, @SuppressLint("RecyclerView") final int position) {
            if (viewHolder instanceof MyViewHolder) {
                final TempPwd tempPwdInfo = mTempPwdList.get(position - countHead);
                long failureTime;
                final String tempPwd;
                final boolean valid;
                if (tempPwdInfo != null) {
                    tempPwd = tempPwdInfo.getTempPwd();
                    failureTime = DateTimeUtil.getFailureTime(tempPwdInfo.getPwdCreateTime(), tempPwdInfo.getRandomNum());
                    ((MyViewHolder) viewHolder).mTempPwdTv.setText(
                            tempPwd.substring(0, 3) +
                                    getResources().getString(R.string.temp_password) +
                                    tempPwd.substring(tempPwd.length() - 3)
                    );
                    ((MyViewHolder) viewHolder).mTempPwdFailureTimeTv.setText(DateTimeUtil.timeStamp2Date(
                            String.valueOf(failureTime),
                            "yyyy-MM-dd HH:mm"));
                    valid = (System.currentTimeMillis() / 1000 - failureTime >= 0) ? false : true;
                    if (!valid) {
//                    viewHolder.mTempPwdValidIv.setText(getResources().getString(R.string.temp_pwd_invalid));
//                    viewHolder.mTempPwdValidIv.setTextColor(getResources().getColor(R.color.red));
                        ((MyViewHolder) viewHolder).mTempPwdValidIv.setImageResource(R.mipmap.icon_invalid);
                        ((MyViewHolder) viewHolder).mDelete.setVisibility(View.VISIBLE);
                        ((MyViewHolder) viewHolder).mShare.setVisibility(View.GONE);
                    } else {
//                    viewHolder.mTempPwdValidIv.setText(getResources().getString(R.string.temp_pwd_valid));
//                    viewHolder.mTempPwdValidIv.setTextColor(getResources().getColor(R.color.light_black));
                        ((MyViewHolder) viewHolder).mTempPwdValidIv.setImageResource(R.mipmap.icon_valid);
                        ((MyViewHolder) viewHolder).mDelete.setVisibility(View.GONE);
                        ((MyViewHolder) viewHolder).mShare.setVisibility(View.VISIBLE);
                    }
                    if (tempPwdInfo.getRandomNum() % 2 == 0) {
                        ((MyViewHolder) viewHolder).mTempPwdCheckNumTv.setText(getString(R.string.many));
                    } else {
                        ((MyViewHolder) viewHolder).mTempPwdCheckNumTv.setText(getString(R.string.one));
                    }
                    if (valid) {
                        ((MyViewHolder) viewHolder).mTempPwdCheckNumTv.setTextColor(getResources().getColor(R.color.blue2));
                    } else {
                        ((MyViewHolder) viewHolder).mTempPwdCheckNumTv.setTextColor(getResources().getColor(R.color.gray1));
                    }
                    ((MyViewHolder) viewHolder).mTempPwdLl.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showPwdDialog(String.valueOf(tempPwdInfo.getTempPwd()), valid);
                        }
                    });
                    ((MyViewHolder) viewHolder).mDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            TempPwdDao.getInstance(TempPwdActivity.this).delete(tempPwdInfo);
                            deleteItem(position);
                            mTempPwdAdapter.notifyDataSetChanged();
                        }
                    });
                    ((MyViewHolder) viewHolder).mShare.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mTempPwdAdapter.notifyDataSetChanged();
                            SystemUtils.shareText(TempPwdActivity.this, getString(R.string.share), "*" + tempPwd);
//                        Toast.makeText(TempPwdActivity.this, "还没有实现", Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            }
        }

        @Override
        public int getItemCount() {
            return mTempPwdList.size() + countHead + countFoot;
        }

        class MyViewHolder extends RecyclerView.ViewHolder {

            SwipeLayout mSwipeLayout;
            private TextView mTempPwdTv;
            private TextView mTempPwdFailureTimeTv;
            private TextView mTempPwdCheckNumTv;
            private ImageView mTempPwdValidIv;
            private LinearLayout mTempPwdLl;
            private LinearLayout mDelete;
            private LinearLayout mShare;

            private MyViewHolder(View itemView) {
                super(itemView);
                mSwipeLayout = (SwipeLayout) itemView;
                mTempPwdValidIv = itemView.findViewById(R.id.iv_temp_pwd_valid);
                mTempPwdTv = itemView.findViewById(R.id.tv_temp_pwd);
                mTempPwdCheckNumTv = itemView.findViewById(R.id.temp_pwd_check_num);
                mTempPwdFailureTimeTv = itemView.findViewById(R.id.tv_temp_pwd_failure_time);
                mTempPwdLl = itemView.findViewById(R.id.ll_temp_pwd);
                mDelete = itemView.findViewById(R.id.ll_delete);
                mShare = itemView.findViewById(R.id.ll_share);
            }
        }

        class FootViewHolder extends RecyclerView.ViewHolder {
            private FootViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}