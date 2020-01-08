package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.action.CheckOtaAction;
import com.smart.lock.action.CheckVersionAction;
import com.smart.lock.ble.AutoConnectBle;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.parser.PacketParser;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.entity.Device;
import com.smart.lock.entity.VersionModel;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.FileUtil;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SendOTAData;
import com.smart.lock.utils.StringUtil;
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

public class FaceOtaUpdateActivity extends Activity implements View.OnClickListener, UiListener, SendOTAData.OnSendingListener {

    private static final String TAG = FaceOtaUpdateActivity.class.getSimpleName();
    /**
     * 返回控件
     */
    private ImageView ivBack;

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
    private String mNCPUDownloadUrl = ConstantUtil.BASE_URL;
    /**
     * 版本下载连接
     */
    private String mSCPUDownloadUrl = ConstantUtil.BASE_URL;

    private String tempSCpuPath;
    private String tempNCpuPath;
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
    private TextView mConnStatus;

    /**
     * 升级文件的路径
     */
    private String mNCPUPath;
    /**
     * 升级文件的路径
     */
    private String mSCPUPath;

    //back time
    private long mBackPressedTime;
    /**
     * DFU状态
     */
    private int mDfuReady;

    private byte[] gCmdBytes;

    private final static int PAYLOAD_LEN = 20;

    private boolean bWriteDfuData = false; //数据传输阶段
    private boolean mOtaMode = false; //是否进入OTA模式
    private boolean mIsUpdateSuccess = false;

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

    private CheckOtaAction mVersionAction = null;

    /**
     * 文件名称
     */
    private String mNCPUFileName;
    /**
     * 文件名称
     */
    private String mSCPUFileName;

    /**
     * 下载线程
     */
    private Thread mThread;

    private Device mDevice;

    private SendOTAData mSendOTAData;

    private static final int TAG_OTA_PREPARE = 0;
    private static final int TAG_OTA_START = 1;
    private static final int TAG_OTA_END = 2;
    private VersionModel mVersionModel;

    private final PacketParser mOtaParser = new PacketParser();
    private byte[] mSha1;

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
//                    showNotification();
                    downloadSize = 0;
                    fileSize = 0;
                    mStartBt.setText(R.string.start_update);
                    mPb.setProgress(0);
                    mConnStatus.setText(R.string.new_dev_version);

