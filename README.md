# BLE GPS Streamer Monorepo

This repository contains:

- `ios/GPSBleStreamer`: iOS BLE GPS transmitter app (SwiftUI/CoreBluetooth).
- `android/BleGpsReceiver`: Android BLE GPS receiver app (Compose/BLE/location injector).

## End-to-End Flow

1. iOS app reads GPS (`CoreLocation`) and publishes notifications over BLE GATT (`FEED/BEEF` UUIDs).
2. Android app reads BLE notifications and parses JSON or `$GPRMC`.
3. Android app either injects to mock provider (system apps like Maps/Waze can consume) or keeps location in-app for embedded navigation.

## Acceptance Tests

1. **BLE discovery:** Android finds iOS peripheral advertising `FEED`.
2. **Notification stream:** Android receives payloads at expected frequency.
3. **Payload parsing:** JSON and NMEA parse into valid coordinates.
4. **Validation guardrails:** Invalid lat/lon are rejected and logged.
5. **Reconnection:** Disabling/re-enabling Bluetooth reconnects via exponential backoff.
6. **Google Maps behavior:** with mock provider enabled, map marker/route follows streamed trajectory.

## Manual Test Checklist (Google Maps / Waze)

- [ ] Enable Android Developer Options and choose this app as mock location app.
- [ ] Start Android app and confirm foreground notification channel is active.
- [ ] Start iOS app, grant permissions, start stream (JSON mode).
- [ ] Open Google Maps and monitor blue dot movement.
- [ ] Switch iOS payload to NMEA and confirm movement still updates.
- [ ] Toggle iOS simulated route and verify smooth loop movement.
- [ ] Turn iOS Bluetooth off/on; verify Android reconnects automatically.
- [ ] Turn Android Bluetooth off/on; verify reconnect after backoff.

## Build

- iOS: open `ios/GPSBleStreamer` in Xcode and run tests.
- Android: from `android/BleGpsReceiver`, run `./gradlew test connectedAndroidTest`.
