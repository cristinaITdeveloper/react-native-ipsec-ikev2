"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.disconnect = exports.getCharonErrorState = exports.getCurrentState = exports.connect = exports.VpnType = exports.prepare = exports.onStateChangedListener = exports.removeOnStateChangeListener = exports.STATE_CHANGED_EVENT_NAME = exports.CharonErrorState = exports.VpnState = void 0;
const react_native_1 = require("react-native");
var VpnState;
(function (VpnState) {
    VpnState[VpnState["disconnected"] = 0] = "disconnected";
    VpnState[VpnState["connecting"] = 1] = "connecting";
    VpnState[VpnState["connected"] = 2] = "connected";
    VpnState[VpnState["disconnecting"] = 3] = "disconnecting";
    VpnState[VpnState["genericError"] = 4] = "genericError";
})(VpnState = exports.VpnState || (exports.VpnState = {}));
var CharonErrorState;
(function (CharonErrorState) {
    CharonErrorState[CharonErrorState["NO_ERROR"] = 0] = "NO_ERROR";
    CharonErrorState[CharonErrorState["AUTH_FAILED"] = 1] = "AUTH_FAILED";
    CharonErrorState[CharonErrorState["PEER_AUTH_FAILED"] = 2] = "PEER_AUTH_FAILED";
    CharonErrorState[CharonErrorState["LOOKUP_FAILED"] = 3] = "LOOKUP_FAILED";
    CharonErrorState[CharonErrorState["UNREACHABLE"] = 4] = "UNREACHABLE";
    CharonErrorState[CharonErrorState["GENERIC_ERROR"] = 5] = "GENERIC_ERROR";
    CharonErrorState[CharonErrorState["PASSWORD_MISSING"] = 6] = "PASSWORD_MISSING";
    CharonErrorState[CharonErrorState["CERTIFICATE_UNAVAILABLE"] = 7] = "CERTIFICATE_UNAVAILABLE";
    CharonErrorState[CharonErrorState["UNDEFINED"] = 8] = "UNDEFINED";
})(CharonErrorState = exports.CharonErrorState || (exports.CharonErrorState = {}));
const stateChanged = new react_native_1.NativeEventEmitter(react_native_1.NativeModules.RNIpSecVpn);
exports.STATE_CHANGED_EVENT_NAME = "stateChanged";
exports.removeOnStateChangeListener = (stateChangedEvent) => {
    stateChangedEvent.remove();
};
exports.onStateChangedListener = (callback) => {
    return stateChanged.addListener(exports.STATE_CHANGED_EVENT_NAME, (e) => callback(e));
};
exports.prepare = react_native_1.NativeModules.RNIpSecVpn.prepare;
var VpnType;
(function (VpnType) {
    VpnType["IKEV2_EAP"] = "ikev2-eap";
    VpnType["IKEV2_CERT"] = "ikev2-cert";
    VpnType["IKEV2_CERT_EAP"] = "ikev2-cert-eap";
    VpnType["IKEV2_EAP_TLS"] = "ikev2-eap-tls";
    VpnType["IKEV2_BYOD_EAP"] = "ikev2-byod-eap";
})(VpnType = exports.VpnType || (exports.VpnType = {}));
exports.connect = (name, address, username, password, vpnType, secret, disconnectOnSleep, mtu, b64CaCert, b64UserCert, certAlias, userCertPassword) => react_native_1.NativeModules.RNIpSecVpn.connect(name || "", address || "", username || "", password || "", vpnType || "", secret || "", disconnectOnSleep || false, mtu || 1400, b64CaCert || "", b64UserCert || "", userCertPassword || "", certAlias || "");
exports.getCurrentState = react_native_1.NativeModules.RNIpSecVpn.getCurrentState;
exports.getCharonErrorState = react_native_1.NativeModules.RNIpSecVpn.getCharonErrorState;
exports.disconnect = react_native_1.NativeModules.RNIpSecVpn.disconnect;
exports.default = react_native_1.NativeModules.RNIpSecVpn;
//# sourceMappingURL=index.js.map