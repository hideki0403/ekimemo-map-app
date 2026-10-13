package dev.yukineko.ekimemo_map.data.gpx

import dev.yukineko.ekimemo_map.data.station.LatLng
import org.junit.Assert.assertEquals
import org.junit.Test

class GpxTest {
    private fun read(xml: String) = readGpxTrackPoints(xml.byteInputStream())

    @Test
    fun readsTrackPointsAcrossTracksAndSegments() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
              <wpt lat="1.0" lon="1.0"/>
              <trk>
                <trkseg>
                  <trkpt lat="35.681236" lon="139.767125"><ele>3.0</ele></trkpt>
                  <trkpt lat="35.689487" lon="139.691711"/>
                </trkseg>
                <trkseg>
                  <trkpt lat="35.658034" lon="139.701636"/>
                </trkseg>
              </trk>
              <trk>
                <trkseg>
                  <trkpt lat="35.729503" lon="139.710900"/>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        assertEquals(
            listOf(
                LatLng(35.681236, 139.767125),
                LatLng(35.689487, 139.691711),
                LatLng(35.658034, 139.701636),
                LatLng(35.729503, 139.7109),
            ),
            read(xml),
        )
    }

    @Test
    fun skipsPointsWithoutCoordinates() {
        val xml = """<gpx><trk><trkseg><trkpt lat="35.0"/><trkpt lat="35.0" lon="139.0"/></trkseg></trk></gpx>"""

        assertEquals(listOf(LatLng(35.0, 139.0)), read(xml))
    }
}
