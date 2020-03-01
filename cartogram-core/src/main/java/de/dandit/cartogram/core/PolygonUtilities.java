package de.dandit.cartogram.core;

import de.dandit.cartogram.core.context.PolygonData;
import de.dandit.cartogram.core.context.RegionData;
import de.dandit.cartogram.core.pub.Logging;
import de.dandit.cartogram.core.pub.MapFeatureData;


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
  public static double calculateOrientedArea(double[] ringX, double[] ringY) {
    double area = 0.0;
    int pointCount = ringX.length;
    for (int i = 0; i < pointCount - 1; i++) {
      area -= 0.5 * (ringX[i] + ringX[i + 1]) * (ringY[i + 1] - ringY[i]);
    }
    area -= 0.5 * (ringX[pointCount - 1] + ringX[0]) *
      (ringY[0] - ringY[pointCount - 1]);
    return area;
  }

  public static double calculatePolygonPerimeter(double[] ringX, double[] ringY) {
    int pointCount = ringX.length;
    double perimeter = 0.0;
    for (int i = 0; i < pointCount - 1; i++) {
      perimeter += Math.sqrt((ringX[i + 1] - ringX[i]) * (ringX[i + 1] - ringX[i]) +
        (ringY[i + 1] - ringY[i]) * (ringY[i + 1] - ringY[i]));
    }
    return perimeter + Math.sqrt((ringX[0] - ringX[pointCount - 1]) * (ringX[0] - ringX[pointCount - 1]) +
      (ringY[0] - ringY[pointCount - 1]) * (ringY[0] - ringY[pointCount - 1]));
  }

  private static RegionData make_region(MapFeatureData mapData, PolygonData polygonData) {
    return new RegionData(mapData.getRegions(), polygonData.getPolygonId(), polygonData.getPolygonRingsX(), polygonData.getPolygonRingsY());
  }

  public static PolygonData removeTinyPolygonsInNonLSpace(Logging logging, MapFeatureData mapData, PolygonData polygonData) {
    double[][] ringsX = polygonData.getPolygonRingsX();
    double[][] ringsY = polygonData.getPolygonRingsY();
    int ringCount = ringsX.length;

    double map_maxx = mapData.getMap_maxx();
    double map_maxy = mapData.getMap_maxy();
    double map_minx = mapData.getMap_minx();
    double map_miny = mapData.getMap_miny();

    boolean[] ringHasTinyArea = new boolean[ringCount];
    double relativeTinyAreaThreshold = AREA_THRESHOLD * (map_maxx - map_minx) * (map_maxy - map_miny);
    logging.debug("Amount of polygons: {0}", ringCount);
    logging.debug("Relative area threshold: {0}", relativeTinyAreaThreshold);
    for (int ringIndex = 0; ringIndex < ringCount; ringIndex++) {
      double orientedArea = calculateOrientedArea(ringsX[ringIndex], ringsY[ringIndex]);
      double currentArea = Math.abs(orientedArea);
      logging.debug("Polygon {3} (id= {0}) with {1} points has area |{2,number,#.######E0}|",
        polygonData.getPolygonId()[ringIndex], ringsX[ringIndex].length, orientedArea, ringIndex);
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
          n_non_tiny_polycorn[nonTinyRingCount] = ringsX[poly_indx].length;
          non_tiny_polygon_id[nonTinyRingCount] = polygon_id[poly_indx];
          nonTinyRingCount++;
        }
      }
      double[][] non_tiny_polycornX = new double[nonTinyRingCount][];
      double[][] non_tiny_polycornY = new double[nonTinyRingCount][];
      for (int poly_indx = 0; poly_indx < nonTinyRingCount; poly_indx++) {
        non_tiny_polycornX[poly_indx] = new double[n_non_tiny_polycorn[poly_indx]];
        non_tiny_polycornY[poly_indx] = new double[n_non_tiny_polycorn[poly_indx]];
      }
      nonTinyRingCount = 0;
      for (int poly_indx = 0; poly_indx < ringCount; poly_indx++) {
        if (!ringHasTinyArea[poly_indx]) {
          for (int corn_indx = 0;
               corn_indx < ringsX[poly_indx].length;
               corn_indx++) {
            non_tiny_polycornX[nonTinyRingCount][corn_indx]
              = ringsX[poly_indx][corn_indx];
            non_tiny_polycornY[nonTinyRingCount][corn_indx]
                = ringsY[poly_indx][corn_indx];
          }
          nonTinyRingCount++;
        }
      }


      return createOverridenPolygons(nonTinyRingCount, non_tiny_polycornX, non_tiny_polycornY, n_non_tiny_polycorn, non_tiny_polygon_id);
    }
    return polygonData;
  }

  private static PolygonData createOverridenPolygons(int n_non_tiny_poly, double[][] non_tiny_polycornX, double[][] non_tiny_polycornY,
                                                     int[] n_non_tiny_polycorn, int[] non_tiny_polygon_id) {
    int[] polygonId = new int[n_non_tiny_poly];
    int[] n_polycorn = new int[n_non_tiny_poly];
    for (int poly_indx = 0; poly_indx < n_non_tiny_poly; poly_indx++) {
      polygonId[poly_indx] = non_tiny_polygon_id[poly_indx];
      n_polycorn[poly_indx] = n_non_tiny_polycorn[poly_indx];
    }
    double[][] polycornX = new double[n_non_tiny_poly][];
    double[][] polycornY = new double[n_non_tiny_poly][];
    for (int poly_indx = 0; poly_indx < n_non_tiny_poly; poly_indx++) {
      polycornX[poly_indx] = new double[n_polycorn[poly_indx]];
      polycornY[poly_indx] = new double[n_polycorn[poly_indx]];
    }
    for (int poly_indx = 0; poly_indx < n_non_tiny_poly; poly_indx++) {
      for (int corn_indx = 0; corn_indx < n_polycorn[poly_indx]; corn_indx++) {
        polycornX[poly_indx][corn_indx] = non_tiny_polycornX[poly_indx][corn_indx];
        polycornY[poly_indx][corn_indx] = non_tiny_polycornY[poly_indx][corn_indx];
      }
    }
    return new PolygonData(polycornX, polycornY, polygonId);
  }
}
