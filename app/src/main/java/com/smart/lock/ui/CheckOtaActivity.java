package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;
import com.smart.lock.R;
import com.smart.lock.action.AbstractTransaction;
import com.smart.lock.action.CheckOtaAction;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.entity.Device;
import com.smart.lock.entity.VersionModel;
import com.smart.lock.transfer.HttpCodeHelper;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SharedPreferenceUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.widget.SpacesItemDecoration;

import java.util.ArrayList;
import java.util.Iterator;

public class CheckOtaActivity extends AppCompatActivity implements View.OnClickListener, UiListener {
    private static final String TAG = CheckOtaActivity.class.getSimpleName();
    /**
     * 返回控件
     */
    private ImageView ivBack;
    private RecyclerView mOtaUpdateRv;
    private LinearLayout mCheckVersionLl;
    private LinearLayout mEmptyUpdateLl;

    /**
     * 蓝牙服务类
     */
    private BleManagerHelper mBleManagerHelper;
    private Device mDevice; //设备信息

    private DeviceInfo mDefaultDev; //默认设备
    private CheckOtaAction mVersionAction = null;
    private CheckOtaAdapter mOtaAdapter;

    private final int RECEIVER_OTA_VERSION = 0;
    private final int VIEW_GONE_CHECK_VERSION = 1;
    private final int EMPTY_VERSION_UPDATE = 2;

    public static final int CHECK_FP_VERSION = 1001;
    public static final int CHECK_DEV_VERSION = 1002;
    public static final int CHECK_FACE_VERSION = 1003;

