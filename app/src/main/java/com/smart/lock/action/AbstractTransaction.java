
package com.smart.lock.action;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.smart.lock.transfer.HttpsClient;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 交易公共父类
 */
@SuppressLint("HandlerLeak")
public abstract class AbstractTransaction {

    // 交易类型
    public enum TransType {
        POST, GET, DELETE, PUT
    }


    public static final String TAG = "AbstractTransaction";

    public boolean READ_INPUT_STREAM = true;

    public static final String ENCODE_UTF8 = "UTF-8";
    private static final String msg_bundle_key_1 = "key1";
    protected static String responseCode = "-1";

    /**
     * 连接后台的URL地址
     */
    public String url = "";

    // 交易类型
    protected int transType = 0;
    protected String transeContent;
    protected Map<String, Object> mapTranseContent;
    private static final int msg_server_response = 1;
    protected TransferPayResponse transferPayResponse;

    Timer timer = null;

    public void setReadXml(boolean read) {
        READ_INPUT_STREAM = read;
    }

    private Context mContext;

    class DelayTask extends TimerTask {
        @Override
        public void run() {
            HttpsClient.closeConnect(mContext);
        }
    }


    /**
     * 交易开始
     */
    public void transArrayStart(final Context context) {
        mContext = context;
        transeContent = transPackage();
        if (timer != null) {
            timer.purge();
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new DelayTask(), 4 * 60 * 1000);
        LogUtil.i("request xml", transeContent);
        new Thread(new Runnable() {
            public void run() {
                Message msg = Message.obtain();
                msg.what = msg_server_response;
                Bundle bundle = new Bundle();
                InputStream inputStream = HttpsClient.post(url, transeContent, context);
                responseCode = HttpsClient.getResponseCode();
                if (inputStream != null) {
                    LogUtil.i(TAG, "inputstream is not null ");
                    if (transParse(inputStream, TransType.POST)) {
                        bundle.putBoolean(msg_bundle_key_1, true);
                    } else {
                        bundle.putBoolean(msg_bundle_key_1, false);
                    }
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtil.i(TAG, "inputstream is null ");
                    bundle.putBoolean(msg_bundle_key_1, false);
                }
                if (timer != null) {
                    timer.purge();
                    timer.cancel();
                    timer = null;
                }
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            }
        }).start();
    }

    /**
     * PUT交易开始
     */
    public void transPutStart(final Context context) {
        mContext = context;
        transeContent = transPackage();
        if (timer != null) {
            timer.purge();
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new DelayTask(), 4 * 60 * 1000);
        LogUtil.i("request xml", transeContent);
        new Thread(new Runnable() {
            public void run() {
                Message msg = Message.obtain();
                msg.what = msg_server_response;
                Bundle bundle = new Bundle();
                InputStream inputStream = HttpsClient.put(url, transeContent, context);
                responseCode = HttpsClient.getResponseCode();
                LogUtil.d(TAG, "responseCode = " + responseCode);
                if (inputStream != null) {
                    LogUtil.i(TAG, "inputstream is not null ");
                    if (transParse(inputStream, TransType.PUT)) {
                        bundle.putBoolean(msg_bundle_key_1, true);
                    } else {
                        bundle.putBoolean(msg_bundle_key_1, false);
                    }
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtil.i(TAG, "inputstream is null ");
                    bundle.putBoolean(msg_bundle_key_1, false);
                }
                if (timer != null) {
                    timer.purge();
                    timer.cancel();
                    timer = null;
                }
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            }
        }).start();
    }


    /**
     * 交易开始
     */
    public void transStart(final Context context) {
        mContext = context;
        mapTranseContent = JsonTransPackage();
        if (timer != null) {
            timer.purge();
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new DelayTask(), 4 * 60 * 1000);
        LogUtil.i("request xml", mapTranseContent.size() + "...." + mapTranseContent.toString());
        new Thread(new Runnable() {
            public void run() {
                Message msg = Message.obtain();
                msg.what = msg_server_response;
                Bundle bundle = new Bundle();
                InputStream inputStream = HttpsClient.post(url, mapTranseContent, context);
                responseCode = HttpsClient.getResponseCode();
                LogUtil.d(TAG, "responseCode = " + responseCode);
                if (inputStream != null) {
                    LogUtil.i(TAG, "inputstream is not null ");
                    if (transParse(inputStream, TransType.POST)) {
                        bundle.putBoolean(msg_bundle_key_1, true);
                    } else {
                        bundle.putBoolean(msg_bundle_key_1, false);
                    }
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtil.i(TAG, "inputstream is null ");
                    bundle.putBoolean(msg_bundle_key_1, false);
                }
                if (timer != null) {
                    timer.purge();
                    timer.cancel();
                    timer = null;
                }
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            }
        }).start();
    }

