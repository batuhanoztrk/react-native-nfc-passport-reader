import { NativeModules, DeviceEventEmitter, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-nfc-passport-reader' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const NfcPassportReaderNativeModule = NativeModules.NfcPassportReader
  ? NativeModules.NfcPassportReader
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

enum NfcPassportReaderEvent {
  SUCCESS = 'onNfcResult',
  ERROR = 'onNfcError',
  TAG_DISCOVERED = 'onTagDiscovered',
  NFC_STATE_CHANGED = 'onNfcStateChanged',
}

export type NfcResult = {
  birthDate?: string;
  placeOfBirth?: string;
  documentNo?: string;
  expiryDate?: string;
  firstName?: string;
  gender?: string;
  identityNo?: string;
  lastName?: string;
  mrz?: string;
  nationality?: string;
  originalFacePhoto?: string; // base64
};

export default class NfcPassportReader {
  static startReading(mrz: string) {
    NfcPassportReaderNativeModule.startReading(mrz);
  }

  static stopReading() {
    NfcPassportReaderNativeModule.stopReading();
  }

  static addOnSuccessListener(callback: (data: NfcResult) => void) {
    this.addListener(NfcPassportReaderEvent.SUCCESS, callback);
  }

  static addOnErrorListener(callback: (error: string) => void) {
    this.addListener(NfcPassportReaderEvent.ERROR, callback);
  }

  static addOnTagDiscoveredListener(callback: () => void) {
    this.addListener(NfcPassportReaderEvent.TAG_DISCOVERED, callback);
  }

  static addOnNfcStateChangedListener(callback: (state: 'off' | 'on') => void) {
    this.addListener(NfcPassportReaderEvent.NFC_STATE_CHANGED, callback);
  }

  static isNfcEnabled(): Promise<boolean> {
    return NfcPassportReaderNativeModule.isNfcEnabled();
  }

  static isNfcSupported(): Promise<boolean> {
    return NfcPassportReaderNativeModule.isNfcSupported();
  }

  static openNfcSettings(): Promise<boolean> {
    return NfcPassportReaderNativeModule.openNfcSettings();
  }

  private static addListener(
    event: NfcPassportReaderEvent,
    callback: (data: any) => void
  ) {
    DeviceEventEmitter.addListener(event, callback);
  }

  static removeListeners() {
    DeviceEventEmitter.removeAllListeners(NfcPassportReaderEvent.SUCCESS);
    DeviceEventEmitter.removeAllListeners(NfcPassportReaderEvent.ERROR);
    DeviceEventEmitter.removeAllListeners(
      NfcPassportReaderEvent.TAG_DISCOVERED
    );
    DeviceEventEmitter.removeAllListeners(
      NfcPassportReaderEvent.NFC_STATE_CHANGED
    );
  }
}
