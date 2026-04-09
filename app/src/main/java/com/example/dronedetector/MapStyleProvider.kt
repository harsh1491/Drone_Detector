package com.example.dronedetector

object MapStyleProvider {
    fun getOfflineStyleJson(mbtilesPath: String): String {
        // We use a raw string and ensure no special characters break the formatting
        return """
        {
          "version": 8,
          "sources": {
            "openmaptiles": {
              "type": "vector",
              "url": "mbtiles://$mbtilesPath"
            }
          },
          "glyphs": "asset://fonts/{fontstack}/{range}.pbf",
          "layers": [
            {
              "id": "background",
              "type": "background",
              "paint": { "background-color": "#F8F4F0" }
            },
            {
              "id": "water",
              "type": "fill",
              "source": "openmaptiles",
              "source-layer": "water",
              "paint": { "fill-color": "#A0C8F0" }
            },
            {
              "id": "landcover",
              "type": "fill",
              "source": "openmaptiles",
              "source-layer": "landcover",
              "paint": { "fill-color": "#E0E0E0" }
            },
            {
              "id": "roads",
              "type": "line",
              "source": "openmaptiles",
              "source-layer": "transportation",
              "paint": {
                "line-color": "#FFFFFF",
                "line-width": 2
              }
            },
            {
              "id": "place-labels",
              "type": "symbol",
              "source": "openmaptiles",
              "source-layer": "place",
              "layout": {
                "text-field": "{name}",
                "text-font": ["Open Sans Regular"],
                "text-size": 14
              },
              "paint": {
                "text-color": "#333333",
                "text-halo-color": "#FFFFFF",
                "text-halo-width": 1
              }
            }
          ]
        }
        """.trimIndent()
    }
}