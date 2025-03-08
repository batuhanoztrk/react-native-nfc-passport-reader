# NFC Passport Reader for React Native

This React Native plugin enables the reading of NFC-enabled passports using native device capabilities. It provides a user-friendly interface for initiating and handling NFC operations, including reading passport data, checking NFC support, and managing NFC settings.

## Features

- Start and stop NFC passport reading
- Basic Access Control (BAC) support for secure passport reading
- Check if NFC is supported and enabled on the device
- Open device NFC settings
- Support for both iOS and Android platforms
- Optional image extraction from passport

## Installation

To use the NFC Passport Reader in your React Native project, follow these steps:

1. **Install the Plugin**:
   ```sh
   npm install react-native-nfc-passport-reader
   ```
2. **Link Native Modules (if required for versions below React Native 0.60)**:
   ```sh
   npx react-native link react-native-nfc-passport-reader
   ```
3. **iOS Additional Setup**:
   - Modify your Info.plist to include necessary NFC usage descriptions.
     ```xml
     <key>NFCReaderUsageDescription</key>
     <string>This app requires NFC access to verify your identity.</string>
     <key>com.apple.developer.nfc.readersession.iso7816.select-identifiers</key>
     <array>
        <string>A0000002471001</string>
        <string>00000000000000</string>
        <string>D4100000030001</string>
     </array>
     ```
   - Ensure your entitlements include NFC tag reading capability.
   - Add the following pod to your Podfile:
     ```ruby
     pod 'OpenSSL-Universal', '~> 1.1.1900'
     ```
   - Disable Flipper in your Podfile (required for proper functionality)

4. **Android Additional Setup**:
   - Add NFC permissions in your AndroidManifest.xml.
     ```xml
     <uses-feature android:name="android.hardware.nfc" android:required="false" />
     <uses-permission android:name="android.permission.NFC" />
     ```
   - Ensure your device has NFC capabilities and that NFC is enabled.

## Usage

Import and use the NFC Passport Reader as follows:

```ts
import NfcPassportReader from 'react-native-nfc-passport-reader';
import type { NfcResult } from 'react-native-nfc-passport-reader';
```

### Basic Methods

- **startReading**: Initiates the NFC passport reading process.
  ```ts
  const result: NfcResult = await NfcPassportReader.startReading({
    bacKey: {
      documentNo: '123456789', // Document Number
      expiryDate: '2025-03-09', // YYYY-MM-DD
      birthDate: '2025-03-09', // YYYY-MM-DD
    },
    includeImages: true, // Include images in the result (default: false)
  });
  ```
- **stopReading**: Stops the NFC passport reading process. ***(Only Android)***
  ```ts
  NfcPassportReader.stopReading();
  ```

### Event Listeners (Only Android)

- **addOnTagDiscoveredListener**: Triggers when an NFC tag is discovered.
  ```ts
  NfcPassportReader.addOnTagDiscoveredListener(() => {
    console.log('Tag Discovered');
  });
  ```
- **addOnNfcStateChangedListener**: Monitors changes in NFC state.
  ```ts
  NfcPassportReader.addOnNfcStateChangedListener((state: 'on' | 'off') => {
    console.log('NFC State Changed:', state);
  });
  ```

### Check Device Support

- **isNfcSupported**: Checks if NFC is supported by the device.
  ```ts
  const supported = await NfcPassportReader.isNfcSupported();
  ```
- **isNfcEnabled**: Checks if NFC is enabled on the device.
  ```ts
  const enabled = await NfcPassportReader.isNfcEnabled();
  ```

### Settings

- **openNfcSettings**: Opens the device's NFC settings. ***(Only Android)***
  ```ts
  NfcPassportReader.openNfcSettings();
  ```

## Example

For a detailed example of how to use the NFC Passport Reader, please see the [Example App](example/src/App.tsx).

## Acknowledgments

Special thanks to [Andy Qua](https://github.com/AndyQ) for his excellent [NFCPassportReader](https://github.com/AndyQ/NFCPassportReader) library that powers the iOS implementation of this package. His work on implementing BAC, Secure Messaging, and various passport data group readings has been instrumental in making this React Native wrapper possible.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
