package com.sijav.reactnativeipsecvpn;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import org.strongswan.android.logic.VpnStateService;

public class RNIpSecVpnStateHandler implements ServiceConnection, VpnStateService.VpnStateListener {

    final String TAG = "RNIpSecVpnStateHandler";

    private RNIpSecVpn context;
    VpnStateService vpnStateService;

    RNIpSecVpnStateHandler(RNIpSecVpn context){
        this.context = context;
    }

    @Override
    public void stateChanged() {
        WritableMap params = Arguments.createMap();
        if(vpnStateService == null){
            Log.i(TAG, "RNIpSecVpnStateHandler null");
            params.putInt("state", 4);
            params.putInt("charonState", 8);
            context.sendEvent("stateChanged", params);
            return;
        }
        VpnStateService.ErrorState errorState = vpnStateService.getErrorState();
        if(errorState == VpnStateService.ErrorState.NO_ERROR){
            Log.i(TAG, "RNIpSecVpnStateHandler no error");
            VpnStateService.State state = vpnStateService.getState();
            params.putInt("state", state != null ? state.ordinal() : 4);
            params.putInt("charonState", errorState.ordinal());
            context.sendEvent("stateChanged", params);
        } else {
            Log.i(TAG, "RNIpSecVpnStateHandler error");

            params.putInt("state", 4);
            params.putInt("charonState", errorState != null ? errorState.ordinal() : 8);
            context.sendEvent("stateChanged", params);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        vpnStateService = ((VpnStateService.LocalBinder)service).getService();
        if(vpnStateService != null){
            vpnStateService.registerListener(this);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        vpnStateService = null;
    }
}