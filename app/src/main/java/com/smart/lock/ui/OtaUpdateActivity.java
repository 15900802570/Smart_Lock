package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.action.AbstractTransaction;
import com.smart.lock.action.CheckVersionAction;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.entity.VersionModel;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.Device;
import com.smart.lock.ble.message.Message;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.transfer.HttpCodeHelper;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.FileUtil;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.SystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class OtaUpdateActivity extends Activity implements View.OnClickListener {

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
     * 会话秘钥
     */
    private byte[] mAK;

    /**
     * 绑定的设备SN
     */
    private String mDeviceSn;

    private DeviceInfo mDefaultDev; //默认设备

    /**
     * handler
     */
    private Handler mHandler;

    /**
     * 搜索状态标识
     */
    private boolean mScanning;

    /**
     * OTA更新成功标识
     */
    private boolean mOtaUpdateComplete = false;

    /**
     * OTA进度状态
     */
    private int mState = UART_PROFILE_DISCONNECTED;


    /**
     * 蓝牙未连接
     */
    private static final int UART_PROFILE_DISCONNECTED = 21;

    /**
     * 版本下载连接
     */
    private String mDownloadUrl;

    private String tempPath;

    /**
     * 版本更新内容
     */
    private TextView mUpdateMsg;

    /**
     *
     */
    private TextView mVersionName;

    /**
     * scanning for 10 seconds
     */
    private static final long SCAN_PERIOD = 10000;

    /**
     * 升级状态
     */
    private TextView mConnetStatus;

    private BluetoothAdapter.LeScanCallback mLeScanCallback = null;

    /**
     * OTA模式存储的nodeID
     */
    private byte[] mConnectNodeId = new byte[8];

    /**
     * 升级文件的路径
     */
    private String mDevicePath;

    /**
     * DFU状态
     */
    private int mDfuReady;

    private byte[] gCmdBytes;
    private int iCmdIndex = 0;
    private int iCmdLen = 0;

    private final static int PAYLOAD_LEN = 20;

    private boolean bWriteDfuData;

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
     * 下载提示框
     */
    private NotificationManager mNotificationManager;

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mScanning = false;
            mBtAdapter.stopLeScan(mLeScanCallback);
            showDevicScan();
        }
    };

    private CheckVersionAction mVersionAction = null;

    /**
     * 文件名称
     */
    private String mFileName;

    /**
     * 下载线程
     */
    private Thread mThread;

    /**
     * handler处理消息
     */
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
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
                    showNotification();
                    downloadSize = 0;
                    fileSize = 0;
                    mStartBt.setText(R.string.start_update);
                    mPb.setProgress(0);
                    mConnetStatus.setText(R.string.new_dev_version);

                    gCmdBytes = FileUtil.loadFirmware(mDevicePath);
                    mStartBt.setEnabled(true);
                    showMessage(getString(R.string.down_finish));
                    break;
                case DOWNLOAD_ERROR:
                    isSuccess = false;
                    showMessage(getString(R.string.download_error));
                    downloadSize = 0;
                    fileSize = 0;
                    File file = new File(tempPath);
                    file.delete();
                    break;
                case NOT_NETWORK:
                    showMessage(getString(R.string.net_error));
                    isSuccess = false;
                    break;
            }
            super.handleMessage(msg);
        }
    };

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
        mStartBt = findViewById(R.id.version_start);
        mDeviceSnTv = findViewById(R.id.dev_mac);
        mUpdateMsg = findViewById(R.id.update_content);
        mVersionName = findViewById(R.id.version_name);
    }

    private void initEvent() {
        ivBack.setOnClickListener(this);
        mStartBt.setOnClickListener(this);
    }

    private void initDate() {
        mDefaultDev = (DeviceInfo) getIntent().getExtras().getSerializable(BleMsg.KEY_DEFAULT_DEVICE);

        mHandler = new Handler();

        mPb.setProgress(100);

        mDeviceSnTv.setText(mDefaultDev.getDeviceSn());

        LocalBroadcastManager.getInstance(this).registerReceiver(otaReceiver, makeGattUpdateIntentFilter());

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mBleManagerHelper = BleManagerHelper.getInstance(this, true);
        if (mBleManagerHelper.getServiceConnection()) {
            mBleManagerHelper.getBleCardService().sendCmd19((byte) 7);
        }

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        mVersionAction = new CheckVersionAction();
    }

    /**
     * 得到文件的保存路径
     *
     * @return
     * @throws IOException
     */
    private void getPath(int versionCode) {
        try {
            String dir = FileUtil.createDir(this, "device") + File.separator;
            mDevicePath = dir + mFileName + "_" + versionCode;
            tempPath = mDevicePath + ".temp";
            FileUtil.clearFiles(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkDevVersion() {
        mVersionAction.setUrl(ConstantUtil.CHECK_FIRMWARE_VERSION);
        mVersionAction.setDeviceSn(mDefaultDev.getDeviceSn());
        mVersionAction.setExtension(ConstantUtil.BIN_EXTENSION);
        mVersionAction.setTransferPayResponse(tCheckDevResponse);
        mVersionAction.transStart(this);
    }

    AbstractTransaction.TransferPayResponse tCheckDevResponse = new AbstractTransaction.TransferPayResponse() {

        @Override
        public void transFailed(String httpCode, String errorInfo) {
            compareVersion(CheckVersionAction.NO_NEW_VERSION);
        }

        @Override
        public void transComplete() {
            if (HttpCodeHelper.RESPONSE_SUCCESS.equals(mVersionAction.respondData.respCode)) {
                changeView(1);
                VersionModel version = mVersionAction.respondData.model;
                mUpdateMsg.setText(version.msg);
                mDownloadUrl = ConstantUtil.BASE_URL + version.path;
                Log.d(TAG, "mDownloadUrl = " + mDownloadUrl);
                mVersionName.setText(version.versionName);
                mFileName = getString(R.string.app_name) + version.versionName;
                getPath(version.versionCode);
                int len = version.versionName.length();
                int swLen = mDefaultDev.getDeviceSwVersion().length();
                int code = 0;
                if (len >= 5 && swLen >= 5)
                    code = StringUtil.compareVersion(version.versionName, mDefaultDev.getDeviceHwVersion().split("_")[1]);
                if (0 == code) {
                    compareVersion(CheckVersionAction.NO_NEW_VERSION);
                } else {
                    if (version.forceUpdate) {
                        compareVersion(CheckVersionAction.MAST_UPDATE_VERSION);
                    } else {
                        compareVersion(CheckVersionAction.SELECT_VERSION_UPDATE);
                    }
                }

            } else {
                compareVersion(CheckVersionAction.NO_NEW_VERSION);
            }
        }
    };

    private void compareVersion(int type) {
        switch (type) {
            case CheckVersionAction.NO_NEW_VERSION:
                changeView(1);
                mUpdateMsg.setText(R.string.http_version_latest);
                break;
            case CheckVersionAction.SELECT_VERSION_UPDATE:
                toDownload(false);
                break;
            case CheckVersionAction.MAST_UPDATE_VERSION:
                toDownload(true);
                break;
        }
    }

    public void toDownload(boolean mastDownload) {
        if (mastDownload) {
            mStartBt.setEnabled(false);
            mPb.setProgress(0);
            File file = new File(mDevicePath);
            if (file.exists()) {
                sendMessage(DOWNLOAD_OK);
            } else {
                if (mThread == null) {
                    mThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            downloadFile();
                        }
                    });
                }
                mThread.start();
            }
        } else {
            mStartBt.setVisibility(View.VISIBLE);
            mStartBt.setEnabled(true);
            mPb.setProgress(0);
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
            Log.d(TAG, "fileSize = " + fileSize);
            if (fileSize < 1 || is == null) {
                sendMessage(DOWNLOAD_ERROR);
            } else {
                sendMessage(DOWNLOAD_PREPARE);
                Log.d(TAG, "tempPath = " + tempPath);
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
            Log.e(TAG, " e = " + e.getMessage().toString());
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

    private void initLeCallback() {

        mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (device.getAddress().equals(mDefaultDev.getBleMac())) {
                            LogUtil.d(TAG, "dev rssi = " + rssi + device.getName());
                            mHandler.removeCallbacks(mRunnable);
                            mBtAdapter.stopLeScan(mLeScanCallback);
                            LogUtil.d(TAG, "mIsConnected = " + mBleManagerHelper.getServiceConnection());
                            if (!mBleManagerHelper.getServiceConnection() && mBleManagerHelper.getBleCardService() != null) {
                                boolean result = mBleManagerHelper.getBleCardService().connect(mDefaultDev.getBleMac());
                                LogUtil.d(TAG, "result = " + result);

                            }
                        }

                    }
                });
            }
        };
    }

    /**
     * 广播action
     *
     * @returninter
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.STR_RSP_SECURE_CONNECTION_OTA);
        intentFilter.addAction(BleMsg.STR_RSP_SET_TIMEOUT);
        intentFilter.addAction(BleMsg.STR_RSP_OTA_MODE);
        intentFilter.addAction(BleMsg.ACTION_CHARACTERISTIC_WRITE);
        intentFilter.addAction(BleMsg.STR_RSP_MSG1C_VERSION);
        return intentFilter;
    }

    /**
     * 蓝牙广播接收器
     */
    private final BroadcastReceiver otaReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BleMsg.STR_RSP_MSG1C_VERSION)) {
                String sn = StringUtil.asciiDeBytesToCharString(intent.getByteArrayExtra(BleMsg.KEY_NODE_SN));
                String swVer = StringUtil.asciiDeBytesToCharString(intent.getByteArrayExtra(BleMsg.KEY_SW_VER));
                String hwVer = StringUtil.asciiDeBytesToCharString(intent.getByteArrayExtra(BleMsg.KEY_HW_VER));
                LogUtil.d(TAG, "SW VERSION = " + swVer + '\n' +
                        "HW VERSION = " + hwVer + '\n' +
                        "SN = " + sn);
                mDefaultDev.setDeviceSn(sn);
                mDefaultDev.setDeviceSwVersion(swVer);
                mDefaultDev.setDeviceHwVersion(hwVer);
                DeviceInfoDao.getInstance(OtaUpdateActivity.this).updateDeviceInfo(mDefaultDev);
                checkDevVersion();
            }

            if (action.equals(BleMsg.STR_RSP_SECURE_CONNECTION_OTA)) {
                mAK = intent.getByteArrayExtra(BleMsg.KEY_AK);
                mConnetStatus.setText(R.string.connect_ota_success);

                if (mAK != null && mAK.length != 0) {
                    if (mBleManagerHelper.getBleCardService() != null) {
                        mBleManagerHelper.getBleCardService().sendCmdOta(mConnectNodeId, mBleManagerHelper.getAK());
                    }
                } else {
                    mOtaUpdateComplete = true;
                    prepareDFU();
                }
            }

            if (action.equals(BleMsg.STR_RSP_SET_TIMEOUT)) {
                mConnetStatus.setText(R.string.connect_failed);
                mStartBt.setEnabled(true);
                updateDfuReady(Device.DFU_CHAR_DISCONNECTED);
            }

            if (action.equals(BleMsg.STR_RSP_OTA_MODE)) {
                Log.d(TAG, "otaUpdateComplete = " + mOtaUpdateComplete);

                if (!mOtaUpdateComplete) {
                    mConnetStatus.setText(R.string.connect_ota_mode);
                    scanLeDevice(true);
                } else {
                    mBleManagerHelper.setTempMode(false, 0);
                }
            }

            if (BleMsg.ACTION_CHARACTERISTIC_WRITE.equals(action)) {
                if (bWriteDfuData) {
                    mPb.setProgress(iCmdIndex * 100 / iCmdLen); // add 2017.12.22
                    iCmdIndex += PAYLOAD_LEN;
                    Log.d(TAG, "mGattUpdateReceiver!!! " + " prev iCmdIndex=" + iCmdIndex);
                    if (iCmdIndex < iCmdLen)
                        writeCommandByPosition(iCmdIndex);
                    else { // end of writing command;
                        iCmdIndex = iCmdLen;
                        endDFU();
                        mConnetStatus.setText(R.string.dfu_end_waiting);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mConnetStatus.setText(R.string.ota_complete);
                            }
                        }, 6000);
                    }

                }

            }
        }
    };

    private void changeView(int action) {
        switch (action) {
            case 0:
                mCheckVersion.setVisibility(View.VISIBLE);
                mVersionUpdate.setVisibility(View.GONE);
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
            Log.d(TAG, "updateDfuReady! all reset~~~");
        } else {
            if (status == Device.DFU_CHAR_DISCONNECTED) {
                mDfuReady &= ~Device.DFU_CHAR_EXISTS;
            } else if (status == Device.DFU_FW_UNLOADED)
                mDfuReady &= ~Device.DFU_FW_LOADED;
            else
                // update ready status
                mDfuReady |= status;

            // update start button status
            //if (mDfuReady == Device.DFU_READY) {
            //    btnStartDfu.setEnabled(true);
            //}

            // check firmware file valid
            Log.d(TAG, "Start checking fileExists!");
            if (FileUtil.fileExists(mDevicePath)) {
                mDfuReady |= Device.DFU_FW_LOADED;
            } else {
                mDfuReady &= ~Device.DFU_FW_LOADED;
            }

//            mStartBt.setEnabled(mDfuReady == Device.DFU_READY);
            if (mBleManagerHelper.getAK() != null)
                mConnetStatus.setText(R.string.connection_success);
            else
                mConnetStatus.setText(R.string.connect_failed);
        }
        Log.d(TAG, "updateDfuReady--status=" + status);
        mPb.setProgress(0);
    }

    private void prepareDFU() {
        // write start command - 0 to 1580 command handle;
        buildAndSendDFUCommand(0);

        // wait 500ms;
        WriteCommandAction();
    }

    private void WriteCommandAction() {

        if (mBleManagerHelper.getBleCardService() != null) {
            iCmdIndex = 0;
            iCmdLen = gCmdBytes.length;//updateCmdBytes();
            mTvProgress.setText(iCmdLen + " / " + iCmdIndex);

            Log.d(TAG, "WriteCommandAction!!! iCmdLen = " + iCmdLen);
            writeCommandByPosition(iCmdIndex);
        }
    }

    /**/
    private void writeCommandByPosition(int index) {
        Log.d(TAG, "WriteCommandByPosition!!! index = " + index);
        mTvProgress.setText(iCmdLen + " / " + index);
        //if(textCmd != null && !textCmd.equals("")){
        if (gCmdBytes != null) {
            bWriteDfuData = true;

            byte[] cmd;
            if (index + PAYLOAD_LEN < iCmdLen)
                cmd = StringUtil.getSubBytes(gCmdBytes, index, index + PAYLOAD_LEN);
            else
                cmd = StringUtil.getSubBytes(gCmdBytes, index, iCmdLen);
            mBleManagerHelper.getBleCardService().sendCmdOtaData(cmd, Message.TYPE_BLE_SEND_OTA_DATA);
        }
    }

    private void endDFU() {
        // write command data to 1580 data handle;
        buildAndSendDFUCommand(1);
        //mHandler = null;

        mPb.setProgress(100);

        if (mDfuReady == Device.DFU_READY) {
            mStartBt.setEnabled(true);
        }

    }

    private void buildAndSendDFUCommand(int action) {

        Log.d(TAG, "build DFU Cmd...........");
        // count dfu data length;
        iCmdLen = gCmdBytes.length;
        mTvProgress.setText(iCmdLen + " - DFU CMD - " + action);

        byte[] dfuCmd = new byte[16];
        // flag
        dfuCmd[0] = 0x55;
        dfuCmd[1] = (byte) 0xAA;
        dfuCmd[2] = (byte) 0xA5;
        dfuCmd[3] = 0x5A;
        // length
        dfuCmd[4] = (byte) (iCmdLen & 0x000000FF);
        dfuCmd[5] = (byte) ((iCmdLen & 0x0000FF00) >> 8);
        dfuCmd[6] = (byte) ((iCmdLen & 0x00FF0000) >> 16);
        dfuCmd[7] = (byte) ((iCmdLen & 0xFF000000) >> 32);
        // reserved
        dfuCmd[8] = 0x0;
        dfuCmd[9] = 0x0;
        dfuCmd[10] = 0x0;
        dfuCmd[11] = 0x0;
        // action
        dfuCmd[12] = (byte) action;
        dfuCmd[13] = 0x0;
        dfuCmd[14] = 0x0;
        dfuCmd[15] = 0x0;
        mConnetStatus.setText(R.string.ota_updating);
        mBleManagerHelper.getBleCardService().sendCmdOtaData(dfuCmd, Message.TYPE_BLE_SEND_OTA_CMD);
    }

    @Override
    public void onResume() {
        super.onResume();
        initLeCallback();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(mRunnable, SCAN_PERIOD);

            mStartBt.setEnabled(false);
            mScanning = true;
            mBtAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBtAdapter.stopLeScan(mLeScanCallback);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        mBleManagerHelper.setTempMode(false);

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(otaReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
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

    private void showDevicScan() {
        if (mScanning == false) {
            mStartBt.setEnabled(true);
            mConnetStatus.setText(getString(R.string.search_failed));
        } else finish();
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back_sysset:
                finish();
                break;
            case R.id.version_start:
                if (mStartBt.getText().toString().trim().equals(getString(R.string.download_version))) {
                    toDownload(true);
                } else {
                    mConnetStatus.setText(R.string.start_update);
                    if (mBleManagerHelper.getServiceConnection()) {
                        mBleManagerHelper.getBleCardService().sendCmd19((byte) 9);
                        mConnetStatus.setText(R.string.connect_ota_mode);
                    } else {
                        mBleManagerHelper.startScanDevice();
                    }
                }
                mStartBt.setEnabled(false);
                break;
            default:
                break;
        }
    }

    private void showNotification() {
        String fixMessage = getString(R.string.fix_alert);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.down_alert))
                .setContentText(fixMessage)
                .setSmallIcon(R.mipmap.icon)
                .build();
        notification.defaults |= Notification.DEFAULT_SOUND; // 声音通知
        notification.defaults |= Notification.DEFAULT_VIBRATE; // 颤动通知
        notification.icon = R.mipmap.icon;
        notification.tickerText = getString(R.string.app_name) + getString(R.string.down_finish);
        notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_NO_CLEAR; // 不可清除的通知

        mNotificationManager.notify(R.string.app_name, notification);
    }
}
