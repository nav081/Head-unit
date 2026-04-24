import XCTest
import CoreLocation
@testable import GPSBleStreamer

final class PayloadFormattingTests: XCTestCase {
    func testJSONPayloadContainsLatLon() throws {
        let location = CLLocation(
            coordinate: .init(latitude: 12.34, longitude: 56.78),
            altitude: 123,
            horizontalAccuracy: 3,
            verticalAccuracy: 4,
            timestamp: .init(timeIntervalSince1970: 1710000000)
        )
        let data = PayloadFormatter.jsonData(from: location)
        let payload = try JSONDecoder().decode(CoordinatePayload.self, from: data)
        XCTAssertEqual(payload.lat, 12.34, accuracy: 0.0001)
        XCTAssertEqual(payload.lon, 56.78, accuracy: 0.0001)
    }

    func testNMEAPayloadStartsWithGPRMC() {
        let location = CLLocation(
            coordinate: .init(latitude: 12.34, longitude: 56.78),
            altitude: 123,
            horizontalAccuracy: 3,
            verticalAccuracy: 4,
            timestamp: .init(timeIntervalSince1970: 1710000000)
        )
        let sentence = String(data: PayloadFormatter.nmeaData(from: location), encoding: .utf8)
        XCTAssertTrue(sentence?.hasPrefix("$GPRMC") == true)
    }
}
