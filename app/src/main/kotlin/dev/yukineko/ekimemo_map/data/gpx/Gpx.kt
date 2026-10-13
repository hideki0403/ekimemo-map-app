package dev.yukineko.ekimemo_map.data.gpx

import dev.yukineko.ekimemo_map.data.station.LatLng
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

fun readGpxTrackPoints(input: InputStream): List<LatLng> {
    val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
    val points = factory.newDocumentBuilder().parse(input).getElementsByTagNameNS("*", "trkpt")
    return (0 until points.length).mapNotNull { i ->
        val point = points.item(i) as Element
        val lat = point.getAttribute("lat").toDoubleOrNull() ?: return@mapNotNull null
        val lng = point.getAttribute("lon").toDoubleOrNull() ?: return@mapNotNull null
        LatLng(lat, lng)
    }
}