                    mStartBt.setEnabled(true);
                    showMessage(getString(R.string.down_finish));
                    break;
                case DOWNLOAD_ERROR:
                    isSuccess = false;
                    showMessage(getString(R.string.download_error));
                    downloadSize = 0;
                    fileSize = 0;
                    File file = new File(tempSCpuPath);
                    file.delete();
                    file = new File(tempNCpuPath);
                    file.delete();
                    compareVersion(CheckVersionAction.SELECT_VERSION_UPDATE);
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
        setContentView(R.layout.ota_update_fp);
        initView();
        initEvent();
        initDate();
//        changeView(1);
    }


    private void initView() {
        ((TextView) (findViewById(R.id.tv_message_title))).setText(R.string.face_ota_update);
        ivBack = findViewById(R.id.iv_back_sysset);
        mStartBt = findViewById(R.id.version_start);
        mConnStatus = findViewById(R.id.connect_status);
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
        mPb.setProgress(100);

        mBleManagerHelper = BleManagerHelper.getInstance(this);
        mBleManagerHelper.addUiListener(this);
        mDevice = Device.getInstance(this);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        mVersionModel = (VersionModel) getIntent().getSerializableExtra(ConstantUtil.SERIALIZABLE_FACE_VERSION_MODEL);

        if (mSendOTAData == null) {
            mSendOTAData = new SendOTAData(this, mBleManagerHelper, Message.TYPE_BLE_FP_SEND_OTA_DATA);
            mBleManagerHelper.getBleCardService().registerDevStateCb(mSendOTAData);
        }

        if (mVersionModel == null) {
            showMessage("没有可更新的文件");
            finish();
        } else {
            mNCPUFileName = mVersionModel.nCpuFilename;
            mSCPUFileName = mVersionModel.sCpuFilename;
            getPath();
            if (mVersionModel.sCpuPath != null) {
                mSCPUDownloadUrl += mVersionModel.sCpuPath;
            }
            if (mVersionModel.nCpuPath != null) {
                mNCPUDownloadUrl += mVersionModel.nCpuPath;
            }
            mUpdateMsg.setText(mVersionModel.msg);


            String[] curVerArray = mDefaultDev.getFaceMainVersion().split("_");
            String[] devVerVerArray = mDefaultDev.getFaceMainVersion().split("_");

            LogUtil.d(TAG, "curVer =" + mDefaultDev.getFaceMainVersion() + '\n' + "dev=" + mDefaultDev.getFaceMainVersion());

            String surVer = curVerArray[1].trim();
            String devVer = devVerVerArray[1].trim();

            LogUtil.d(TAG, "surVer " + surVer + " devVer : " + devVer);

            mCurrentVersion.setText("v" + surVer);
            mLatestVersion.setText("v" + devVer);
            mDeviceSnTv.setText(mDefaultDev.getDeviceSn());

//            int code = StringUtil.compareFPVersion(mDefaultDev.getFpSwVersion(), mVersionModel.versionName);
//            if (SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.IS_DMT_TEST)) {
//                compareVersion(CheckVersionAction.SELECT_VERSION_UPDATE);
//            } else {
//                if (0 == code || code == -1) {
//                    compareVersion(CheckVersionAction.NO_NEW_VERSION);
//                } else {
//                    if (mVersionModel.forceUpdate) {
//                        compareVersion(CheckVersionAction.MAST_UPDATE_VERSION);
//                    } else {
//                        compareVersion(CheckVersionAction.SELECT_VERSION_UPDATE);
//                    }
//                }
//            }
            compareVersion(CheckVersionAction.SELECT_VERSION_UPDATE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            AutoConnectBle autoConnectBle = AutoConnectBle.getInstance(this);
            autoConnectBle.setAutoConnect(true);
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
            mNCPUPath = dir + mNCPUFileName;
            mSCPUPath = dir + mSCPUFileName;

            tempSCpuPath = mSCPUPath + ".temp";
            tempNCpuPath = mNCPUPath + ".temp";
            FileUtil.clearFiles(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void compareVersion(int type) {
        LogUtil.d(TAG, "type : " + type);
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
        File file = new File(mSCPUPath);
        File file1 = new File(mNCPUPath);
        if (file.exists() || file1.exists()) {
            downloadSize = 0;
            fileSize = 0;
            mStartBt.setText(R.string.start_update);
            mPb.setProgress(0);
            mConnStatus.setText(R.string.check_sd_has_new_version);
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
                try {
                    mThread.start();
                } catch (IllegalThreadStateException e) {
                    mThread.interrupt();
                    mThread.start();
                }

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
        LogUtil.e(TAG, "下载 URL= mS" + mSCPUDownloadUrl + "\n" + "ncpu=" + mNCPUDownloadUrl);
        for (String url : Arrays.asList(mSCPUDownloadUrl, mNCPUDownloadUrl)) {
            try {
                if (url.equals(ConstantUtil.BASE_URL))
                    continue;
                LogUtil.e(TAG, "下载 URL= " + url);
                URL u = new URL(url);
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
                    File downFile;
                    downloadSize = 0;
                    if (url.equals(mSCPUDownloadUrl)) {
                        downFile = new File(tempSCpuPath);

                    } else if (url.equals(mNCPUDownloadUrl)) {
                        downFile = new File(tempNCpuPath);

                    } else {
                        break;
                    }
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
        if (isSuccess) {
            copyFile();
            sendMessage(DOWNLOAD_OK);
        }
    }

    private void copyFile() {

        for (String path : Arrays.asList(tempSCpuPath, tempNCpuPath)) {
            if (path == null)
                continue;
            File tempFile = new File(path);
            if (!tempFile.exists()) {
                continue;
            }
            File newFile;
            if (path.equals(tempSCpuPath)) {
                newFile = new File(mSCPUPath);
            } else if (path.equals(tempNCpuPath)) {
                newFile = new File(mNCPUPath);
            } else {
                continue;
            }
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
                mVersionUpdate.setVisibility(View.VISIBLE);
                mConnStatus.setText(R.string.ready_download);
                break;
            case 1:
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
//            mProgressCheck.setProgress(0);
        } else {
            if (status == Device.DFU_CHAR_DISCONNECTED) {
                mDfuReady &= ~Device.DFU_CHAR_EXISTS;
            } else if (status == Device.DFU_FW_UNLOADED)
                mDfuReady &= ~Device.DFU_FW_LOADED;
            else
                // update ready status
                mDfuReady |= status;

            if (FileUtil.fileExists(mSCPUPath) || FileUtil.fileExists(mSCPUPath)) {
                mDfuReady |= Device.DFU_FW_LOADED;
            } else {
                mDfuReady &= ~Device.DFU_FW_LOADED;
            }

            if (mDevice.getState() == Device.BLE_CONNECTED)
                mConnStatus.setText(R.string.connection_success);
            else
                mConnStatus.setText(R.string.connect_failed);
        }
        mPb.setProgress(0);
    }

    private void prepareDFU(int model) {
        setDataSize(model);
        buildAndSendDFUCommand(TAG_OTA_PREPARE);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // wait 500ms;
        writeCommandAction();
    }

    private void setDataSize(int model) {
        // write start command - 0 to 1580 command handle;

        if (model == BleMsg.TYPE_FACE_OTA_SCPU) {
            gCmdBytes = FileUtil.loadFirmware(mSCPUPath);
            mSha1 = StringUtil.hexStringToBytes(mVersionModel.sCpuSHA1);
        } else if (model == BleMsg.TYPE_FACE_OTA_NCPU) {
            gCmdBytes = FileUtil.loadFirmware(mNCPUPath);
            mSha1 = StringUtil.hexStringToBytes(mVersionModel.nCpuSHA1);
        }
        if (mSha1.length < 64) {
            LogUtil.d(TAG, "SHA1=null");
            mOtaParser.set(gCmdBytes);
        } else {
            LogUtil.d(TAG, Arrays.toString(mSha1));
            mOtaParser.set(gCmdBytes, mSha1);
        }
    }

    private void writeCommandAction() {
        buildAndSendDFUCommand(TAG_OTA_START);

        if (mBleManagerHelper.getBleCardService() != null) {
            mPb.setProgress(mOtaParser.getProgress());
            mTvProgress.setText(mOtaParser.getProgress() + "%");
            writeCommandByPosition();
        }

    }

    /**
     * 发送包
     */
    private void writeCommandByPosition() {
        mTvProgress.setText(mOtaParser.getProgress() + "%");
        if (gCmdBytes != null) {
            byte[] cmd = mOtaParser.getNextPacket(BleMsg.CMD_OTA_DATA_43);

//            mBleManagerHelper.getBleCardService().sendCmdOtaData(cmd, Message.TYPE_BLE_FP_SEND_OTA_DATA);
            LogUtil.d(TAG, "cmd = " + Arrays.toString(cmd));
            mSendOTAData.start(cmd);
        }
    }

    private void endDFU() {
        // write command data to 1580 data handle;
        buildAndSendDFUCommand(TAG_OTA_END);
        //mHandler = null;
        mPb.setProgress(100);
        if (mDfuReady == Device.DFU_READY) {
            mStartBt.setEnabled(true);
        }
//        mSendOTAData.setStateToDone();
//        mBleManagerHelper.getBleCardService().removeDevStateCb(mSendOTAData);
    }

    @Override
    public void onSending() {
        if (bWriteDfuData) {
            if (mOtaParser.hasNextPacket()) {
                mPb.setProgress(mOtaParser.getProgress());
                mConnStatus.setText(getString(R.string.ota_updating));
                writeCommandByPosition();
            } else if (mOtaParser.isLast()) { // end of writing command;
                mOtaMode = false;
                mPb.setProgress(100);
            }

        }
    }

    private void buildAndSendDFUCommand(int action) {
        mTvProgress.setText(mOtaParser.getProgress() + "%");
        switch (action) {
            case TAG_OTA_PREPARE:
                mConnStatus.setText(R.string.checking_version);
                break;
            case TAG_OTA_START:
                mConnStatus.setText(R.string.start_update);
                bWriteDfuData = true;
                break;
            case TAG_OTA_END:
                mConnStatus.setText(R.string.dfu_end_waiting);
                mTvProgress.setText(mOtaParser.getProgress() + "%");
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
        AutoConnectBle autoConnectBle = AutoConnectBle.getInstance(this);
        autoConnectBle.setAutoConnect(false);
        if (mBleManagerHelper != null) {
            mBleManagerHelper.removeUiListener(this);
            if (mBleManagerHelper.getBleCardService() != null)
                mBleManagerHelper.getBleCardService().removeDevStateCb(mSendOTAData);
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
                    ToastUtil.showShort(this, getString(R.string.ota_back_message));
                } else {
                    setResult(CheckOtaActivity.CHECK_FP_VERSION);
                    finish();
                }
                break;
            case R.id.version_start:
                if (mStartBt.getText().toString().trim().equals(getString(R.string.download_version))) {
                    toDownload(true);
                } else {
                    mStartBt.setEnabled(false);
                    connectOTA();
                }
                break;
            default:
                break;
        }
    }

    private void connectOTA() {
        LogUtil.e(TAG, "链接OTA模式");
        mConnStatus.setText(R.string.connect_ota_mode);
        if (Device.getInstance(this).getState() == Device.BLE_CONNECTED && mBleManagerHelper.getBleCardService() != null) {
            if (mVersionModel.sCpuPath != null) {
//                if (false) {
                mBleManagerHelper.getBleCardService().sendCmd41(BleMsg.CMD_JOIN_FACE_OTA,
                        BleMsg.TYPE_FACE_SCPU_OTA,
                        0,
                        BleMsg.INT_DEFAULT_TIMEOUT);
            } else if (mVersionModel.nCpuPath != null) {
                mBleManagerHelper.getBleCardService().sendCmd41(BleMsg.CMD_JOIN_FACE_OTA,
                        BleMsg.TYPE_FACE_NCPU_OTA,
                        0,
                        BleMsg.INT_DEFAULT_TIMEOUT);
            }
        } else {
            showMessage(getString(R.string.plz_reconnect));
        }
    }

    private void updateVersion() {
        LogUtil.e(TAG, "开始更新");
        mStartBt.setEnabled(false);
        mConnStatus.setText(R.string.start_update);
        if (mVersionModel.sCpuPath != null) {
            updateData(BleMsg.TYPE_FACE_OTA_SCPU);
        } else {
            updateData(BleMsg.TYPE_FACE_OTA_NCPU);
        }
    }

    private void updateData(byte model) {
        setDataSize(model);
        if (Device.getInstance(this).getState() == Device.BLE_CONNECTED && mBleManagerHelper.getBleCardService() != null) {
            if (model == BleMsg.TYPE_FACE_OTA_SCPU) {
                LogUtil.e(TAG, "CMD = " + BleMsg.CMD_START_FACE_OTA + "\nTYPE = " + BleMsg.TYPE_FACE_SCPU_OTA);
                mBleManagerHelper.getBleCardService().sendCmd41(BleMsg.CMD_START_FACE_OTA,
                        BleMsg.TYPE_FACE_SCPU_OTA,
                        0,
                        BleMsg.INT_DEFAULT_TIMEOUT);
            } else if (model == BleMsg.TYPE_FACE_OTA_NCPU) {
                LogUtil.e(TAG, "CMD = " + BleMsg.CMD_START_FACE_OTA + "\nTYPE = " + BleMsg.TYPE_FACE_SCPU_OTA);
                mBleManagerHelper.getBleCardService().sendCmd41(BleMsg.CMD_START_FACE_OTA,
                        BleMsg.TYPE_FACE_NCPU_OTA,
                        0,
                        BleMsg.INT_DEFAULT_TIMEOUT);
            }

        } else {
            showMessage(getString(R.string.plz_reconnect));
        }
    }

    @Override
    public void deviceStateChange(Device device, int state) {
        mDevice = device;
        switch (state) {
            case BluetoothGatt.GATT_SUCCESS:
                break;
            case BleMsg.STATE_DISCONNECTED:
                if (mOtaParser.hasNextPacket() && !mIsUpdateSuccess) {
                    bWriteDfuData = false;
                    mConnStatus.setText(R.string.ota_file_dan);
                    mOtaMode = false; //异常断开，重置ota模式
                } else if (!mOtaMode) {
                    mConnStatus.setText(R.string.dfu_end_waiting);
                } else if (mOtaParser.getTotal() == 0) {
                    mConnStatus.setText(R.string.ota_connection);
                } else {
                    mStartBt.setEnabled(false);
                    mConnStatus.setText(R.string.disconnect_ble);
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
        LogUtil.d(TAG, "rsp = " + msg.getType());
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
                final byte[] errCode = extra.getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode != null)
                    dispatchErrorCode(errCode[3]);

                break;
            case Message.TYPE_BLE_RECEIVER_CMD_04:
                LogUtil.i(TAG, "receiver 04!" + mOtaMode);
                if (!mOtaMode && !bWriteDfuData && !mOtaParser.hasNextPacket() && mIsUpdateSuccess) {
                    String dir = FileUtil.createDir(this, ConstantUtil.DEV_DIR_NAME) + File.separator;
                    FileUtil.clearFiles(dir);
                    mConnStatus.setText(R.string.ota_complete);
                    mOtaMode = false;//异常断开，重置ota模式
                } else if (mOtaMode) {
                    LogUtil.e(TAG, "");
                    updateVersion();
                } else {
                    downloadSize = 0;
                    fileSize = 0;
                    mStartBt.setText(R.string.start_update);
                    mPb.setProgress(0);
                    mOtaParser.clear();
                    mConnStatus.setText(R.string.new_dev_version);
                    mTvProgress.setText("0" + "%");
                    mStartBt.setEnabled(true);
                }
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_42:
                byte repCode = extra.getByte(BleMsg.KEY_KDP_RSP_CODE);
                byte moduleType = extra.getByte(BleMsg.KEY_FACE_OTA_MODULE);
                LogUtil.e(TAG, "CMD 42 respCode = " + repCode + "\nModule type =" + moduleType);
                switch (repCode) {
                    // 0x01
                    case BleMsg.RSP_FACE_OTA_SUCCESS:
                        mIsUpdateSuccess = true;
                        break;
                    //0x02
                    case BleMsg.RSP_FACE_OTA_FAILED:
                        mIsUpdateSuccess = false;
                        bWriteDfuData = false;
                        mOtaMode = false;
                        mConnStatus.setText(R.string.check_fp_secret_error);
                        break;
                    //0x03 0x05
                    case BleMsg.RSP_FACE_OTA_REFUSE:
                    case BleMsg.RSP_FACE_OTA_BUSY:
                        mConnStatus.setText(R.string.device_busy);
                        mStartBt.setEnabled(true);
                        break;
                    //0x04 0x06
                    case BleMsg.RSP_FACE_OTA_ALLOW:
                    case BleMsg.RSP_FACE_OTA_GET_SIZE:
                    case BleMsg.RSP_FACE_OTA_FILE_DONE:
                        doOtherAction(repCode, moduleType);
                        break;
                    default:
                        break;
                }
                break;
            case BleMsg.TYPE_NO_AUTHORITY_1E:
                mOtaMode = true;
                updateVersion();
                break;
            default:
                break;

        }
    }

    private void doOtherAction(byte repCode, byte moduleType) {
        switch (moduleType) {
            // 0x02
            case BleMsg.TYPE_FACE_OTA_NCPU:
                //0x06
                switch (repCode) {
                    case BleMsg.RSP_FACE_OTA_GET_SIZE://0x06
                        prepareDFU(BleMsg.TYPE_FACE_OTA_NCPU);
                        mStartBt.setEnabled(false);
                        break;
                    case BleMsg.RSP_FACE_OTA_ALLOW://0x04
                        sendSize(BleMsg.TYPE_FACE_OTA_NCPU);
                        break;
                    case BleMsg.RSP_FACE_OTA_FILE_DONE://0x07
                        mBleManagerHelper.getBleCardService().sendCmd41(BleMsg.CMD_FACE_OTA_DONE,
                                BleMsg.TYPE_FACE_SCPU_OTA,
                                0,
                                BleMsg.INT_DEFAULT_TIMEOUT);
                        endDFU();

                        break;
                }
                break;
            //0x01
            case BleMsg.TYPE_FACE_OTA_SCPU:
                switch (repCode) {
                    case BleMsg.RSP_FACE_OTA_GET_SIZE://0x06
                        prepareDFU(BleMsg.TYPE_FACE_OTA_SCPU);
                        mStartBt.setEnabled(false);
                        break;
                    case BleMsg.RSP_FACE_OTA_ALLOW://0x04
                        sendSize(BleMsg.TYPE_FACE_OTA_SCPU);
                        break;
                    case BleMsg.RSP_FACE_OTA_FILE_DONE://0x07
                        LogUtil.d(TAG, "发送SCPU完成");
                        if (mVersionModel.nCpuPath != null) {
                            LogUtil.d(TAG, "发送NCPU大小");
                            mOtaMode = true;
                            sendSize(BleMsg.TYPE_FACE_OTA_NCPU);
                        } else {
                            LogUtil.d(TAG, "更新结束");
                            mBleManagerHelper.getBleCardService().sendCmd41(BleMsg.CMD_FACE_OTA_DONE,
                                    BleMsg.TYPE_FACE_SCPU_OTA,
                                    0,
                                    BleMsg.INT_DEFAULT_TIMEOUT);
                            endDFU();
                        }
                        break;
                }
                break;
            // 0x03
            case BleMsg.TYPE_FACE_OTA_MODULE:
                break;

        }
    }

    private void sendSize(byte model) {
        if (mOtaMode) {
            setDataSize(model);
            LogUtil.e(TAG, "传输大小");
            if (mDevice.getState() == Device.BLE_CONNECTED && mBleManagerHelper.getBleCardService() != null) {
                if (model == BleMsg.TYPE_FACE_OTA_SCPU) {
                    mBleManagerHelper.getBleCardService().sendCmd41(BleMsg.CMD_FACE_FIRMWARE_SIZE,
                            BleMsg.TYPE_FACE_SCPU_OTA,
                            (gCmdBytes.length),
                            BleMsg.INT_DEFAULT_TIMEOUT);

                } else if (model == BleMsg.TYPE_FACE_OTA_NCPU) {
                    mBleManagerHelper.getBleCardService().sendCmd41(BleMsg.CMD_FACE_FIRMWARE_SIZE,
                            BleMsg.TYPE_FACE_NCPU_OTA,
                            (gCmdBytes.length),
                            BleMsg.INT_DEFAULT_TIMEOUT);
                }
                mStartBt.setEnabled(false);
            } else {
                showMessage(getString(R.string.plz_reconnect));
            }
        } else {
            LogUtil.i(TAG, "mOtaMode : " + mOtaMode);
            mOtaMode = true;
        }
    }

    private void dispatchErrorCode(byte errCode) {
        LogUtil.i(TAG, "errCode : " + errCode);
        switch (errCode) {
            case BleMsg.TYPE_REFUSE_FINGERPRINT_OTA_UPDATE:
                mConnStatus.setText(R.string.device_busy);
                mStartBt.setEnabled(true);
                break;
            case BleMsg.TYPE_FINGERPRINT_OTA_UPDATE_SUCCESS:
                mIsUpdateSuccess = true;
                break;
            case BleMsg.TYPE_FINGERPRINT_OTA_UPDATE_FAILED:
                bWriteDfuData = false;
                mOtaMode = false;
                mIsUpdateSuccess = false;
                mConnStatus.setText(R.string.check_fp_secret_error);
                break;
            case BleMsg.TYPE_OPEN_SLIDE:
                if (mBleManagerHelper.getBleCardService() != null)
                    mBleManagerHelper.getBleCardService().cancelCmd(Message.TYPE_BLE_SEND_CMD_19 + "#" + "single");
                showMessage(getString(R.string.plz_open_slide));
                mOtaMode = false;
                break;
            case BleMsg.TYPE_NO_AUTHORITY_1E:
                mOtaMode = true;
                updateVersion();
                break;
            default:
                break;
        }
    }

    @Override
    public void reConnectBle(Device device) {
        LogUtil.d(TAG, "reconnect!");
        mConnStatus.setText(R.string.bt_connecting);
    }

    @Override
    public void sendFailed(Message msg) {

    }

    @Override
    public void addUserSuccess(Device device) {

    }

    @Override
    public void scanDevFailed() {
        mConnStatus.setText(R.string.connect_failed);
        mStartBt.setEnabled(false);
        updateDfuReady(Device.DFU_CHAR_DISCONNECTED);
    }

    @Override
    public void onBackPressed() {
        if (mDevice != null && mDevice.getState() == Device.BLE_CONNECTED && mOtaParser.hasNextPacket()) {
            ToastUtil.showShort(this, getString(R.string.ota_back_message));
        } else {
            setResult(CheckOtaActivity.CHECK_FP_VERSION);
            finish();
        }
    }
}
