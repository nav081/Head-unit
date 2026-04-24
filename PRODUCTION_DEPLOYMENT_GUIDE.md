# Production Deployment Guide

## Mock Location Limitation

`LocationManager` test providers require developer-mode style capabilities and are not guaranteed for normal consumer builds. Relying on manual mock-location toggles is fragile in production.

## Recommended Production Strategies

1. **OEM / device-owner provisioning (preferred for head units):**
   - Enroll devices into managed mode.
   - Push app and policy via EMM/MDM.
   - Grant required runtime permissions and background execution allowlists.
   - Standardize BLE pairing process for fleet devices.

2. **Signed system app (platform/OEM partnership):**
   - Install as privileged/system app.
   - Use privileged location injection APIs under OEM governance.
   - Best for deeply integrated infotainment platforms.

3. **In-app navigation fallback:**
   - If global injection is restricted, consume BLE coordinates directly in embedded map/navigation view.

## Security and Reliability Recommendations

- Sign BLE payloads or include message authentication for tamper resistance.
- Add sequence IDs and timestamp drift checks.
- Persist last good coordinate and fail-safe when stream stalls.
- Capture telemetry for reconnect attempts, parse failures, and permission denials.
