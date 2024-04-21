# NFC Passport Reader for React Native

This React Native plugin enables the reading of NFC-enabled passports using native device capabilities. It provides a user-friendly interface for initiating and handling NFC operations, including reading passport data, checking NFC support, and managing NFC settings.

## Features

- Start and stop NFC passport reading
- Check if NFC is supported and enabled on the device
- Open device NFC settings

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
4. **Android Additional Setup**:
   - Add NFC permissions in your AndroidManifest.xml.
     ```xml
     <uses-feature android:name="android.hardware.nfc" android:required="false" />
     <uses-permission android:name="android.permission.NFC" />
     ```
   - Ensure your device has NFC capabilities and that NFC is enabled.

**Note**: This plugin is currently only available for Android devices.

## Usage

Import and use the NFC Passport Reader as follows:

```ts
import NfcPassportReader from 'react-native-nfc-passport-reader';
import type { NfcResult } from 'react-native-nfc-passport-reader';
```

### Basic Methods

- **startReading**: Initiates the NFC passport reading process.
  ```ts
  NfcPassportReader.startReading({
    mrz: 'P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<1234567890',
    imagesIncluded: true,
  });
  ```
- **stopReading**: Stops the NFC passport reading process.
  ```ts
  NfcPassportReader.stopReading();
  ```

### Event Listeners

- **addOnSuccessListener**: Listens for successful NFC read operations.
  ```ts
  NfcPassportReader.addOnSuccessListener((data: NfcResult) => {
    console.log('NFC Read Success:', data);
  });
  ```
- **addOnErrorListener**: Captures errors during NFC operations.
  ```ts
  NfcPassportReader.addOnErrorListener((error: string) => {
    console.error('NFC Read Error:', error);
  });
  ```
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
- **stopReading**: Checks if NFC is enabled on the device.
  ```ts
  const enabled = await NfcPassportReader.isNfcEnabled();
  ```

### Settings

- **openNfcSettings**: Opens the device's NFC settings.
  ```ts
  NfcPassportReader.openNfcSettings();
  ```

## Example

For a detailed example of how to use the NFC Passport Reader, please see the [Example App](example/src/App.tsx).

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
