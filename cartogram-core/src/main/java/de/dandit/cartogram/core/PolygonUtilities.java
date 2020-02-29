package de.dandit.cartogram.core;

import de.dandit.cartogram.core.pub.MapFeatureData;
import de.dandit.cartogram.core.context.Point;
import de.dandit.cartogram.core.context.PolygonData;
import de.dandit.cartogram.core.context.RegionData;
import de.dandit.cartogram.core.pub.Logging;


public class PolygonUtilities {
  /**
   * Polygons are considered to have a tiny area if their summed absolute area is smaller than
   * this threshold, scaled to fit assumption that all given polygons in the map fit into a rectangle
   * with a unit area (area=1).
   */
  private static final double AREA_THRESHOLD = 1e-12;

  public static RegionData processMap(Logging logging, MapFeatureData mapData, PolygonData polygonData) {
    polygonData = removeTinyPolygonsInNonLSpace(logging, mapData, polygonData);
    return make_region(mapData, polygonData);
  }

  // positive for clockwise oriented order, negative for ccw oriented order
  public static double calculateOrientedArea(Point[] polygon) {
    double area = 0.0;
    int pointCount = polygon.length;
    for (int i = 0; i < pointCount - 1; i++) {
      area -= 0.5 * (polygon[i].x + polygon[i + 1].x) * (polygon[i + 1].y - polygon[i].y);
    }
    area -= 0.5 * (polygon[pointCount - 1].x + polygon[0].x) *
      (polygon[0].y - polygon[pointCount - 1].y);
    return area;
  }

  public static double calculatePolygonPerimeter(Point[] polygon) {
    int pointCount = polygon.length;
    double perimeter = 0.0;
    for (int i = 0; i < pointCount - 1; i++) {
      perimeter += Math.sqrt((polygon[i + 1].x - polygon[i].x) * (polygon[i + 1].x - polygon[i].x) +
        (polygon[i + 1].y - polygon[i].y) * (polygon[i + 1].y - polygon[i].y));
    }
    return perimeter + Math.sqrt((polygon[0].x - polygon[pointCount - 1].x) * (polygon[0].x - polygon[pointCount - 1].x) +
      (polygon[0].y - polygon[pointCount - 1].y) * (polygon[0].y - polygon[pointCount - 1].y));
  }

  private static RegionData make_region(MapFeatureData mapData, PolygonData polygonData) {
    return new RegionData(mapData.getRegions(), polygonData.getPolygonId(), polygonData.getPolygonRings());
  }

  public static PolygonData removeTinyPolygonsInNonLSpace(Logging logging, MapFeatureData mapData, PolygonData polygonData) {
    Point[][] rings = polygonData.getPolygonRings();
    int ringCount = rings.length;

    double map_maxx = mapData.getMap_maxx();
    double map_maxy = mapData.getMap_maxy();
    double map_minx = mapData.getMap_minx();
    double map_miny = mapData.getMap_miny();

    boolean[] ringHasTinyArea = new boolean[ringCount];
    double relativeTinyAreaThreshold = AREA_THRESHOLD * (map_maxx - map_minx) * (map_maxy - map_miny);
    logging.debug("Amount of polygons: {0}", ringCount);
    logging.debug("Relative area threshold: {0}", relativeTinyAreaThreshold);
    for (int ringIndex = 0; ringIndex < ringCount; ringIndex++) {
      double orientedArea = calculateOrientedArea(rings[ringIndex]);
      double currentArea = Math.abs(orientedArea);
      logging.debug("Polygon {3} (id= {0}) with {1} points has area |{2,number,#.######E0}|",
        polygonData.getPolygonId()[ringIndex], rings[ringIndex].length, orientedArea, ringIndex);
      ringHasTinyArea[ringIndex] = currentArea < relativeTinyAreaThreshold;
    }
    int nonTinyRingCount = 0;
    for (int ringIndex = 0; ringIndex < ringCount; ringIndex++) {
      boolean nonTinyArea = !(ringHasTinyArea[ringIndex]);
      if (nonTinyArea) {
        nonTinyRingCount++;
      }
    }
    if (nonTinyRingCount < ringCount) {
      logging.debug("Removing tiny polygons.");

      int[] n_non_tiny_polycorn = new int[nonTinyRingCount];
      int[] non_tiny_polygon_id = new int[nonTinyRingCount];
      nonTinyRingCount = 0;
      int[] polygon_id = polygonData.getPolygonId();
      for (int poly_indx = 0; poly_indx < ringCount; poly_indx++) {
        if (!ringHasTinyArea[poly_indx]) {
          n_non_tiny_polycorn[nonTinyRingCount] = rings[poly_indx].length;
          non_tiny_polygon_id[nonTinyRingCount] = polygon_id[poly_indx];
          nonTinyRingCount++;
        }
      }
      Point[][] non_tiny_polycorn = new Point[nonTinyRingCount][];
      for (int poly_indx = 0; poly_indx < nonTinyRingCount; poly_indx++) {
        non_tiny_polycorn[poly_indx] = new Point[n_non_tiny_polycorn[poly_indx]];
      }
      nonTinyRingCount = 0;
      for (int poly_indx = 0; poly_indx < ringCount; poly_indx++) {
        if (!ringHasTinyArea[poly_indx]) {
          for (int corn_indx = 0;
               corn_indx < rings[poly_indx].length;
               corn_indx++) {
            non_tiny_polycorn[nonTinyRingCount][corn_indx]
              = rings[poly_indx][corn_indx].createCopy();
          }
          nonTinyRingCount++;
        }
      }


      return createOverridenPolygons(nonTinyRingCount, non_tiny_polycorn, n_non_tiny_polycorn, non_tiny_polygon_id);
    }
    return polygonData;
  }

  private static PolygonData createOverridenPolygons(int n_non_tiny_poly, Point[][] non_tiny_polycorn,
                                                     int[] n_non_tiny_polycorn, int[] non_tiny_polygon_id) {
    int[] polygonId = new int[n_non_tiny_poly];
    int[] n_polycorn = new int[n_non_tiny_poly];
    for (int poly_indx = 0; poly_indx < n_non_tiny_poly; poly_indx++) {
      polygonId[poly_indx] = non_tiny_polygon_id[poly_indx];
      n_polycorn[poly_indx] = n_non_tiny_polycorn[poly_indx];
    }
    Point[][] polycorn = new Point[n_non_tiny_poly][];
    for (int poly_indx = 0; poly_indx < n_non_tiny_poly; poly_indx++) {
      polycorn[poly_indx] = new Point[n_polycorn[poly_indx]];
    }
    for (int poly_indx = 0; poly_indx < n_non_tiny_poly; poly_indx++) {
      for (int corn_indx = 0; corn_indx < n_polycorn[poly_indx]; corn_indx++) {
        polycorn[poly_indx][corn_indx] =
          non_tiny_polycorn[poly_indx][corn_indx].createCopy();
      }
    }
    return new PolygonData(polycorn, polygonId);
  }
}
