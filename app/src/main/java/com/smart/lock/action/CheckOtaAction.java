
package com.smart.lock.action;

import com.smart.lock.entity.VersionModel;
import com.smart.lock.transfer.HttpCodeHelper;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.ConstantUtil.ParamName;
import com.smart.lock.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @version 创建时间：2012-8-24 下午5:08:51 说明：
 */

public class CheckOtaAction extends AbstractTransaction {
    public static final int NO_NEW_VERSION = 0;
    public static final int SELECT_VERSION_UPDATE = 1;
    public static final int MAST_UPDATE_VERSION = 2;

    class ClientCheckVersionSend {
        String deviceSn;
        String devCurVer;
        String extension;
        String fpType;
        String fpCurVer;
        String fpCurZone;
    }

    public String getDevCurVer() {
        return sendData.devCurVer;
    }

    public void setDevCurVer(String devCurVer) {
        sendData.devCurVer = devCurVer;
    }

    public String getFpType() {
        return sendData.fpType;
    }

    public void setFpType(String fpType) {
        sendData.fpType = fpType;
    }

    public String getFpCurVer() {
        return sendData.fpCurVer;
    }

    public void setFpCurVer(String fpCurVer) {
        sendData.fpCurVer = fpCurVer;
    }

    public String getFpCurZone() {
        return sendData.fpCurZone;
    }

    public void setFpCurZone(String fpCurZone) {
        sendData.fpCurZone = fpCurZone;
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
        public ArrayList<VersionModel> models;

    }

    private static final String XML_TAG = "CheckVersionAction";

    public CheckOtaAction() {
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

    private void resloveCheckVersionResult(String response) throws JSONException, IOException {
        JSONObject object = super.processResult(response);
        if (object == null) {
            respondData.respCode = HttpCodeHelper.HTTP_REQUEST_ERROR;
        } else {
            try {
                respondData.respCode = object.getString(ParamName.RESD_CODE);
                if (HttpCodeHelper.RESPONSE_SUCCESS.equals(respondData.respCode)) {
                    respondData.models = new ArrayList<>();
                    JSONObject devObj = null;
                    if (object.has("device")) {
                        devObj = object.getJSONObject("device");
                        if (devObj != null) {
                            VersionModel versionModel = new VersionModel();
                            versionModel.type = ConstantUtil.OTA_LOCK_SW_VERSION;
                            versionModel.fileName = devObj.getString("filename");
                            versionModel.updateDate = devObj.getString("updateDate");
                            versionModel.versionCode = devObj.getInt("versionCode");
                            versionModel.versionName = devObj.getString("version");
                            versionModel.md5 = devObj.getString("md5");
                            versionModel.forceUpdate = devObj.getBoolean("forceUpdate");
                            versionModel.path = devObj.getString("path");
                            versionModel.msg = devObj.getString("msg");
                            respondData.models.add(versionModel);
                        }
                    }
                    JSONObject fpObj = null;
                    if (object.has("fingerprint")) {
                        fpObj = object.getJSONObject("fingerprint");
                        if (fpObj != null) {
                            VersionModel versionModel = new VersionModel();
                            versionModel.type = ConstantUtil.OTA_FP_SW_VERSION;
                            versionModel.fileName = fpObj.getString("filename");
                            versionModel.updateDate = fpObj.getString("updateDate");
                            versionModel.versionCode = fpObj.getInt("versionCode");
                            versionModel.versionName = fpObj.getString("version");
                            versionModel.path = fpObj.getString("path");
                            versionModel.sha1 = fpObj.getString("sha1");
                            versionModel.zone = fpObj.getString("zone");
                            respondData.models.add(versionModel);
                        }
                    }
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
        paramCheckVersion.put(ParamName.DEV_CUR_VER, sendData.devCurVer);
        Map<String, Object> fingerprint = new HashMap<>();
        fingerprint.put(ParamName.FP_TYPE, sendData.fpType);
        fingerprint.put(ParamName.FP_CUR_VER, sendData.fpCurVer);
        fingerprint.put(ParamName.FP_CUR_ZONE, sendData.fpCurZone);
        paramCheckVersion.put(ParamName.FINGERPRINT, fingerprint);

        super.initParamData(paramCheckVersion);
        return paramCheckVersion;
    }

}
