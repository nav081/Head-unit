package com.example.blegps

import com.example.blegps.ble.PayloadParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PayloadParserTest {
    @Test
    fun parsesJsonPayload() {
        val payload = """{"lat":37.1,"lon":-122.2,"alt":5.0,"speed":2.5,"timestamp":1710000000}"""
        val parsed = PayloadParser.parse(payload)
        assertNotNull(parsed)
        assertEquals(37.1, parsed.lat, 0.0001)
    }

    @Test
    fun parsesNmeaPayload() {
        val payload = "\$GPRMC,173102.00,A,3746.494,N,12225.164,W,21.2,0.0,010100,,,A*00"
        val parsed = PayloadParser.parse(payload)
        assertNotNull(parsed)
        assertEquals(-122.4194, parsed.lon, 0.01)
    }
}
