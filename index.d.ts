import { EmitterSubscription } from "react-native";
export declare enum VpnState {
    disconnected = 0,
    connecting = 1,
    connected = 2,
    disconnecting = 3,
    genericError = 4
}
export declare enum CharonErrorState {
    NO_ERROR = 0,
    AUTH_FAILED = 1,
    PEER_AUTH_FAILED = 2,
    LOOKUP_FAILED = 3,
    UNREACHABLE = 4,
    GENERIC_ERROR = 5,
    PASSWORD_MISSING = 6,
    CERTIFICATE_UNAVAILABLE = 7,
    UNDEFINED = 8
}
export declare const STATE_CHANGED_EVENT_NAME: string;
export declare const removeOnStateChangeListener: (stateChangedEvent: EmitterSubscription) => void;
export declare const onStateChangedListener: (callback: (state: {
    state: VpnState;
    charonState: CharonErrorState;
}) => void) => EmitterSubscription;
export declare const prepare: () => Promise<void>;
export declare enum VpnType {
    IKEV2_EAP = "ikev2-eap",
    IKEV2_CERT = "ikev2-cert",
    IKEV2_CERT_EAP = "ikev2-cert-eap",
    IKEV2_EAP_TLS = "ikev2-eap-tls",
    IKEV2_BYOD_EAP = "ikev2-byod-eap"
}
export declare const connect: (name: string, address: string, username: string, password: string, vpnType?: VpnType, secret?: string, disconnectOnSleep?: boolean, mtu?: number, b64CaCert?: string, b64UserCert?: string, userCertPassword?: string, certAlias?: string) => Promise<void>;
export declare const getCurrentState: () => Promise<VpnState>;
export declare const getCharonErrorState: () => Promise<CharonErrorState>;
export declare const disconnect: () => Promise<void>;
declare const _default: any;
export default _default;
