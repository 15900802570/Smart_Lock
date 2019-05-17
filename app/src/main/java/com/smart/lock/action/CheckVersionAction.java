
package com.smart.lock.action;

import android.util.Log;

import com.smart.lock.entity.VersionModel;
import com.smart.lock.transfer.HttpCodeHelper;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.ConstantUtil.ParamName;
import com.smart.lock.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @version 创建时间：2012-8-24 下午5:08:51 说明：
 */

public class CheckVersionAction extends AbstractTransaction {
    public static final int NO_NEW_VERSION = 0;
    public static final int SELECT_VERSION_UPDATE = 1;
    public static final int MAST_UPDATE_VERSION = 2;

    class ClientCheckVersionSend {
        String deviceSn;
        String extension;
    }

    public String getDeviceSn() {
        return sendData.deviceSn;
    }

    public void setDeviceSn(String deviceSn) {
        sendData.deviceSn = deviceSn;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getExtension() {
        return sendData.extension;
    }

    public void setExtension(String extension) {
        this.sendData.extension = extension;
    }

    public class CheckVersionRespond {
        public String respCode;
        public String respDesc;
        public VersionModel model;

    }

    private static final String XML_TAG = "CheckVersionAction";

    public CheckVersionAction() {
        respondData = new CheckVersionRespond();
        sendData = new ClientCheckVersionSend();
    }

    public CheckVersionRespond respondData;
    private ClientCheckVersionSend sendData;

    @Override
    protected String transPackage() {

        return null;
    }

    @Override
    protected boolean transParse(InputStream inputStream, TransType type) {
        LogUtil.i(XML_TAG, "开始解析inputStream");
        /**
         * 采用pull解析方式：XmlPullParser采用驱动解析，占用内存少，无需一次性加载数据
         */
        String json = null;
        if (inputStream == null) {
            // json = TestData.creatCheckVersionData();
            respondData.respCode = HttpCodeHelper.ERROR;
            return false;
        } else {
            json = readInputStream(inputStream);
        }

        LogUtil.i(XML_TAG, "json:" + json);
        try {
            resloveCheckVersionResult(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    void resloveCheckVersionResult(String response) throws JSONException, IOException {
        JSONObject object = super.processResult(response);
        if (object == null) {
            respondData.respCode = HttpCodeHelper.HTTP_REQUEST_ERROR;
        } else {
            try {
                respondData.respCode = object.getString(ParamName.RESD_CODE);
                if (HttpCodeHelper.RESPONSE_SUCCESS.equals(respondData.respCode)) {
                    VersionModel versionModel = new VersionModel();
                    versionModel.updateDate = object.getString("updateDate");
                    versionModel.versionCode = object.getInt("versionCode");
                    versionModel.versionName = object.getString("version");
                    versionModel.md5 = object.getString("md5");
                    versionModel.forceUpdate = object.getBoolean("forceUpdate");
                    versionModel.extension = object.getString("extension");
                    versionModel.path = object.getString("path");
                    versionModel.msg = object.getString("msg");
                    respondData.model = versionModel;
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected Map<String, Object> JsonTransPackage() {
        Map<String, Object> paramCheckVersion = new HashMap<>();
        paramCheckVersion.put(ParamName.DEVICE_SN, sendData.deviceSn);
        paramCheckVersion.put(ParamName.EXTENSION, sendData.extension);

        super.initParamData(paramCheckVersion);
        return paramCheckVersion;
    }

}
