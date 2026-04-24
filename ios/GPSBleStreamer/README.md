# iOS GPS BLE Streamer

SwiftUI iOS app that publishes GPS coordinates over BLE GATT.

## Targets

- Swift 5
- iOS 14+
- CoreBluetooth peripheral mode
- CoreLocation

## BLE GATT

- Service UUID: `0000feed-0000-1000-8000-00805f9b34fb`
- Characteristic UUID: `0000beef-0000-1000-8000-00805f9b34fb`
- Characteristic properties: notify + read

## Payloads

- JSON: `{"lat":37.7749,"lon":-122.4194,"alt":12.1,"speed":10.5,"timestamp":1710000000}`
- NMEA: `$GPRMC,173102.00,A,3746.494,N,12225.164,W,21.2,0.0,010100,,,A*00`

## Project Modules

- `BLEManager.swift`: BLE peripheral lifecycle and notifications.
- `LocationProvider.swift`: CLLocation + simulation mode.
- `StreamController.swift`: stream orchestration + exponential backoff reconnect.
- `RouteSimulator.swift`: loop route generation for testing.
- `ContentView.swift`: Home, Device List, Settings tabs.
- `GPSBleStreamerTests/PayloadFormattingTests.swift`: formatting tests.

## Pairing + Streaming Steps

1. Install and run app on iPhone.
2. Allow Bluetooth and Location permissions.
3. Enable simulated route (optional).
4. Tap `Start Stream`.
5. On Android receiver app, scan/connect and enable notifications for BEEF characteristic.

## Info.plist Keys

- `NSBluetoothAlwaysUsageDescription`
- `NSLocationWhenInUseUsageDescription`
- `NSLocationAlwaysAndWhenInUseUsageDescription`
- Background modes: `bluetooth-peripheral`, `location`

## Dependencies

Uses only Apple frameworks (no third-party pods/SPM required):

- SwiftUI
- CoreBluetooth
- CoreLocation
- Combine
