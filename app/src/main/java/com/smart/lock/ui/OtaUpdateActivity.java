package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.action.AbstractTransaction;
import com.smart.lock.action.CheckOtaAction;
import com.smart.lock.action.CheckVersionAction;
import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleCardService;
import com.smart.lock.ble.listener.DeviceStateCallback;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.ble.parser.OtaAESPacketParser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.entity.Device;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.entity.VersionModel;
import com.smart.lock.transfer.HttpCodeHelper;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.FileUtil;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.SystemUtils;
import com.smart.lock.utils.ToastUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

public class OtaUpdateActivity extends Activity implements View.OnClickListener, UiListener, DeviceStateCallback, Handler.Callback {

    private static final String TAG = OtaUpdateActivity.class.getSimpleName();
    /**
     * 返回控件
     */
    private ImageView ivBack;

    /**
     * 检查更新控件
     */
    private LinearLayout mCheckVersion;

    /**
     * 更新提示
     */
    private ProgressBar mProgressCheck;

    /**
     * 进度条
     */
    private ProgressBar mPb;

    /**
     * 进度显示
     */
    private TextView mTvProgress = null;

    /**
     * 下载更新
     */
    private Button mStartBt;

    private BluetoothAdapter mBtAdapter = null;

    private TextView mDeviceSnTv;

    /**
     * 更新界面
     */
    private ScrollView mVersionUpdate;

    /**
     * 绑定的设备SN
     */
    private String mDeviceSn;

    private DeviceInfo mDefaultDev; //默认设备

    /**
     * OTA更新成功标识
     */
    private boolean mOtaUpdateComplete = false;

    /**
     * 版本下载连接
     */
    private String mDownloadUrl = ConstantUtil.BASE_URL;

    private String tempPath;

    /**
     * 版本更新内容
     */
    private TextView mUpdateMsg;

    /**
     *
     */
    private TextView mCurrentVersion;

    private TextView mLatestVersion;

    /**
     * 升级状态
     */
    private TextView mConnetStatus;

    /**
     * 升级文件的路径
     */
    private String mDevicePath;

    //back time
    private long mBackPressedTime;
    /**
     * DFU状态
     */
    private int mDfuReady;

    private byte[] gCmdBytes;
    private int iCmdIndex = 0;
    private int iCmdLen = 0;

    private final static int PAYLOAD_LEN = 20;

    private boolean bWriteDfuData = false;

    /**
     * 蓝牙服务类
     */
    private BleManagerHelper mBleManagerHelper;

    /**
     * 文件一共的大小
     */
    int fileSize = 0;

    /**
     * 已经下载的大小
     */
    int downloadSize = 0;

    public static final int DOWNLOAD_PREPARE = 0;
    public static final int DOWNLOAD_WORK = 1;
    public static final int DOWNLOAD_OK = 2;
    public static final int DOWNLOAD_ERROR = 3;
    public static final int NOT_NETWORK = 4;

    /**
     * 下载成功标志
     */
    private boolean isSuccess = true;
    private String fileSizeText = null;

    /**
     * 文件名称
     */
    private String mFileName;

    /**
     * 下载线程
     */
    private Thread mThread;

    private Device mDevice;

    private VersionModel mVersionModel;

    public static final int OTA_PREPARE = 0xFF00;
    public static final int OTA_START = 0xFF01;
    public static final int OTA_END = 0xFF02;

    private static final int TAG_OTA_PREPARE = 0;
    private static final int TAG_OTA_START = 1;
    private static final int TAG_OTA_END = 2;

    private static final int STATE_PROGRESS = 10;

    private final OtaAESPacketParser mOtaParser = new OtaAESPacketParser();

