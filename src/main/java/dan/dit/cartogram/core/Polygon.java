package dan.dit.cartogram.core;

import dan.dit.cartogram.core.context.MapFeatureData;
import dan.dit.cartogram.core.context.Point;
import dan.dit.cartogram.core.context.PolygonData;
import dan.dit.cartogram.core.context.RegionData;
import dan.dit.cartogram.core.pub.Logging;


public class Polygon {
  private static final double AREA_THRESHOLD = 1e-12;

  public static RegionData processMap(Logging logging, MapFeatureData mapData, PolygonData polygonData) {
    polygonData = remove_tiny_polygons_in_nonLSpace(logging, mapData, polygonData);
    return make_region(mapData, polygonData);
  }

  public static double polygon_area(Point[] polygon) {
    double area = 0.0;
    int pointCount = polygon.length;
    for (int i = 0; i < pointCount - 1; i++) {
      area -= 0.5 * (polygon[i].x + polygon[i + 1].x) * (polygon[i + 1].y - polygon[i].y);
    }
    area -= 0.5 * (polygon[pointCount - 1].x + polygon[0].x) *
      (polygon[0].y - polygon[pointCount - 1].y);
    return area;
  }

  public static double polygon_perimeter(Point[] polygon) {
    int ncrns = polygon.length;
    double perimeter = 0.0;
    for (int i = 0; i < ncrns - 1; i++) {
      perimeter += Math.sqrt((polygon[i + 1].x - polygon[i].x) * (polygon[i + 1].x - polygon[i].x) +
        (polygon[i + 1].y - polygon[i].y) * (polygon[i + 1].y - polygon[i].y));
    }
    return perimeter + Math.sqrt((polygon[0].x - polygon[ncrns - 1].x) * (polygon[0].x - polygon[ncrns - 1].x) +
      (polygon[0].y - polygon[ncrns - 1].y) * (polygon[0].y - polygon[ncrns - 1].y));
  }

  private static RegionData make_region(MapFeatureData mapData, PolygonData polygonData) {
    return new RegionData(mapData.getRegions(), polygonData.getPolygonId(), polygonData.getPolycorn());
  }

  public static PolygonData remove_tiny_polygons_in_nonLSpace(Logging logging, MapFeatureData mapData, PolygonData polygonData) {

    Point[][] polycorn = polygonData.getPolycorn();
    int n_poly = polycorn.length;

    double map_maxx = mapData.getMap_maxx();
    double map_maxy = mapData.getMap_maxy();
    double map_minx = mapData.getMap_minx();
    double map_miny = mapData.getMap_miny();

    boolean[] poly_has_tiny_area = new boolean[n_poly];
    double relativeTinyAreaThreshold = AREA_THRESHOLD * (map_maxx - map_minx) * (map_maxy - map_miny);
    logging.debug("Amount of polygons: {0}", n_poly);
    logging.debug("Relative area threshold: {0}", relativeTinyAreaThreshold);
    for (int poly_indx = 0; poly_indx < n_poly; poly_indx++) {
      double current_area = Math.abs(polygon_area(polycorn[poly_indx]));
      logging.debug("Polygon {3} (id= {0}) with {1} points has area {2,number,#.######E0}",
        polygonData.getPolygonId()[poly_indx], polycorn[poly_indx].length, current_area, poly_indx);
      poly_has_tiny_area[poly_indx] =
        (current_area <
          relativeTinyAreaThreshold);
    }
    int n_non_tiny_poly = 0;
    for (int poly_indx = 0; poly_indx < n_poly; poly_indx++) {
      boolean nonTinyArea = !(poly_has_tiny_area[poly_indx]);
      if (nonTinyArea) {
        n_non_tiny_poly++;
      }
    }
    if (n_non_tiny_poly < n_poly) {
      logging.debug("Removing tiny polygons.");

      int[] n_non_tiny_polycorn = new int[n_non_tiny_poly];
      int[] non_tiny_polygon_id = new int[n_non_tiny_poly];
      n_non_tiny_poly = 0;
      int[] polygon_id = polygonData.getPolygonId();
      for (int poly_indx = 0; poly_indx < n_poly; poly_indx++) {
        if (!poly_has_tiny_area[poly_indx]) {
          n_non_tiny_polycorn[n_non_tiny_poly] = polycorn[poly_indx].length;
          non_tiny_polygon_id[n_non_tiny_poly] = polygon_id[poly_indx];
          n_non_tiny_poly++;
        }
      }
      Point[][] non_tiny_polycorn = new Point[n_non_tiny_poly][];
      for (int poly_indx = 0; poly_indx < n_non_tiny_poly; poly_indx++) {
        non_tiny_polycorn[poly_indx] = new Point[n_non_tiny_polycorn[poly_indx]];
      }
      n_non_tiny_poly = 0;
      for (int poly_indx = 0; poly_indx < n_poly; poly_indx++) {
        if (!poly_has_tiny_area[poly_indx]) {
          for (int corn_indx = 0;
               corn_indx < polycorn[poly_indx].length;
               corn_indx++) {
            non_tiny_polycorn[n_non_tiny_poly][corn_indx]
              = polycorn[poly_indx][corn_indx].createCopy();
          }
          n_non_tiny_poly++;
        }
      }


      return createOverridenPolygons(n_non_tiny_poly, non_tiny_polycorn, n_non_tiny_polycorn, non_tiny_polygon_id);
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
