package com.example.dronedetector

import org.maplibre.android.maps.Style

object MapStyleProvider {
    fun getOfflineStyleJson(mbtilesPath: String): String {
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
              "id": "park", 
              "type": "fill", 
              "source": "openmaptiles", 
              "source-layer": "landuse", 
              "filter": ["==", "class", "park"], 
              "paint": { "fill-color": "#C8E6C9" } 
            },
            { 
              "id": "roads", 
              "type": "line", 
              "source": "openmaptiles", 
              "source-layer": "transportation", 
              "paint": { 
                "line-color": "#FFFFFF", 
                "line-width": ["interpolate", ["linear"], ["zoom"], 10, 1, 18, 10] 
              } 
            },
            { 
              "id": "buildings", 
              "type": "fill", 
              "source": "openmaptiles", 
              "source-layer": "building", 
              "paint": { "fill-color": "#D9D0C9" } 
            },
            {
              "id": "place-labels",
              "type": "symbol",
              "source": "openmaptiles",
              "source-layer": "place",
              "layout": {
                "text-field": "{name:en}",
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