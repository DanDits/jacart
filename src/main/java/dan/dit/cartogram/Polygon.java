package dan.dit.cartogram;

import java.text.MessageFormat;


public class Polygon {
    /* We remove areas less than AREA_THRESHOLD * (area of bounding box). */
    private static final double AREA_THRESHOLD = 1e-12;

    public static void processMap(MapFeatureData mapData, CartogramContext context) {
        remove_tiny_polygons_in_nonLSpace(mapData, context);
        make_region(mapData, context);
    }

    /*****************************************************************************/
    /* Function to determine polygon area. This is needed to remove polygons     */
    /* zero area and determine the average population.                            */
    /* The problem in short is to find the area of a polygon whose vertices are  */
    /* given. Recall Stokes' theorem in 3d for a vector field v:                 */
    /* integral[around closed curve dA]v(x,y,z).ds =                             */
    /*                                          integral[over area A]curl(v).dA. */
    /* Now let v(x,y,z) = (0,Q(x,y),0) and dA = (0,0,dx*dy). Then                */
    /* integral[around closed curve dA]Q(x,y)dy =                                */
    /*                                         integral[over area A]dQ/dx*dx*dy. */
    /* If Q = x:                                                                 */
    /* A = integral[over area A]dx*dy = integral[around closed curve dA]x dy.    */
    /* For every edge from (x[i],y[i]) to (x[i+1],y[i+1]) there is a             */
    /* parametrization                                                           */
    /* (x(t),y(t)) = ((1-t)x[i]+t*x[i+1],(1-t)y[i]+t*y[i+1]), 0<t<1              */
    /* so that the path integral along this edge is                              */
    /* int[from 0 to 1]{(1-t)x[i]+t*x[i+1]}(y[i+1]-y[i])dt =                     */
    /*                                          0.5*(y[i+1]-y[i])*(x[i]+x[i+1]). */
    /* Summing over all edges yields:                                            */
    /* Area = 0.5*[(x[0]+x[1])(y[1]-y[0]) + (x[1]+x[2])(y[2]-y[1]) + ...         */
    /*               ... + (x[n-1]+x[n])(y[n]-y[n-1]) + (x[n]+x[0])(y[0]-y[n])]. */
    /* ArcGIS treats a clockwise direction as positive, so that there is an      */
    /* additional minus sign.                                                    */
    public static double polygon_area(int ncrns, Point[] polygon) {
        double area = 0.0;
        int i;

        for (i = 0; i < ncrns - 1; i++) {
            area -=
                    0.5 * (polygon[i].x + polygon[i + 1].x) * (polygon[i + 1].y - polygon[i].y);
        }
        area -= 0.5 * (polygon[ncrns - 1].x + polygon[0].x) *
                (polygon[0].y - polygon[ncrns - 1].y);
        return area;
    }

    public static double polygon_perimeter(int ncrns, Point[] polygon) {
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
    }


/*****************************************************************************/
    /********************* Function to remove tiny polygons. *********************/

    public static void remove_tiny_polygons_in_nonLSpace(MapFeatureData mapData, CartogramContext context) {

        /* Find out whether there are any tiny polygons. */
        int n_poly = context.getN_poly();
        int[] n_polycorn = context.getN_polycorn();
        Point[][] polycorn = context.getPolycorn();

        double map_maxx = mapData.getMap_maxx();
        double map_maxy = mapData.getMap_maxy();
        double map_minx = mapData.getMap_minx();
        double map_miny = mapData.getMap_miny();

        boolean[] poly_has_tiny_area = new boolean[n_poly];
        for (int poly_indx = 0; poly_indx < n_poly; poly_indx++) {
            double current_area = Math.abs(polygon_area(n_polycorn[poly_indx], polycorn[poly_indx]));
            printDebug(MessageFormat.format("Polygon (id= {0}) with {1} points has area {2,number,#.######E0}", context.getPolygonId()[poly_indx], n_polycorn[poly_indx], current_area));
            poly_has_tiny_area[poly_indx] =
                    (current_area <
                            AREA_THRESHOLD * (map_maxx - map_minx) * (map_maxy - map_miny));
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

            /* If there are tiny polygons, we replace the original polygons by the   */
            /* subset of non-tiny polygons.                                          */

            int[] n_non_tiny_polycorn = new int[n_non_tiny_poly];
            int[] non_tiny_polygon_id = new int[n_non_tiny_poly];
            n_non_tiny_poly = 0;
            int[] polygon_id = context.getPolygonId();
            for (int poly_indx = 0; poly_indx < n_poly; poly_indx++) {
                if (!poly_has_tiny_area[poly_indx]) {
                    n_non_tiny_polycorn[n_non_tiny_poly] = n_polycorn[poly_indx];
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
                         corn_indx < n_polycorn[poly_indx];
                         corn_indx++) {
                        non_tiny_polycorn[n_non_tiny_poly][corn_indx]
                                = polycorn[poly_indx][corn_indx].createCopy();
                    }
                    n_non_tiny_poly++;
                }
            }

            /* Copy the non-tiny polygons to the variables used by the original      */
            /* polygons.                                                             */
            context.overridePolygons(n_non_tiny_poly, non_tiny_polycorn, n_non_tiny_polycorn, non_tiny_polygon_id);
        }
    }

    private static void printDebug(String text) {
        System.out.println(text);
    }
}