    /**
     * handler处理消息
     */
    private Handler handler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ota_update_activity);
        initView();
        initEvent();
        initDate();
        changeView(0);
    }


    private void initView() {
        ivBack = findViewById(R.id.iv_back_sysset);
        mCheckVersion = findViewById(R.id.check_version);
        mProgressCheck = findViewById(R.id.progress_check);
        mStartBt = findViewById(R.id.version_start);
        mConnetStatus = findViewById(R.id.connect_status);
        mPb = findViewById(R.id.progress_bar);
        mTvProgress = findViewById(R.id.progress_size);
        mVersionUpdate = findViewById(R.id.update_version);
        mDeviceSnTv = findViewById(R.id.dev_mac);
        mUpdateMsg = findViewById(R.id.update_content);
        mCurrentVersion = findViewById(R.id.tv_current_version);
        mLatestVersion = findViewById(R.id.tv_latest_version);
    }

    private void initEvent() {
        ivBack.setOnClickListener(this);
        mStartBt.setOnClickListener(this);
    }

    private void initDate() {
        mDefaultDev = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
        LogUtil.d(TAG, "mDefaultDev : " + mDefaultDev.toString());
        mPb.setProgress(100);

        mBleManagerHelper = BleManagerHelper.getInstance(this);
        mBleManagerHelper.addUiListener(this);
        mDevice = Device.getInstance(this);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mBleManagerHelper.getBleCardService().registerDevStateCb(this);
        handler = new Handler(this);

        mVersionModel = (VersionModel) getIntent().getSerializableExtra(ConstantUtil.SERIALIZABLE_DEV_VERSION_MODEL);
        if (mVersionModel == null) {
            showMessage("没有可更新的文件");
            finish();
        } else {
            mFileName = mVersionModel.fileName;
            getPath();
            mDownloadUrl += mVersionModel.path;
            mUpdateMsg.setText(mVersionModel.msg);
            String curSw = mDefaultDev.getDeviceSwVersion().split("_")[1];
            mCurrentVersion.setText(curSw);
            mLatestVersion.setText(mVersionModel.versionName);
            mDeviceSnTv.setText(mDefaultDev.getDeviceSn());

            int len = mVersionModel.versionName.length();
            int code = 0;
            int swLen = mDefaultDev.getDeviceSwVersion().length();
            if (len >= 5 && swLen >= 5)
                code = StringUtil.compareVersion(mVersionModel.versionName, mDefaultDev.getDeviceSwVersion().split("_")[1]);
//            if (0 == code || code == -1) {
//                compareVersion(CheckVersionAction.NO_NEW_VERSION);
//            } else {
//                if (mVersionModel.forceUpdate) {
//                    compareVersion(CheckVersionAction.MAST_UPDATE_VERSION);
//                } else {
//                    compareVersion(CheckVersionAction.SELECT_VERSION_UPDATE);
//                }
//            }
            compareVersion(CheckVersionAction.SELECT_VERSION_UPDATE);

            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /**
     * 得到文件的保存路径
     *
     * @return
     * @throws IOException
     */
    private void getPath() {
        try {
            String dir = FileUtil.createDir(this, ConstantUtil.DEV_DIR_NAME) + File.separator;
            mDevicePath = dir + mFileName;

            tempPath = mDevicePath + ".temp";
            FileUtil.clearFiles(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void compareVersion(int type) {
        switch (type) {
            case CheckVersionAction.NO_NEW_VERSION:
                changeView(1);
                mUpdateMsg.setText(R.string.http_version_latest);
                break;
            case CheckVersionAction.SELECT_VERSION_UPDATE:
                changeView(0);
                toDownload(false);
                break;
            case CheckVersionAction.MAST_UPDATE_VERSION:
                toDownload(true);
                break;
        }
    }

    public void toDownload(boolean mastDownload) {
        File file = new File(mDevicePath);
        if (file.exists()) {
            downloadSize = 0;
            fileSize = 0;
            mStartBt.setText(R.string.start_update);
            mPb.setProgress(0);
            mConnetStatus.setText(R.string.check_sd_has_new_version);
            mStartBt.setEnabled(true);
        } else {
            if (mastDownload) {
                mStartBt.setEnabled(false);
                mPb.setProgress(0);

                if (mThread == null) {
                    mThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            downloadFile();
                        }
                    });
                }
                mThread.start();
            } else {
                mStartBt.setVisibility(View.VISIBLE);
                mStartBt.setEnabled(true);
                mPb.setProgress(0);
            }
        }
    }

    /**
     * 文件下载
     */
    private void downloadFile() {
        isSuccess = true;
        FileOutputStream fos = null;
        InputStream is = null;
        try {
            URL u = new URL(mDownloadUrl);
            URLConnection conn = u.openConnection();
            conn.connect();
            is = conn.getInputStream();
            conn.setConnectTimeout(10 * 1000);
            conn.setReadTimeout(20 * 1000);
            fileSize = conn.getContentLength();
            if (fileSize < 1 || is == null) {
                sendMessage(DOWNLOAD_ERROR);
            } else {
                sendMessage(DOWNLOAD_PREPARE);
                File downFile = new File(tempPath);
                if (downFile.exists()) {
                    downFile.delete();
                }
                fos = new FileOutputStream(downFile);
                byte[] bytes = new byte[200];
                int len = -1;
                while ((len = is.read(bytes)) != -1) {
                    fos.write(bytes, 0, len);
                    fos.flush();
                    downloadSize += len;
                    sendMessage(DOWNLOAD_WORK);

                }
                if (isSuccess) {
                    copyFile();
                    sendMessage(DOWNLOAD_OK);
                }
            }
        } catch (Exception e) {
            sendMessage(DOWNLOAD_ERROR);
            e.printStackTrace();
        } finally {
            try {
                is.close();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void copyFile() {
        File tempFile = new File(tempPath);
        if (!tempFile.exists()) {
            return;
        }
        File newFile = new File(mDevicePath);
        if (!newFile.exists()) {
            try {
                newFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileInputStream tempInputStream = null;
        FileOutputStream newOutPutStream = null;
        try {
            tempInputStream = new FileInputStream(tempFile);
            newOutPutStream = new FileOutputStream(newFile);
            byte[] buff = new byte[1024 * 200];
            int len = -1;
            while ((len = tempInputStream.read(buff)) != -1) {
                newOutPutStream.write(buff, 0, len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (newOutPutStream != null) {
                try {
                    newOutPutStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (tempInputStream != null) {
                try {
                    tempInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        tempFile.delete();
    }

    /**
     * 给hand发送消息
     *
     * @param what
     */
    private void sendMessage(int what) {
        android.os.Message m = new android.os.Message();
        m.what = what;
        handler.sendMessage(m);
    }


    private void changeView(int action) {
        switch (action) {
            case 0:
                mCheckVersion.setVisibility(View.GONE);
                mVersionUpdate.setVisibility(View.VISIBLE);
                mConnetStatus.setText(R.string.ready_download);
                break;
            case 1:
                mCheckVersion.setVisibility(View.GONE);
                mVersionUpdate.setVisibility(View.VISIBLE);
                mStartBt.setText(R.string.download_version);
                mStartBt.setEnabled(false);
                mPb.setProgress(0);
                break;
            case 2:
                break;
            default:
                break;
        }
    }

    /**
     * 更新DFU状态
     *
     * @param status
     */
    private void updateDfuReady(int status) {
        // clean dfu ready status
        if (status == 0) {
            mDfuReady &= Device.DFU_FW_LOADED; // only keep the fw selection;
            mProgressCheck.setProgress(0);
        } else {
            if (status == Device.DFU_CHAR_DISCONNECTED) {
                mDfuReady &= ~Device.DFU_CHAR_EXISTS;
            } else if (status == Device.DFU_FW_UNLOADED)
                mDfuReady &= ~Device.DFU_FW_LOADED;
            else
                // update ready status
                mDfuReady |= status;

            if (FileUtil.fileExists(mDevicePath)) {
                mDfuReady |= Device.DFU_FW_LOADED;
            } else {
                mDfuReady &= ~Device.DFU_FW_LOADED;
            }

            if (mDevice.getState() == Device.BLE_CONNECTED)
                mConnetStatus.setText(R.string.connection_success);
            else
                mConnetStatus.setText(R.string.connect_failed);
        }
        mPb.setProgress(0);
    }

    private void prepareDFU() {
        // write start command - 0 to 1580 command handle;
        buildAndSendDFUCommand(TAG_OTA_PREPARE);

        buildAndSendDFUCommand(TAG_OTA_START);
        // wait 100ms;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        writeCommandAction();
    }

    private void writeCommandAction() {
        if (mBleManagerHelper.getBleCardService() != null) {
            writeCommandByPosition();
        }
    }

    private void writeCommandByPosition() {
        if (mOtaParser.hasNextPacket() && mBleManagerHelper.getBleCardService() != null) {
            bWriteDfuData = true;
            byte[] cmd = mOtaParser.getNextPacket();
            mBleManagerHelper.getBleCardService().sendCmdOtaData(cmd, Message.TYPE_BLE_SEND_OTA_DATA);
        }
    }

    private void endDFU() {
        // write command data to 1580 data handle;
        buildAndSendDFUCommand(TAG_OTA_END);
        //mHandler = null;
        mOtaParser.clear();
    }

    private void buildAndSendDFUCommand(int action) {
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        byte[] sendData = new byte[20];
        switch (action) {
            case TAG_OTA_PREPARE:
                mConnetStatus.setText(R.string.checking_version);
//                byte[] prePareCmd = new byte[]{OTA_PREPARE & 0xFF, (byte) (OTA_PREPARE >> 8 & 0xFF)}; //泰凌微
                byte[] prePareCmd = new byte[16];
                for (int i = 0; i < 16; i++) {
                    prePareCmd[i] = (byte) 0xFF;
                }
                prePareCmd[0] = OTA_PREPARE & 0xFF;
                prePareCmd[1] = (byte) (OTA_PREPARE >> 8 & 0xFF);

                byte[] buf = new byte[16];

                try {
                    if (MessageCreator.mIs128Code)
                        AES_ECB_PKCS7.AES128Encode(prePareCmd, buf, MessageCreator.m128AK);
                    else
                        AES_ECB_PKCS7.AES256Encode(prePareCmd, buf, MessageCreator.m256AK);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.arraycopy(buf, 0, sendData, 0, 16);
                Arrays.fill(sendData, 16, 20, (byte) 0xFF);

                mBleManagerHelper.getBleCardService().sendCmdOtaData(sendData, Message.TYPE_BLE_SEND_OTA_DATA);
                break;
            case TAG_OTA_START:
//                byte[] dfuCmd = new byte[]{OTA_START & 0xFF, (byte) (OTA_START >> 8 & 0xFF)}; //泰凌微

                byte[] dfuCmd = new byte[16];
                for (int i = 0; i < 16; i++) {
                    dfuCmd[i] = (byte) 0xFF;
                }
                dfuCmd[0] = OTA_START & 0xFF;
                dfuCmd[1] = (byte) (OTA_START >> 8 & 0xFF);

                byte[] startBuf = new byte[16];

                try {
                    if (MessageCreator.mIs128Code)
                        AES_ECB_PKCS7.AES128Encode(dfuCmd, startBuf, MessageCreator.m128AK);
                    else
                        AES_ECB_PKCS7.AES256Encode(dfuCmd, startBuf, MessageCreator.m256AK);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.arraycopy(startBuf, 0, sendData, 0, 16);
                Arrays.fill(sendData, 16, 20, (byte) 0xFF);

                mBleManagerHelper.getBleCardService().sendCmdOtaData(sendData, Message.TYPE_BLE_SEND_OTA_DATA);
                break;
            case TAG_OTA_END:
                if (bWriteDfuData) {
                    byte[] data = new byte[8];
                    data[0] = OTA_END & 0xFF;
                    data[1] = (byte) ((OTA_END >> 8) & 0xFF);
                    data[2] = (byte) (iCmdIndex & 0xFF);
                    data[3] = (byte) (iCmdIndex >> 8 & 0xFF);
                    data[4] = (byte) (~iCmdIndex & 0xFF);
                    data[5] = (byte) (~iCmdIndex >> 8 & 0xFF);

                    int crc = mOtaParser.crc16(data);
                    mOtaParser.fillCrc(data, crc);

                    byte[] endCmd = new byte[16];
                    System.arraycopy(data, 0, endCmd, 0, 8);
                    Arrays.fill(endCmd, 8, 16, (byte) 0xFF);


                    byte[] endBuf = new byte[16];

                    try {
                        if (MessageCreator.mIs128Code)
                            AES_ECB_PKCS7.AES128Encode(endCmd, endBuf, MessageCreator.m128AK);
                        else
                            AES_ECB_PKCS7.AES256Encode(endCmd, endBuf, MessageCreator.m256AK);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    System.arraycopy(endBuf, 0, sendData, 0, 16);
                    Arrays.fill(sendData, 16, 20, (byte) 0xFF);

                    mBleManagerHelper.getBleCardService().sendCmdOtaData(sendData, Message.TYPE_BLE_SEND_OTA_DATA);
                }
                bWriteDfuData = false;
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBleManagerHelper != null) {
            mBleManagerHelper.removeUiListener(this);
            if (mBleManagerHelper.getBleCardService() != null)
                mBleManagerHelper.getBleCardService().removeDevStateCb(this);
        }


        try {
            if (mThread != null) {
                mThread.interrupt();
            }
            mThread = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back_sysset:
                if (mDevice != null && mDevice.getState() == Device.BLE_CONNECTED && mOtaParser.hasNextPacket()) {
//                    long curTime = SystemClock.uptimeMillis();
//                    if (curTime - mBackPressedTime < 3000) {
//                        bWriteDfuData = false;
//                        mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_EXIT_OTA_UPDATE);
//                        finish();
//                        return;
//                    }
//                    mBackPressedTime = curTime;
                    ToastUtil.showShort(this, getString(R.string.ota_back_message));
                } else {
                    setResult(CheckOtaActivity.CHECK_DEV_VERSION);
                    finish();
                }
                break;
            case R.id.version_start:
                if (mStartBt.getText().toString().trim().equals(getString(R.string.download_version))) {
                    toDownload(true);
                } else {
                    mConnetStatus.setText(R.string.start_update);
                    gCmdBytes = FileUtil.loadFirmware(mDevicePath);
                    mOtaParser.set(gCmdBytes);
                    if (Device.getInstance(this).getState() == Device.BLE_CONNECTED && mBleManagerHelper.getBleCardService() != null) {
                        mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_OTA_UPDATE);
                    } else {
                        showMessage(getString(R.string.plz_reconnect));
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void deviceStateChange(Device device, int state) {
        mDevice = device;
        switch (state) {
            case BluetoothGatt.GATT_SUCCESS:

                break;
            case BleMsg.STATE_DISCONNECTED:
                if (mOtaParser.hasNextPacket()) {
                    bWriteDfuData = false;
                    mConnetStatus.setText(R.string.ota_file_dan);
                } else if (mOtaParser.getTotal() != 0) {
                    mConnetStatus.setText(R.string.dfu_end_waiting);
                } else {
                    mStartBt.setEnabled(false);
                    mConnetStatus.setText(R.string.disconnect_ble);
                }
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
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
                final byte[] errCode = extra.getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode != null)
                    dispatchErrorCode(errCode[3]);

                break;
            case Message.TYPE_BLE_RECEIVER_CMD_04:
                LogUtil.i(TAG, "receiver 04!");
                if (!bWriteDfuData && !mOtaParser.hasNextPacket()) {
//                    if (StringUtil.checkNotNull(mVersionModel.versionName)) {
//                        mDefaultDev.setDeviceSwVersion(mVersionModel.versionName);
//                        DeviceInfoDao.getInstance(this).updateDeviceInfo(mDefaultDev);
//                    }
                    String dir = FileUtil.createDir(this, ConstantUtil.DEV_DIR_NAME) + File.separator;
                    FileUtil.clearFiles(dir);
                    mConnetStatus.setText(R.string.ota_complete);
                } else {
                    downloadSize = 0;
                    fileSize = 0;
                    mStartBt.setText(R.string.start_update);
                    mPb.setProgress(0);
                    mOtaParser.clear();
                    mConnetStatus.setText(R.string.new_dev_version);
                    mTvProgress.setText("0" + "%");
                    mStartBt.setEnabled(true);
                }
                break;
            default:
                break;

        }
    }

    private void dispatchErrorCode(byte errCode) {
        LogUtil.i(TAG, "errCode : " + errCode);
        switch (errCode) {
            case BleMsg.TYPE_ALLOW_OTA_UPDATE:
                prepareDFU();
                mStartBt.setEnabled(false);
                break;
            case BleMsg.TYPE_REFUSE_OTA_UPDATE:
                mConnetStatus.setText(R.string.device_busy);
                mStartBt.setEnabled(true);
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

    @Override
    public void reConnectBle(Device device) {
        mConnetStatus.setText(R.string.bt_connecting);
    }

    @Override
    public void sendFailed(Message msg) {

    }

    @Override
    public void addUserSuccess(Device device) {

    }

    @Override
    public void scanDevFailed() {
        mConnetStatus.setText(R.string.connect_failed);
        mStartBt.setEnabled(true);
        updateDfuReady(Device.DFU_CHAR_DISCONNECTED);
    }

    @Override
    public void onBackPressed() {
        if (mDevice != null && mDevice.getState() == Device.BLE_CONNECTED && mOtaParser.hasNextPacket()) {
//            long curTime = SystemClock.uptimeMillis();
//            if (curTime - mBackPressedTime < 3000) {
//                bWriteDfuData = false;
//                mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_EXIT_OTA_UPDATE);
//                finish();
//                return;
//            }
//            mBackPressedTime = curTime;
            ToastUtil.showShort(this, getString(R.string.ota_back_message));
        } else {
            setResult(CheckOtaActivity.CHECK_DEV_VERSION);
            finish();
        }
    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onServicesDiscovered(int state) {

    }

    @Override
    public void onGattStateChanged(int state, int type) {
        if (state == BluetoothGatt.GATT_SUCCESS) {
            LogUtil.d(TAG, "state : " + state + type);
//            sendMessage(type);


            switch (type) {
                case BleCardService.READ:
                    LogUtil.d(TAG, "READ");
                    if (bWriteDfuData) {
                        if (mOtaParser.hasNextPacket()) {
                            writeCommandByPosition();
                        } else { // end of writing command;
                            endDFU();
                        }
                    }
                    break;
                case BleCardService.WRITE:
                    LogUtil.d(TAG, "WRITE");
                    if (bWriteDfuData) {
                        boolean ret = mBleManagerHelper.getBleCardService().validateOta(Message.TYPE_BLE_SEND_OTA_DATA, mOtaParser.getNextPacketIndex());
                        if (!ret) {
                            if (mOtaParser.hasNextPacket()) {
                                writeCommandByPosition();
                            } else { // end of writing command;
                                endDFU();
                            }
                        }
                    }
                    if (mOtaParser.invalidateProgress()) {
                        sendMessage(STATE_PROGRESS);
                    }
                    break;
            }
        }
    }


    @Override
    public boolean handleMessage(android.os.Message msg) {

        switch (msg.what) {
            case STATE_PROGRESS:
                mPb.setProgress(mOtaParser.getProgress());
                mTvProgress.setText(mOtaParser.getProgress() + "%");
                mConnetStatus.setText(R.string.ota_updating);
                if (mDfuReady == Device.DFU_READY) {
                    mStartBt.setEnabled(true);
                    mConnetStatus.setText(R.string.ota_complete);
                }
                break;
            case BleCardService.WRITE:
                LogUtil.d(TAG, "WRITE");
                if (bWriteDfuData) {
                    boolean ret = mBleManagerHelper.getBleCardService().validateOta(Message.TYPE_BLE_SEND_OTA_DATA, mOtaParser.getNextPacketIndex());
                    if (!ret) {
                        mPb.setProgress(mOtaParser.getProgress());
                        if (mOtaParser.hasNextPacket()) {
                            mConnetStatus.setText(R.string.ota_updating);
                            writeCommandByPosition();
                        } else { // end of writing command;
                            endDFU();
                        }
                    }
                }
                break;
            case DOWNLOAD_PREPARE:
                showMessage(getString(R.string.ready_download));
                LogUtil.i(TAG, "一共:" + fileSize);
                mPb.setProgress(100);
                fileSizeText = FileUtil.formatFileSize(fileSize);
                break;
            case DOWNLOAD_WORK:
                LogUtil.i(TAG, "已下载:" + downloadSize);
                mPb.setProgress(downloadSize * 100 / fileSize);
                if (fileSize < 1) {
                    fileSize = 1;
                }
                mTvProgress.setText(FileUtil.formatFileSize(downloadSize) + "/" + fileSizeText);
                break;
            case DOWNLOAD_OK:
//                    showNotification();
                downloadSize = 0;
                fileSize = 0;
                mStartBt.setText(R.string.start_update);
                mPb.setProgress(0);
                mConnetStatus.setText(R.string.new_dev_version);
                mStartBt.setEnabled(true);
                showMessage(getString(R.string.down_finish));
                break;
            case DOWNLOAD_ERROR:
                try {
                    if (mThread != null) {
                        mThread.interrupt();
                    }
                    mThread = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                isSuccess = false;
                showMessage(getString(R.string.download_error));
                downloadSize = 0;
                fileSize = 0;
                File file = new File(tempPath);
                file.delete();
                compareVersion(CheckVersionAction.SELECT_VERSION_UPDATE);
                break;
            case NOT_NETWORK:
                showMessage(getString(R.string.net_error));
                isSuccess = false;
                break;
        }

        return true;
    }
}
