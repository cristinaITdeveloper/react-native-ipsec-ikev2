package com.sijav.reactnativeipsecvpn;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.strongswan.android.logic.VpnStateService;
import org.strongswan.android.data.*;
import org.strongswan.android.logic.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.*;
import java.security.Security;
import android.content.*;
import android.content.pm.PackageManager;
import android.app.Service;
import android.net.*;
import android.os.*;
import android.util.Log;
import java.io.*;
import java.util.List;
import org.json.*;
import org.strongswan.android.security.LocalCertificateKeyStoreProvider;
import java.util.Enumeration;
import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.ui.VpnProfileListFragment.OnVpnProfileSelectedListener;
import java.security.PrivateKey;

import static android.app.Activity.RESULT_OK;

public class RNIpSecVpn extends ReactContextBaseJavaModule implements OnVpnProfileSelectedListener {
    private static final String TAG = RNIpSecVpn.class.getSimpleName();

    public static final boolean USE_BYOD = true;

    private static final String DIALOG_TAG = "Dialog";

    @SuppressLint("StaticFieldLeak")
    private static ReactApplicationContext reactContext;

    private RNIpSecVpnStateHandler _RNIpSecVpnStateHandler;

    /*
     * The libraries are extracted to /data/data/org.strongswan.android/... during
     * installation. On newer releases most are loaded in JNI_OnLoad.
     */
    static {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            System.loadLibrary("strongswan");

            if (RNIpSecVpn.USE_BYOD) {
                System.loadLibrary("tpmtss");
                System.loadLibrary("tncif");
                System.loadLibrary("tnccs");
                System.loadLibrary("imcv");
            }

            System.loadLibrary("charon");
            System.loadLibrary("ipsec");
        }
        System.loadLibrary("androidbridge");

        Security.addProvider(new LocalCertificateKeyStoreProvider());
    }

    RNIpSecVpn(ReactApplicationContext context) {
        super(context);
        // Load charon bridge
        System.loadLibrary("androidbridge");
        reactContext = context;
        Intent vpnStateServiceIntent = new Intent(context, VpnStateService.class);
        _RNIpSecVpnStateHandler = new RNIpSecVpnStateHandler(this);
        context.bindService(vpnStateServiceIntent, _RNIpSecVpnStateHandler, Service.BIND_AUTO_CREATE);
        new LoadCertificatesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onVpnProfileSelected(VpnProfile profile) {
        // startVpnProfile(profile, true);
    }

    /**
     * Class that loads the cached CA certificates.
     */
    private class LoadCertificatesTask extends AsyncTask<Void, Void, TrustedCertificateManager> {
        @Override
        protected TrustedCertificateManager doInBackground(Void... params) {
            return TrustedCertificateManager.getInstance().load();
        }
    }

    void sendEvent(String eventName, @Nullable WritableMap params) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    @Override
    public String getName() {
        return "RNIpSecVpn";
    }

    @ReactMethod
    public void prepare(final Promise promise) {
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            promise.reject("E_ACTIVITY_DOES_NOT_EXIST", "Activity doesn't exist");
            return;
        }
        Intent intent = VpnService.prepare(currentActivity);
        if (intent != null) {
            reactContext.addActivityEventListener(new BaseActivityEventListener() {
                public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
                    if (requestCode == 0 && resultCode == RESULT_OK) {
                        promise.resolve(null);
                    } else {
                        promise.reject("PrepareError", "Failed to prepare");
                    }
                }
            });
            currentActivity.startActivityForResult(intent, 0);
        }
    }

    @ReactMethod
    public void connect(String name, String address, String username, String password, String vpnType, String secret, Boolean disconnectOnSleep, Integer mtu,
            String b64CaCert, String b64UserCert, String userCertPassword, String certAlias, Promise promise)
            throws Exception {

        Log.i(TAG, "Connecting");

        if (_RNIpSecVpnStateHandler.vpnStateService == null) {
            promise.reject("E_SERVICE_NOT_STARTED", "Service not started yet");
            return;
        }
        if (mtu == 0) {
            mtu = 1400;
        }
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            promise.reject("E_ACTIVITY_DOES_NOT_EXIST", "Activity doesn't exist");
            return;
        }
        Intent intent = VpnService.prepare(currentActivity);
        if (intent != null) {
            promise.reject("PrepareError", "Not prepared");
            return;
        }
        Log.i(TAG, "Certificate adding....");

        Bundle profileInfo = new Bundle();
        profileInfo.putString("Address", address);
        profileInfo.putString("UserName", username);
        profileInfo.putString("Password", password);
        profileInfo.putString("VpnType", VpnType.IKEV2_CERT.getIdentifier());
        profileInfo.putInt("MTU", 1400);
        profileInfo.putString("CertAlias", "vpnclient");

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

                PrivateKey key = null;
                try {
                    key = UserCredentialManager.getInstance().getUserKey("vpnclient", "080021500".toCharArray());

                    if (key == null) {
                        UserCredentialManager.getInstance().storeCredentials(b64UserCert.getBytes(),
                                userCertPassword.toCharArray());
                    }
                    Log.i(TAG, "Certificate Added");

                    // Decode the CA certificate from base64 to an X509Certificate
                    byte[] decoded = android.util.Base64.decode(b64CaCert.getBytes(), 0);
                    CertificateFactory factory = CertificateFactory.getInstance("X.509");
                    InputStream in = new ByteArrayInputStream(decoded);
                    X509Certificate certificate = (X509Certificate) factory.generateCertificate(in);

                    // And then import it into the Strongswan LocalCertificateStore
                    KeyStore store = KeyStore.getInstance("LocalCertificateStore");
                    store.load(null, null);
                    store.setCertificateEntry(null, certificate);
                    TrustedCertificateManager.getInstance().reset();
                    Log.i(TAG, "Sending startConnection request");

                    _RNIpSecVpnStateHandler.vpnStateService.connect(profileInfo, true);
                    Log.i(TAG, "Sent startConnection request");
                } catch (KeyStoreException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (UnrecoverableKeyException e) {
                    e.printStackTrace();
                } catch (CertificateException e) {
                    e.printStackTrace();
                }
            }
        });

        promise.resolve(null);
    }

    @ReactMethod
    public void getCurrentState(Promise promise) {
        if (_RNIpSecVpnStateHandler.vpnStateService == null) {
            promise.reject("E_SERVICE_NOT_STARTED", "Service not started yet");
            return;
        }
        VpnStateService.ErrorState errorState = _RNIpSecVpnStateHandler.vpnStateService.getErrorState();
        VpnStateService.State state = _RNIpSecVpnStateHandler.vpnStateService.getState();
        if (errorState == VpnStateService.ErrorState.NO_ERROR) {
            promise.resolve(state != null ? state.ordinal() : 4);
        } else {
            promise.resolve(4);
        }
    }

    @ReactMethod
    public void getCharonErrorState(Promise promise) {
        if (_RNIpSecVpnStateHandler.vpnStateService == null) {
            promise.reject("E_SERVICE_NOT_STARTED", "Service not started yet");
            return;
        }
        VpnStateService.ErrorState errorState = _RNIpSecVpnStateHandler.vpnStateService.getErrorState();
        promise.resolve(errorState != null ? errorState.ordinal() : 8);
    }

    @ReactMethod
    public void disconnect(Promise promise) {
        if (_RNIpSecVpnStateHandler.vpnStateService != null) {
            _RNIpSecVpnStateHandler.vpnStateService.disconnect();
        }
        promise.resolve(null);
    }

    /**
     * Returns the current application context
     *
     * @return context
     */
    public static ReactApplicationContext getContext() {
        return reactContext;
    }
}
