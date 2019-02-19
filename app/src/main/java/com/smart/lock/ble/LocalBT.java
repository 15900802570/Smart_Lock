package com.smart.lock.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.UUID;


/**********************
 * 
 * @author baofu
 *
 * @Usage: 
 * 
 * first:
 * private LocalBT mLocalBt = LocalBT.getInstance(getActivity());
 * 
 * then:
 * BluetoothAdapter mBluetoothAdapter = mLocalBt.getBtAdapter();
 * 
 */


public final class LocalBT {

    private static final String TAG = "LocalBT";
    
    private static LocalBT sInstance = null;
    
    private BluetoothAdapter mBtAdapter = null;
    //private Activity mThisActivity;
    private Context mContext;
    
    private BluetoothGattServer mBtGattServer = null;
    
    private static final int REQUEST_ENABLE_BT = 1;
    
    public LocalBT(Context context) {
        // TODO Auto-generated constructor stub
        Log.d(TAG, "LocalBT ... context =  " + context);
        
        //mThisActivity = thisActivity;
        mContext = context;
        getBtAdapter();
    }
    
//    public void setActivity(Activity thisActivity) {
//        mThisActivity = thisActivity;
//    }
    
    public static synchronized LocalBT getInstance(Context context) {
        Log.d(TAG, "context = " + context);
        Log.d(TAG, "getInstance = " + sInstance);
        if (sInstance == null) {
            sInstance = new LocalBT(context);
            Log.d(TAG, "sInstance =" + sInstance);
        }
            
        return sInstance;
    }
    
    public BluetoothAdapter getBtAdapter() {
        Log.d(TAG, "Build.VERSION.SDK_INT = " + Build.VERSION.SDK_INT);
        Log.d(TAG, "Build.VERSION_CODES.JELLY_BEAN_MR1 = " + Build.VERSION_CODES.JELLY_BEAN_MR1);
        if (mBtAdapter == null) {
            if (Build.VERSION.SDK_INT <=
                    Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    // Use static method for Android 4.2 and below to get the BluetoothAdapter.
                mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            } else if (mContext != null){
                    // Use BluetoothManager to get the BluetoothAdapter for Android 4.3 and above.
                    BluetoothManager bluetoothManager =
                            (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
                    mBtAdapter = bluetoothManager.getAdapter();
            }
        }
        return mBtAdapter;
    }
    
    public String getAndroidVersion() {
        Log.d(TAG, "Build.VERSION.RELEASE = " + Build.VERSION.RELEASE);
        return Build.VERSION.RELEASE;
    }
    
    public int getSDKLevel() {
        return Build.VERSION.SDK_INT;
    }
    
    public boolean isBleSupported() {
        if (mContext == null) {
            Log.d(TAG, "mContext is null, not initialized!");
            return false;
        }
        
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//          Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
//          finish();
            Log.d(TAG, "mContext hasSystemFeature return false!!!");
          return false;
        }
        else {
            Log.d(TAG, "mContext hasSystemFeature return true!!!");
          return true;
        }
    }
    
    public boolean isBtEnabled() {
        if (mBtAdapter != null) {
            return mBtAdapter.isEnabled();
        } else {
            Log.d(TAG, "mBtAdapter is null, plz initializ it first!");
            return false;
        }
    }
    
    public boolean setBtEnable(boolean bEnable) {
        if (mBtAdapter != null) {
            if (bEnable) {
                mBtAdapter.enable();
                //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            else
                mBtAdapter.disable();
        } else {
            Log.d(TAG, "mBtAdapter is null, plz initializ it first!");
            return false;
        }
        return true;
    }

    public boolean buildGattServer(Context context, BluetoothGattServerCallback callback){
        
        // Use BluetoothManager only for Android 4.3 and above.
        BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);

        mBtGattServer = bluetoothManager.openGattServer(context, callback);
        
        //mBtGattServer.listen();
        // set local device name
        // ...
        
        // add local services
        BluetoothGattService btGattService =
                new BluetoothGattService(UUID.randomUUID(), BluetoothGattService.SERVICE_TYPE_PRIMARY);

        if (mBtGattServer != null){
            mBtGattServer.addService(btGattService);
        }

        
        return false;
        
    }
    
    public boolean destroyGattServer(){
        
        if (mBtGattServer != null){
            mBtGattServer.close();
            mBtGattServer = null;
        }
        
        return true;
    }

    public int getBtState() {
        // Currently only 4 values are defined by android;
        //
        // STATE_OFF = 10;
        // STATE_TURNING_ON = 11;
        // STATE_ON = 12;
        // STATE_TURNING_OFF = 13;
        //
        // STATE_ERROR = 0; defined by ourselves;
        if (mBtAdapter != null) {
            return mBtAdapter.getState();
        }

        return 0;
    }

    public boolean startLeScan(BluetoothAdapter.LeScanCallback callback) {
        if (mBtAdapter != null && isBleSupported()) {
            Log.d(TAG, "startLeScan ... callback="+callback);
            return mBtAdapter.startLeScan(callback);
        }
        return false;
    }

    public boolean stopLeScan(BluetoothAdapter.LeScanCallback callback) {
        if (mBtAdapter != null && callback!=null && isBleSupported()) {
            Log.d(TAG, "stopLeScan ... callback="+callback);
            mBtAdapter.stopLeScan(callback);
            return true;
        }
        return false;
    }

    public String getBtAddress() {
        return mBtAdapter.getAddress();
    }
/*
    public boolean setMTU(int mtu) {
        Log.d(TAG, "setMTU = " + mtu);
        if(mtu > 20) {
            boolean ret = mBluetoothGatt.requestMtu(mtu);
            return ret;
        }
        return false;
    }
    */
}

