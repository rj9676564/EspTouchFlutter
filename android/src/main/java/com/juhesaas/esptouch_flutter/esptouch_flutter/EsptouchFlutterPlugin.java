package com.juhesaas.esptouch_flutter.esptouch_flutter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.location.LocationManagerCompat;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.espressif.iot.esptouch.util.ByteUtil;
import com.espressif.iot.esptouch.util.TouchNetUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EsptouchFlutterPlugin
 */
public class EsptouchFlutterPlugin implements FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private static final String TAG = EsptouchFlutterPlugin.class.getSimpleName();

    private MethodChannel channel;
    private static Context mContext;
    private MethodCall _call;
    private Result _methodResult;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "esptouch_flutter");
        channel.setMethodCallHandler(this);
        mContext = flutterPluginBinding.getApplicationContext();
        mWifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("getWifiInfo")) {
            Map wifiResult = onWifiChanged();
            result.success(wifiResult);
        } else if (call.method.equals("cancelConnect")) {
            if (mTask != null && !mTask.isCancelled()) {
                mTask.cancelEsptouch();
            }
            result.success(true);
        } else if (call.method.equals("connectWifi")) {
            _methodResult = result;
            _call = call;

            String mSsid = call.argument("mSsid");
            String pwd = call.argument("pwd");
            String mBssid = call.argument("mBssid");
            String devCount = call.argument("devCount");
            boolean modelGroup = call.argument("modelGroup");
            if (devCount == null) {
                devCount = "1";
            }

            executeEsptouch(mSsid, pwd, mBssid, devCount, modelGroup ? 1 : 0);
        } else {
            result.notImplemented();
            _methodResult = null;
            _call = null;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    private byte[] mSsidBytes;
    private String mSsid = "";
    private String mBssid = "";
    private EsptouchAsyncTask4 mTask;

    private Map onWifiChanged() {

        StateResult stateResult = checkWifi();
        mSsid = stateResult.ssid;
        mSsidBytes = stateResult.ssidBytes;
        mBssid = stateResult.bssid;
        CharSequence message = stateResult.message;
        boolean confirmEnable = false;
        if (stateResult.wifiConnected) {
            if (stateResult.is5G) {
                message = "设备不支持 5G Wi-Fi, 请确认当前连接的 Wi-Fi 为 2.4G";
            } else {
                confirmEnable = true;
            }
        } else {
            if (mTask != null) {
                mTask.cancelEsptouch();
                mTask = null;
            }
        }
        Map hashMap = new HashMap<String, String>();
        hashMap.put("message", message);
        hashMap.put("mBssid", mBssid);
        hashMap.put("mSsid", mSsid);
        return hashMap;
    }

    private WifiManager mWifiManager;

    protected StateResult checkWifi() {
        StateResult result = new StateResult();
        result.wifiConnected = false;
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        boolean connected = com.espressif.iot.esptouch2.provision.TouchNetUtil.isWifiConnected(mWifiManager);
        if (!connected) {
            return result;
        }

        String ssid = com.espressif.iot.esptouch2.provision.TouchNetUtil.getSsidString(wifiInfo);
        int ipValue = wifiInfo.getIpAddress();
        if (ipValue != 0) {
            result.address = com.espressif.iot.esptouch2.provision.TouchNetUtil.getAddress(wifiInfo.getIpAddress());
        } else {
            result.address = com.espressif.iot.esptouch2.provision.TouchNetUtil.getIPv4Address();
            if (result.address == null) {
                result.address = com.espressif.iot.esptouch2.provision.TouchNetUtil.getIPv6Address();
            }
        }

        result.wifiConnected = true;
        result.message = "";
        result.is5G = com.espressif.iot.esptouch2.provision.TouchNetUtil.is5G(wifiInfo.getFrequency());
        if (result.is5G) {
//            result.message = getString(R.string.esptouch_message_wifi_frequency);

        }
        result.ssid = ssid;
        result.ssidBytes = com.espressif.iot.esptouch2.provision.TouchNetUtil.getRawSsidBytesOrElse(wifiInfo, ssid.getBytes());
        result.bssid = wifiInfo.getBSSID();

        return result;
    }


    private void executeEsptouch(String mSsid, CharSequence pwdStr, String mBssid, CharSequence devCountStr, int modelGroup) {
        byte[] ssid = mSsidBytes == null ? ByteUtil.getBytesByString(mSsid)
                : mSsidBytes;
        byte[] password = pwdStr == null ? null : ByteUtil.getBytesByString(pwdStr.toString());
        byte[] bssid = TouchNetUtil.parseBssid2bytes(mBssid);
        byte[] deviceCount = devCountStr == null ? new byte[0] : devCountStr.toString().getBytes();
        byte[] broadcast = {(byte) modelGroup};

        if (mTask != null) {
            mTask.cancelEsptouch();
        }
        mTask = new EsptouchAsyncTask4(this);
        mTask.execute(ssid, bssid, password, deviceCount, broadcast);

    }

    private static class EsptouchAsyncTask4 extends AsyncTask<byte[], IEsptouchResult, List<IEsptouchResult>> {
        private final WeakReference<EsptouchFlutterPlugin> mFlutterPlugin;
        private final Object mLock = new Object();
        private IEsptouchTask mEsptouchTask;

        EsptouchAsyncTask4(EsptouchFlutterPlugin activity) {
            mFlutterPlugin = new WeakReference<>(activity);
        }

        void cancelEsptouch() {
            cancel(true);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(IEsptouchResult... values) {
            IEsptouchResult result = values[0];
            Log.i(TAG, "EspTouchResult: " + result);
            String text = result.getBssid() + " is connected to the wifi";
            Log.i(TAG, "EspTouchResult: " + text);
        }

        @Override
        protected List<IEsptouchResult> doInBackground(byte[]... params) {
            int taskResultCount;
            synchronized (mLock) {
                byte[] apSsid = params[0];
                byte[] apBssid = params[1];
                byte[] apPassword = params[2];
                byte[] deviceCountData = params[3];
                byte[] broadcastData = params[4];
                taskResultCount = deviceCountData.length == 0 ? -1 : Integer.parseInt(new String(deviceCountData));
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, mContext);
                mEsptouchTask.setPackageBroadcast(broadcastData[0] == 1);
                mEsptouchTask.setEsptouchListener(this::publishProgress);
            }
            return mEsptouchTask.executeForResults(taskResultCount);
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {
            Result methodResult = mFlutterPlugin.get()._methodResult;
            if (result == null) {
                methodResult.success(false);
                return;
            }

            // check whether the task is cancelled and no results received
            IEsptouchResult firstResult = result.get(0);
            Map resultMap = new HashMap();
            resultMap.put("success", firstResult.isSuc());
            resultMap.put("cancel", firstResult.isCancelled());
            methodResult.success(resultMap);
            Log.e(TAG, "配置成功");
        }
    }

}