    private boolean isHide = false;
    private boolean mCheckFpVersion = false;
    private boolean mHadFP = false;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case RECEIVER_OTA_VERSION:
                    refreshView(VIEW_GONE_CHECK_VERSION);
                    break;
                case CHECK_FP_VERSION:
                    checkFpVersion();
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ota_update_file);
        initView();
        initEvent();
        initDate();
    }

    private void initView() {
        ivBack = findViewById(R.id.iv_back_sysset);
        mOtaUpdateRv = findViewById(R.id.rv_firmware);
        mCheckVersionLl = findViewById(R.id.check_version);
        mEmptyUpdateLl = findViewById(R.id.ll_empty);
    }

    private void initDate() {
        mBleManagerHelper = BleManagerHelper.getInstance(this);
        LogUtil.d(TAG, "uiListener is contains!~");
        mBleManagerHelper.addUiListener(this);
        mDevice = Device.getInstance(this);
        mDefaultDev = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
        mVersionAction = new CheckOtaAction();
        mOtaAdapter = new CheckOtaAdapter(this);
        LinearLayoutManager linerLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mOtaUpdateRv.setLayoutManager(linerLayoutManager);
        mOtaUpdateRv.setItemAnimator(new DefaultItemAnimator());
        mOtaUpdateRv.setAdapter(mOtaAdapter);
        mOtaUpdateRv.addItemDecoration(new SpacesItemDecoration(getResources().getDimensionPixelSize(R.dimen.y10dp)));

//        if (mDevice != null && mDevice.getState() == Device.BLE_CONNECTED) {
//            mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_CHECK_VERSION);
//        } else
//            showMessage(getString(R.string.unconnected_device));
    }

    private void initEvent() {
        ivBack.setOnClickListener(this);
    }

    private void refreshView(int action) {
        switch (action) {
            case VIEW_GONE_CHECK_VERSION:
                mCheckVersionLl.setVisibility(View.GONE);
                mOtaUpdateRv.setVisibility(View.VISIBLE);
                mEmptyUpdateLl.setVisibility(View.GONE);
                break;
            case EMPTY_VERSION_UPDATE:
                mCheckVersionLl.setVisibility(View.GONE);
                mOtaUpdateRv.setVisibility(View.GONE);
                mEmptyUpdateLl.setVisibility(View.VISIBLE);
                break;

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back_sysset:
                finish();
                break;

            default:
                break;
        }
    }

    @Override
    public void deviceStateChange(Device device, int state) {
        mDevice = device;
        switch (state) {
            case BleMsg.STATE_DISCONNECTED:

                break;
            case BleMsg.STATE_CONNECTED:

                break;
            case BleMsg.GATT_SERVICES_DISCOVERED:

                break;
            default:
                break;
        }
    }

    @Override
    public void dispatchUiCallback(Message msg, Device device, int type) {
        mDevice = device;
        Bundle extra = msg.getData();
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1C:
                String sn = StringUtil.asciiDeBytesToCharString(extra.getByteArray(BleMsg.KEY_NODE_SN));
                String swVer = StringUtil.asciiDeBytesToCharString(extra.getByteArray(BleMsg.KEY_SW_VER));
                LogUtil.d(TAG, "SW VERSION = " + swVer + '\n' + "SN = " + sn);
                if (extra.getByteArray(BleMsg.KEY_HW_VER) != null) {
                    String hwVer = StringUtil.asciiDeBytesToCharString(extra.getByteArray(BleMsg.KEY_HW_VER));

                    mDefaultDev.setDeviceSn(sn);
                    mDefaultDev.setDeviceSwVersion(swVer);
                    mDefaultDev.setDeviceHwVersion(hwVer);
                    DeviceInfoDao.getInstance(this).updateDeviceInfo(mDefaultDev);
                    checkFpVersion();
                } else {
                    mDefaultDev.setFpSwVersion(swVer);
                    DeviceInfoDao.getInstance(this).updateDeviceInfo(mDefaultDev);
                    if (!mCheckFpVersion) {
                        mHadFP = true;
                        checkDevVersion(true);
                    } else {
                        mCheckFpVersion = false;
                        mDefaultDev = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true); //刷新数据库
                        if (mVersionAction.respondData.models != null && mDefaultDev != null) {
                            mOtaAdapter.setDataSource(mVersionAction.respondData.models);
                            mOtaAdapter.notifyDataSetChanged(); //刷新升级界面
                        }
                    }
                }
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
                final byte[] errCode = msg.getData().getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode != null)
                    dispatchErrorCode(errCode[3]);
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_44:
                String majorVer = StringUtil.asciiDeBytesToCharString(extra.getByteArray(BleMsg.KEY_FACE_MAJOR_VERSION));
                String nCPUVer = StringUtil.asciiDeBytesToCharString(extra.getByteArray(BleMsg.KEY_FACE_NCPU_VERSION));
                String sCPUVer = StringUtil.asciiDeBytesToCharString(extra.getByteArray(BleMsg.KEY_FACE_SCPU_VERSION));
                String moduleVer = StringUtil.asciiDeBytesToCharString(extra.getByteArray(BleMsg.KEY_FACE_OTA_MODULE));
                mDefaultDev.setFaceMainVersion(majorVer);
                mDefaultDev.setFaceNCPUVersion(nCPUVer);
                mDefaultDev.setFaceSCPUVersion(sCPUVer);
                mDefaultDev.setFaceModuleVersion(moduleVer);
                DeviceInfoDao.getInstance(this).updateDeviceInfo(mDefaultDev);
                LogUtil.d(TAG, "FACE VERSION : major=" + majorVer + '\n' +
                        "nCPU = " + nCPUVer + '\n' +
                        "sCPU = " + sCPUVer + '\n' +
                        "module = " + moduleVer);
                break;
            default:
                break;
        }
    }

    int fpCount = 0;

    private void dispatchErrorCode(byte errCode) {
        LogUtil.i(TAG, "errCode : " + errCode);
        switch (errCode) {
            case BleMsg.TYPE_REFUSE_FINGERPRINT_OTA_UPDATE:
                if (isHide) {
                    checkDevVersion(false);
                }
                break;
            case BleMsg.TYPE_EQUIPMENT_BUSY:
                if (isHide) {
                    if (fpCount < 3) {
                        fpCount++;
                        showMessage(getString(R.string.device_busy));
                        android.os.Message msg = android.os.Message.obtain();
                        msg.what = CHECK_FP_VERSION;
                        mHandler.sendMessageDelayed(msg, 5000);
                    } else {
                        fpCount = 0;
                        checkDevVersion(false);
                    }
                }
                break;
            case BleMsg.TYPE_OPEN_SLIDE:
                if (mBleManagerHelper.getBleCardService() != null)
                    mBleManagerHelper.getBleCardService().cancelCmd(Message.TYPE_BLE_SEND_CMD_19 + "#" + "single");
                showMessage(getString(R.string.plz_open_slide));
                break;
            default:
                break;
        }
    }

    private void checkFpVersion() {
        if (mDevice != null && mDevice.getState() == Device.BLE_CONNECTED) {
            mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_CHECK_FINGERPRINT_VERSION);
        } else
            showMessage(getString(R.string.unconnected_device));
    }

    private void checkFaceVersion() {
        LogUtil.d(TAG, "SEND 41");
        if (mDevice != null && mDevice.getState() == Device.BLE_CONNECTED) {
            mBleManagerHelper.getBleCardService().sendCmd41(BleMsg.CMD_CHECK_FACE_VERSION,
                    (byte) 0,
                    0,
                    BleMsg.INT_DEFAULT_TIMEOUT);
        } else
            showMessage(getString(R.string.unconnected_device));
    }

    private void checkDevVersion(boolean hasFp) {
        if (mDefaultDev != null && !mCheckFpVersion) {
            mVersionAction.setUrl(ConstantUtil.CHECK_FIRMWARE_VERSION);
            mVersionAction.setDeviceSn(mDefaultDev.getDeviceSn());
            mVersionAction.setDevCurVer(mDefaultDev.getDeviceSwVersion());
            mVersionAction.setExtension(ConstantUtil.BIN_EXTENSION);
            if (SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.IS_DMT_TEST)) {
                mVersionAction.setTest(true);
            } else {
                mVersionAction.setTest(false); //正式
            }
            if (hasFp) {
                String fpSwVersion = mDefaultDev.getFpSwVersion();
                String[] fpSw = fpSwVersion.split("\\.");
                String ret = fpSw[fpSw.length - 2];
                mVersionAction.setFpType(fpSwVersion.split("_")[0]);
                mVersionAction.setFpCurVer(mDefaultDev.getFpSwVersion());
                mVersionAction.setFpCurZone(ret);
            }
            if (mDefaultDev.isEnableFace()) {
                mVersionAction.setFaceType(mDefaultDev.getFaceMainVersion().split("_")[0]);
                mVersionAction.setFaceMainCurVer(mDefaultDev.getFaceMainVersion());
                mVersionAction.setFaceNCpuCurVer(mDefaultDev.getFaceNCPUVersion());
                mVersionAction.setFaceSCpuCurVer(mDefaultDev.getFaceSCPUVersion());
                mVersionAction.setFaceSCpuCurVer(mDefaultDev.getFaceModuleVersion());
            }

            mVersionAction.setTransferPayResponse(tCheckDevResponse);
            mVersionAction.transStart(this);
        } else {
            mCheckFpVersion = false;
            mDefaultDev = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true); //刷新数据库
            if (mVersionAction.respondData.models != null && mDefaultDev != null) {
                mOtaAdapter.setDataSource(mVersionAction.respondData.models);
                mOtaAdapter.notifyDataSetChanged(); //刷新升级界面
            }
        }
    }

    @Override
    protected void onResume() {
        LogUtil.d(TAG, "onResume");
        super.onResume();
        isHide = true;
        if (mDevice != null && mDevice.getState() == Device.BLE_CONNECTED) {
            mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_CHECK_VERSION);
            if (mDefaultDev.isEnableFace()) {
                checkFaceVersion();
            }
        } else
            showMessage(getString(R.string.unconnected_device));
    }

    @Override
    protected void onPause() {
        super.onPause();
        isHide = false;
    }

    AbstractTransaction.TransferPayResponse tCheckDevResponse = new AbstractTransaction.TransferPayResponse() {

        @Override
        public void transFailed(String httpCode, String errorInfo) {
//            refreshView(EMPTY_VERSION_UPDATE);
            ArrayList<VersionModel> models = new ArrayList<>();
            VersionModel versionModel = new VersionModel();
            versionModel.type = ConstantUtil.OTA_LOCK_SW_VERSION;
            versionModel.versionCode = 0;
            models.add(versionModel);
            if (mHadFP) {
                VersionModel versionModel2 = new VersionModel();
                versionModel2.type = ConstantUtil.OTA_FP_SW_VERSION;
                versionModel2.versionCode = 0;
                models.add(versionModel2);
            }
            if (mDefaultDev.isEnableFace()) {
                VersionModel versionModel3 = new VersionModel();
                versionModel3.type = ConstantUtil.OTA_FACE_SW_VERSION;
                versionModel3.versionCode = 0;
                models.add(versionModel3);
            }
            sendMessage(RECEIVER_OTA_VERSION, null, 0);
            mOtaAdapter.setDataSource(models);
            mOtaAdapter.notifyDataSetChanged();
        }

        @Override
        public void transComplete() {
            if (mVersionAction.respondData.models.size() == 3 && !mDefaultDev.isEnableFace()) {
                mVersionAction.respondData.models.remove(2);
            }
            if (HttpCodeHelper.RESPONSE_SUCCESS.equals(mVersionAction.respondData.respCode)) {
                sendMessage(RECEIVER_OTA_VERSION, null, 0);
                mOtaAdapter.setDataSource(mVersionAction.respondData.models);
                mOtaAdapter.notifyDataSetChanged();
            } else {
                sendMessage(RECEIVER_OTA_VERSION, null, 0);
                mOtaAdapter.setDataSource(mVersionAction.respondData.models);
                mOtaAdapter.notifyDataSetChanged();
            }
        }
    };

    private void sendMessage(int type, Bundle bundle, long time) {
        android.os.Message msg = new android.os.Message();
        if (bundle != null) {
            msg.setData(bundle);
        }
        msg.what = type;
        if (time != 0) {
            mHandler.sendMessageDelayed(msg, time);
        } else mHandler.sendMessage(msg);
    }

    @Override
    public void reConnectBle(Device device) {

    }

    @Override
    public void sendFailed(Message msg) {

    }

    @Override
    public void addUserSuccess(Device device) {

    }

    @Override
    public void scanDevFailed() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBleManagerHelper.removeUiListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.d(TAG, "requestCode : " + requestCode);
        switch (requestCode) {
            case CHECK_DEV_VERSION:
                if (mDevice != null && mDevice.getState() == Device.BLE_CONNECTED) {
                    mCheckFpVersion = true;
                    mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_CHECK_VERSION);
                    if (mDefaultDev.isEnableFace()) {
                        checkFaceVersion();
                    }
                } else
                    showMessage(getString(R.string.unconnected_device));
                break;
            case CHECK_FP_VERSION:
                if (mDevice != null && mDevice.getState() == Device.BLE_CONNECTED) {
                    mCheckFpVersion = true;
                    mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_CHECK_FINGERPRINT_VERSION);
                } else
                    showMessage(getString(R.string.unconnected_device));
                break;
        }
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public class CheckOtaAdapter extends RecyclerView.Adapter<CheckOtaAdapter.ViewHolder> {
        private Context mContext;
        public ArrayList<VersionModel> mVersionList = new ArrayList<>();

        public CheckOtaAdapter(Context context) {
            mContext = context;
        }

        public void setDataSource(@NonNull ArrayList<VersionModel> versionList) {
            mVersionList = versionList;
//            for (VersionModel model : mVersionList) {
//                if (model.type.equals(ConstantUtil.OTA_FP_SW_VERSION) && !mHadFP) {
//                    mVersionList.remove(model);
//                }
//            }
            Iterator<VersionModel> iterator = mVersionList.iterator();
            while (iterator.hasNext()) {
                VersionModel versionModel = iterator.next();
                if (versionModel.type.equals(ConstantUtil.OTA_FP_SW_VERSION) && !mHadFP) {
                    iterator.remove();
                }
            }

        }

        public void addItem(VersionModel model) {
            mVersionList.add(mVersionList.size(), model);
            notifyItemInserted(mVersionList.size());
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_ota_firmware, parent, false);
            SwipeLayout swipeLayout = inflate.findViewById(R.id.item_ll_ota);
            swipeLayout.setClickToClose(false);
            swipeLayout.setRightSwipeEnabled(false);
            return new ViewHolder(inflate);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder viewHolder, @SuppressLint("RecyclerView") final int position) {
            final VersionModel model = mVersionList.get(position);
            LogUtil.d(TAG, "length =" + mVersionList.size());
            if (model != null) {
//                int len = model.versionName.length();
//                int code = 0;
                if (model.type.equals(ConstantUtil.OTA_FP_SW_VERSION)) {
                    viewHolder.mType.setImageResource(R.mipmap.ota_fingerprint);
                    viewHolder.mNameTv.setText(R.string.fingerprint_firmware);
//                    if (StringUtil.checkNotNull(mDefaultDev.getFpSwVersion())) {
//                        code = StringUtil.compareFPVersion(mDefaultDev.getFpSwVersion(), model.versionName);
//                    }
                } else if (model.type.equals(ConstantUtil.OTA_LOCK_SW_VERSION)) {
                    viewHolder.mType.setImageResource(R.mipmap.ota_lock);
                    viewHolder.mNameTv.setText(R.string.lock_default_name);
                } else if (model.type.equals(ConstantUtil.OTA_FACE_SW_VERSION)) {
                    viewHolder.mType.setImageResource(R.mipmap.icon_face);
                    viewHolder.mNameTv.setText(R.string.face_manager);
                }
                LogUtil.d(TAG, "model type=" + model.type + '\n' + "versionCode = " + model.versionCode);
                if (model.versionCode != 0) {
                    viewHolder.mSwVersion.setText(mContext.getString(R.string.new_dev_version));
                    viewHolder.mNextIconIv.setVisibility(View.VISIBLE);
                    viewHolder.mSwVersion.setTextColor(getResources().getColor(R.color.red));
                    viewHolder.mSwipeLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mDevice != null && mDevice.getState() == Device.BLE_CONNECTED) {
                                Intent intent = new Intent();
                                Bundle bundle = new Bundle();
                                if (model.type.equals(ConstantUtil.OTA_FP_SW_VERSION)) {
                                    bundle.putSerializable(ConstantUtil.SERIALIZABLE_FP_VERSION_MODEL, model);
                                    intent.putExtras(bundle);
                                    intent.setClass(mContext, FpOtaUpdateActivity.class);
                                    CheckOtaActivity.this.startActivityForResult(intent, CHECK_FP_VERSION);
                                } else if (model.type.equals(ConstantUtil.OTA_LOCK_SW_VERSION)) {
                                    bundle.putSerializable(ConstantUtil.SERIALIZABLE_DEV_VERSION_MODEL, model);
                                    intent.putExtras(bundle);
                                    intent.setClass(mContext, OtaUpdateActivity.class);
                                    CheckOtaActivity.this.startActivityForResult(intent, CHECK_DEV_VERSION);
                                } else if (model.type.equals(ConstantUtil.OTA_FACE_SW_VERSION)) {
                                    bundle.putSerializable(ConstantUtil.SERIALIZABLE_FACE_VERSION_MODEL, model);
                                    intent.putExtras(bundle);
                                    intent.setClass(mContext, FaceOtaUpdateActivity.class);
                                    CheckOtaActivity.this.startActivityForResult(intent, CHECK_FACE_VERSION);
                                }

                            } else
                                showMessage(getString(R.string.unconnected_device));

                        }
                    });
                }
            }
        }

        @Override
        public int getItemCount() {
            return mVersionList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {


            SwipeLayout mSwipeLayout;
            TextView mNameTv;
            TextView mSwVersion;
            ImageView mType;
            ImageButton mEditIBtn;
            ImageView mNextIconIv;

            public ViewHolder(View itemView) {
                super(itemView);

                mSwipeLayout = (SwipeLayout) itemView;
                mType = itemView.findViewById(R.id.iv_type);
                mNameTv = itemView.findViewById(R.id.tv_name);
                mEditIBtn = itemView.findViewById(R.id.ib_edit);
                mSwVersion = itemView.findViewById(R.id.tv_sw_version);
                mNextIconIv = itemView.findViewById(R.id.iv_next_icon);
            }
        }
    }
}
