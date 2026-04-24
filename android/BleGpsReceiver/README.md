# Android BLE GPS Receiver

Foreground BLE receiver that ingests iOS-streamed coordinates and supports system mock injection or in-app location usage.

## Targets

- Kotlin, Android 10+ (API 29+)
- Jetpack Compose UI
- BLE GATT client for notifications

## Key Modules

- `ble/BleReceiverService.kt`: foreground service + BLE connect/reconnect backoff.
- `ble/PayloadParser.kt`: JSON/NMEA parsing and coordinate validation.
- `location/LocationInjector.kt`: test provider injection + fused fallback.
- `simulation/SimulatedRouteGenerator.kt`: route generation for testing.
- `screens/ReceiverScreens.kt`: Receiver Home, Logs, Settings UI.
- Unit/integration tests under `app/src/test` and `app/src/androidTest`.

## BLE Contract

- Service: `0000feed-0000-1000-8000-00805f9b34fb`
- Characteristic: `0000beef-0000-1000-8000-00805f9b34fb`
- Expected notifications: UTF-8 JSON or `$GPRMC` NMEA.

## Permissions + Manifest

Includes BLE scan/connect permissions, location permissions, and foreground service permissions for Android 10+.

## Provisioning / Injection Notes

### Option A: System-wide location for Maps/Waze

1. Enable developer options.
2. Set this app as mock location app.
3. Start receiver service.
4. Connect to iOS stream; verify Google Maps/Waze follows path.

For production fleet deployments, prefer OEM provisioning (device owner policy) or signed system app approach to avoid manual developer-option dependency.

### Option B: In-app embedded Google Maps navigation

Use parsed coordinates directly in app state and render on Google Maps SDK map/camera without system injection.

## Play Store Policy Notes

- `ACCESS_MOCK_LOCATION` and mock-provider behavior are sensitive; do not misrepresent location use.
- Disclose location handling clearly in Data Safety form.
- Consumer distribution for mock-location tools may be restricted by review context; enterprise deployment is typically safer.

## Sample Payloads

- JSON: `{"lat":37.7749,"lon":-122.4194,"alt":11.2,"speed":13.4,"timestamp":1710000000}`
- NMEA: `$GPRMC,173102.00,A,3746.494,N,12225.164,W,21.2,0.0,010100,,,A*00`
