package dan.dit.cartogram;

import java.text.MessageFormat;


public class Polygon {
    /* We remove areas less than AREA_THRESHOLD * (area of bounding box). */
    private static final double AREA_THRESHOLD = 1e-12;

    public static void processMap(MapFeatureData mapData, CartogramContext context) {
        remove_tiny_polygons_in_nonLSpace(mapData, context);
        make_region(mapData, context);
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

    /*****************************************************************************/
    /* Function to make regions from polygons. Region IDs in the .gen file must  */
    /* be nonnegative.
     * Is required to correctly handle MultiPolygon geometries to ensure that their collective area
     * is considered. Another option could be to split the contribution to each sub-polygon depending on its
     * relative area.                                */
    private static void make_region(MapFeatureData mapData, CartogramContext context) {
        context.initRegions(mapData.getRegions());
        context.initInverseRegionId();
        context.initPolyInRegionAssumesPolygonIdAndRegionIdInv();
        printDebug("Amount of regions: " + context.getRegionId().length);
    }

    public static void remove_tiny_polygons_in_nonLSpace(MapFeatureData mapData, CartogramContext context) {

        Point[][] polycorn = context.getPolycorn();
        int n_poly = polycorn.length;

        double map_maxx = mapData.getMap_maxx();
        double map_maxy = mapData.getMap_maxy();
        double map_minx = mapData.getMap_minx();
        double map_miny = mapData.getMap_miny();

        boolean[] poly_has_tiny_area = new boolean[n_poly];
        double relativeTinyAreaThreshold = AREA_THRESHOLD * (map_maxx - map_minx) * (map_maxy - map_miny);
        printDebug("Amount of polygons: " + n_poly);
        printDebug("Relative area threshold: " + relativeTinyAreaThreshold);
        for (int poly_indx = 0; poly_indx < n_poly; poly_indx++) {
            double current_area = Math.abs(polygon_area(polycorn[poly_indx]));
            printDebug(MessageFormat.format("Polygon {3} (id= {0}) with {1} points has area {2,number,#.######E0}", context.getPolygonId()[poly_indx], polycorn[poly_indx].length, current_area, poly_indx));
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
            printDebug("Removing tiny polygons.\n");

            int[] n_non_tiny_polycorn = new int[n_non_tiny_poly];
            int[] non_tiny_polygon_id = new int[n_non_tiny_poly];
            n_non_tiny_poly = 0;
            int[] polygon_id = context.getPolygonId();
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

            context.overridePolygons(n_non_tiny_poly, non_tiny_polycorn, n_non_tiny_polycorn, non_tiny_polygon_id);
        }
    }

    private static void printDebug(String text) {
        System.out.println(text);
    }
}