    /**
     * 交易开始
     */
    public void transDeleteStart(final Context context) {
        mContext = context;
        mapTranseContent = JsonTransPackage();
        if (timer != null) {
            timer.purge();
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new DelayTask(), 4 * 60 * 1000);
        LogUtil.i("request xml", mapTranseContent.size() + "...." + mapTranseContent.toString());
        new Thread(new Runnable() {
            public void run() {
                Message msg = Message.obtain();
                msg.what = msg_server_response;
                Bundle bundle = new Bundle();
                InputStream inputStream = HttpsClient.delete(url, mapTranseContent, context);
                responseCode = HttpsClient.getResponseCode();
                if (inputStream != null) {
                    LogUtil.i(TAG, "inputstream is not null ");
                    if (transParse(inputStream, TransType.DELETE)) {
                        bundle.putBoolean(msg_bundle_key_1, true);
                    } else {
                        bundle.putBoolean(msg_bundle_key_1, false);
                    }
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtil.i(TAG, "inputstream is null ");
                    bundle.putBoolean(msg_bundle_key_1, false);
                }
                if (timer != null) {
                    timer.purge();
                    timer.cancel();
                    timer = null;
                }
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            }
        }).start();
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case msg_server_response:
                    Bundle bundle = msg.getData();
                    boolean isOk = bundle.getBoolean(msg_bundle_key_1);
                    if (isOk) {
                        transComplete();
                    } else {
                        transFailed(HttpsClient.getResponseCode(),
                                HttpsClient.getErrInfo());
                    }
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }

    };

    /**
     * 预处理
     */
    protected boolean preparStart = false;

    public AbstractTransaction() {
        super();
        initData();
        prepareStart();

    }

    private void prepareStart() {
        if (preparStart) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "预请求服务器URL" + url);
                    HttpsClient.preConnect(url, mContext);
                }
            }).start();
        }
    }

    /**
     * 初始化操作
     */
    public void initData() {
        prepareStart();
    }

    /**
     * 服务器请求组包 说明：组包从keyFactor(密钥因子)开始，其中不包含partnerNo（商品编号），到remark（预留字段）前结束
     */
    protected abstract String transPackage();

    /**
     * 服务器请求组包 说明：组包从keyFactor(密钥因子)开始，其中不包含partnerNo（商品编号），到remark（预留字段）前结束
     */
    protected abstract Map<String, Object> JsonTransPackage();

    /**
     * 返回结果解析解包
     */
    protected abstract boolean transParse(InputStream inputStream, TransType method);

    public void transComplete() {
        LogUtil.i("transComplete", "..transComplete:");
        if (transferPayResponse != null) {
            transferPayResponse.transComplete();
        }
    }

    /**
     * 服务器请求失败
     */
    protected void transFailed(String httpCode, String errorInfo) {
        if (transferPayResponse != null) {
            transferPayResponse.transFailed(httpCode, errorInfo);
        }
    }

    /**
     * 读取流信息
     *
     * @param inputStream
     */
    public String readInputStream(InputStream inputStream) {
        StringBuilder str = new StringBuilder();
        BufferedReader bufferedReader;
        String line = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(
                    inputStream, StandardCharsets.UTF_8));
            while ((line = bufferedReader.readLine()) != null) {
                str.append(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LogUtil.i(TAG, "response xml=" + str);
        return str.toString();
    }

    /***
     * 切换服务器超时
     *
     * @param isEK 是否是易酷服务器
     */
    public void setServerTimeOut(boolean isEK) {
        if (isEK) {
            HttpsClient.setHttpClient(null);
            HttpsClient.setConnTimeout(HttpsClient.EK_SERVER_CONN_TIME_OUT);
            HttpsClient.setRecvTimeout(HttpsClient.EK_SERVER_RECV_TIME_OUT);
        } else {
            HttpsClient.setHttpClient(null);
            HttpsClient.setConnTimeout(HttpsClient.CONN_TIME_OUT);
            HttpsClient.setRecvTimeout(HttpsClient.RECV_TIME_OUT);
        }
    }

    /**
     * 返回结果
     */
    public interface TransferPayResponse {
        /**
         * 服务器请求完成
         */
        void transComplete();

        /**
         * 服务器请求失败
         */
        void transFailed(String httpCode, String errorInfo);
    }

    public void setTransferPayResponse(TransferPayResponse tPayResponse) {
        this.transferPayResponse = tPayResponse;
    }

    /**
     * 判断交易类型是否需要签名
     **/
    public static boolean needSign(TransType transType) {

        return false;
    }

    public JSONObject processResult(String response) {
        JSONObject object = null;
        try {
            // object = new JSONObject(response);
            // String result = object.getString(ParamName.RESULT);
            return new JSONObject(response);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            String result = "{\"respCode\":\"-99\",\"respDesc\":\"数据解析失败\"}";
            try {
                return new JSONObject(result);
            } catch (JSONException e1) {
                return null;
            }
        }

    }

    public JSONObject processResp(String response) {
        JSONObject object = null;
        try {
            object = new JSONObject(response);
            String result = object.getString(ConstantUtil.ParamName.RESULT);
            return new JSONObject(response);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            String result = "{\"respCode\":\"-99\",\"respDesc\":\"数据解析失败\"}";
            try {
                return new JSONObject(result);
            } catch (JSONException e1) {
                return null;
            }
        }

    }

    public void initParamData(ArrayList<Object> params) {
        // String sendDate = System.currentTimeMillis() + "";
        // NameValuePair sendDatePair = new
        // BasicNameValuePair(ParamName.SENDTIME,
        // sendDate);
        // params.add(sendDatePair);
        // String singertrue = "null";
        // try {
        // // singertrue = MainApplication.app.getRsaDeHelper().sign(sendDate);
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        // LogUtil.d("initParamData", sendDate + "....." + singertrue);
        // NameValuePair signerPair = new
        // BasicNameValuePair(ParamName.SIGNER_DATA,
        // singertrue);
        // params.add(signerPair);
    }

    public void initParamData(Map<String, Object> map) {
        LogUtil.d(TAG, "requst map : " + map.toString());
    }

}
