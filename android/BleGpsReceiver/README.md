# Android Headunit Navigator

Android 10+ headunit app that receives iOS-streamed coordinates over BLE and renders in-app navigation with Google Maps.

## Targets

- Kotlin, Android 10+ (API 29+)
- Jetpack Compose UI
- BLE GATT client for notifications

## Gradle Modules

- `app`: launcher activity, runtime permissions, service startup.
- `ble`: BLE GATT receiver service, parser, coordinate bus, source bridge.
- `navigation`: route preview engine abstraction (replaceable with Directions API / Nav SDK).
- `ui`: Compose map, route, connection, and settings flows.

## BLE Contract

- Service: `0000feed-0000-1000-8000-00805f9b34fb`
- Characteristic: `0000beef-0000-1000-8000-00805f9b34fb`
- Expected notifications: UTF-8 JSON or `$GPRMC` NMEA.

## Permissions + Manifest

Includes BLE scan/connect permissions, location permissions, and foreground service permissions for Android 10+.

## Google Maps setup

1. Add your key to `app/src/main/AndroidManifest.xml`:
   `<meta-data android:name="com.google.android.geo.API_KEY" android:value="YOUR_KEY"/>`
2. Enable Maps SDK and billing in Google Cloud.
3. Restrict key by package/signature for release builds.

Directions API integration can replace `CachedDirectionsEngine` in `navigation`.

## Behavior

- BLE stream can be selected as exclusive in-app location source.
- If BLE is disconnected, UI can switch to system GPS mode.
- Safety mode toggle is exposed in settings to reduce update/network load.

## Sample payloads

- JSON: `{"lat":37.7749,"lon":-122.4194,"alt":11.2,"speed":13.4,"timestamp":1710000000}`
- NMEA: `$GPRMC,173102.00,A,3746.494,N,12225.164,W,21.2,0.0,010100,,,A*00`
