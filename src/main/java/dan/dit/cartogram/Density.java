package dan.dit.cartogram;

import dan.dit.cartogram.dft.FftPlan2D;
import dan.dit.cartogram.dft.FftPlanFactory;

import java.text.MessageFormat;
import java.util.List;

import static dan.dit.cartogram.Integrate.displayDoubleArray;
import static dan.dit.cartogram.Integrate.displayIntArray;

public class Density {
    /**
     * Defines a threshold for the resulting cartogram areas: The maximum error of each region
     * must only differ by that percentage. The error hereby is defined by how much the cartogram region's relative area
     * differs from the region's relative target area: For example if region A should accumulate 20%
     * of the total area mass but currently accumulates 30%, the error would be 0.2/0.3-1=0.66-1=-0.33 which is 33%
     */
    public static final double MAX_PERMITTED_AREA_ERROR = 0.01;

    /**
     * Defines the grid's resolution. Must be a multiple of 2.
     */
    public static final int L = 512;

    /**
     * Defines a padding for placing the initial polygons within the grid.
     */
    private static final double PADDING = 1.5;

    private static final double BLUR_WIDTH = 5e0;

    /**
     * Defines a factor to replace a target area of 0 with.
     * Will use the positive minimum target area multiplied by this factor.
     */
    private static final double MIN_POP_FAC = 0.2;
    private static final double MIN_PERIMETER_FAC = 0.025;
    private final CartogramContext context;

    public Density(CartogramContext context) {
        this.context = context;
    }

    void initPolycornAndPolygonId(MapFeatureData featureData) {
        List<Region> regions = featureData.getRegions();
        int n_poly = 0;
        for (Region region : regions) {
            int polys = region.getHullCoordinates().length;
            n_poly += polys;
        }

        int[] n_polycorn = new int[n_poly];
        Point[][] polycorn = new Point[n_poly][];
        int poly_counter = 0;
        int[] polygonId = new int[n_poly];
        for (Region region : regions) {
            for (Point[] hullCoordinates : region.getHullCoordinates()) {
                n_polycorn[poly_counter] = hullCoordinates.length;
                polycorn[poly_counter] = hullCoordinates;
                polygonId[poly_counter] = region.getId();
                poly_counter++;
            }
        }
        context.initPoly(n_poly, n_polycorn, polycorn, polygonId);
    }

    void transformMapToLSpace(MapFeatureData featureData, boolean inv) {
        double latt_const, new_maxx, new_maxy, new_minx, new_miny;
        int i, j;

        int lx, ly;
        double map_minx = featureData.getMap_minx();
        double map_miny = featureData.getMap_miny();
        double map_maxx = featureData.getMap_maxx();
        double map_maxy = featureData.getMap_maxy();

        new_maxx = 0.5 * ((1.0 + PADDING) * map_maxx + (1.0 - PADDING) * map_minx);
        new_minx = 0.5 * ((1.0 - PADDING) * map_maxx + (1.0 + PADDING) * map_minx);
        new_maxy = 0.5 * ((1.0 + PADDING) * map_maxy + (1.0 - PADDING) * map_miny);
        new_miny = 0.5 * ((1.0 - PADDING) * map_maxy + (1.0 + PADDING) * map_miny);

        // retain aspect ratio, setting either lx or ly to the maximum L
        if (map_maxx - map_minx > map_maxy - map_miny) {
            lx = L;
            latt_const = (new_maxx - new_minx) / L;
            ly = 1 << ((int) Math.ceil(Math.log((new_maxy - new_miny) / latt_const) / Math.log(2)));
            new_maxy = 0.5 * (map_maxy + map_miny) + 0.5 * ly * latt_const;
            new_miny = 0.5 * (map_maxy + map_miny) - 0.5 * ly * latt_const;
        } else {
            ly = L;
            latt_const = (new_maxy - new_miny) / L;
            lx = 1 << ((int) Math.ceil(Math.log((new_maxx - new_minx) / latt_const) / Math.log(2)));
            new_maxx = 0.5 * (map_maxx + map_minx) + 0.5 * lx * latt_const;
            new_minx = 0.5 * (map_maxx + map_minx) - 0.5 * lx * latt_const;
        }
        printError(
                "Using a {0} x {1} lattice with bounding box\n\t({2} {3} {4} {5}).\n",
                lx, ly, new_minx, new_miny, new_maxx, new_maxy);

        int n_poly = context.getN_poly();
        int[] n_polycorn = context.getN_polycorn();
        Point[][] polycorn = context.getPolycorn();
        for (i = 0; i < n_poly; i++) {
            for (j = 0; j < n_polycorn[i]; j++) {
                polycorn[i][j].x = (polycorn[i][j].x - new_minx) / latt_const;
                polycorn[i][j].y = (polycorn[i][j].y - new_miny) / latt_const;
            }
        }

        Point[][] origcorn = null;
        if (inv) {
            origcorn = new Point[n_poly][];
            for (i = 0; i < n_poly; i++) {
                origcorn[i] = new Point[n_polycorn[i]];
            }
            for (i = 0; i < n_poly; i++) {
                for (j = 0; j < n_polycorn[i]; j++) {
                    origcorn[i][j] = polycorn[i][j].createCopy();
                }
            }
        }

        context.initMapGrid(lx, ly);
        context.initOriginalPolygon(origcorn);
    }

    private void printError(String s, Object... parameters) {
        System.err.println(MessageFormat.format(s, parameters));
    }

    public static void set_inside_values_for_polygon(int region, int n_polycorn,
                                                     Point[] polycorn, int[][] inside) {
        double poly_minx = polycorn[0].x;
        int k, n;

        for (k = 0; k < n_polycorn; k++) {
            poly_minx = Math.min(poly_minx, polycorn[k].x);
        }

        for (k = 0, n = n_polycorn - 1; k < n_polycorn; n = k++) {
            set_inside_values_between_points(region, polycorn[k], polycorn[n],
                    poly_minx, inside);
        }
    }

    private static void set_inside_values_between_points(int region, Point pk, Point pn,
                                                         double poly_minx, int[][] inside) {
        for (int l = (int) Math.ceil(Math.min(pn.y, pk.y) - 0.5); l < Math.max(pn.y - 0.5, pk.y - 0.5); l++) {
            set_inside_value_at_y(region, pk, pn, l, poly_minx, inside);
        }
    }

    private static void set_inside_value_at_y(int region, Point pk, Point pn, int l,
                                              double poly_minx, int[][] inside) {
        double intersection = (pn.x - 0.5 - (pk.x - 0.5)) * (l - (pk.y - 0.5)) /
                (pn.y - 0.5 - (pk.y - 0.5)) + (pk.x - 0.5);
        for (int m = (int) poly_minx; m < intersection; m++) {
            inside[m][l] = region - inside[m][l] - 1;
        }
    }

    boolean fill_with_density1(MapFeatureData featureData, CartogramConfig config) {
        double area, avg_dens, tot_init_area, tot_target_area;
        double[] dens, init_area;
        int i, id, j;
        boolean eps = config.isEps();

        initPolycornAndPolygonId(featureData);
        Polygon.processMap(featureData, context);
        transformMapToLSpace(featureData, config.isInv());
        context.initArea();

        int n_reg = context.getN_reg();
        double[] target_area = context.getTarget_area();
        boolean[] region_na = context.getRegionNa();
        if (n_reg == 1) {

            target_area[0] = 1.0;
            return true;
        }

        context.initXyHalfShift2Reg();
        dens = new double[n_reg];
        init_area = new double[n_reg];

        interior();

        double[] featureTargetArea = featureData.getTargetAreaPerRegion();

        for (i = 0; i < n_reg; i++) {
            target_area[i] = featureTargetArea[i];
            if (target_area[i] == -2.0) {
                region_na[i] = true;
            }
        }

        int[] region_id = context.getRegionId();
        for (i = 0; i < n_reg; i++) {
            if (target_area[i] < 0.0 && target_area[i] != -2.0) {
                throw new IllegalArgumentException(
                        MessageFormat.format("ERROR: No target area for region {0}", region_id[i]));
            }
        }
        displayDoubleArray("target_area", target_area);

        int na_ctr = 0;
        double tmp_tot_target_area = 0.0;
        tot_init_area = 0.0;
        int[] n_polyinreg = context.getN_polyinreg();
        int[] n_polycorn = context.getN_polycorn();
        int[][] polyinreg = context.getPolyinreg();
        Point[][] polycorn = context.getPolycorn();
        double[] region_perimeter = context.getRegionPerimeter();
        for (i = 0; i < n_reg; i++) {
            if (region_na[i]) {
                na_ctr++;
            } else {
                tmp_tot_target_area += target_area[i];
            }
            for (j = 0; j < n_polyinreg[i]; j++) {
                init_area[i] += Polygon.polygon_area(n_polycorn[polyinreg[i][j]],
                        polycorn[polyinreg[i][j]]);
            }
            tot_init_area += init_area[i];
        }
        System.out.println("Total init area= " + tot_init_area);
        displayDoubleArray("init_area", init_area);

        for (i = 0; i < n_reg; i++) {
            for (j = 0; j < n_polyinreg[i]; j++) {
                region_perimeter[i] += Polygon.polygon_perimeter(n_polycorn[polyinreg[i][j]],
                        polycorn[polyinreg[i][j]]);
            }
        }
        displayDoubleArray("region perimeter", region_perimeter);
        int first_region = 1;
        double total_NA_ratio = 0;

        for (i = 0; i < n_reg; i++) {
            if (region_na[i]) {
                total_NA_ratio += init_area[i] / tot_init_area;
            }
        }

        double total_NA_area = (total_NA_ratio * tmp_tot_target_area) / (1 - total_NA_ratio);
        tmp_tot_target_area += total_NA_area;

        for (i = 0; i < n_reg; i++) {
            if (region_na[i]) {
                if (first_region == 1) {
                    printError("\nSetting area for NA regions:\n");
                    first_region = 0;
                }
                target_area[i] = (init_area[i] / tot_init_area) / total_NA_ratio * total_NA_area;
                printError("{0}: {1}", region_id[i], target_area[i]);
            }
        }

        printError("\n");

        if (config.isUsePerimeterThreshold()) {
            printError("Note: Enlarging extremely small regions using scaled perimeter threshold.");
            boolean[] region_small = new boolean[n_reg];
            int region_small_ctr = 0;
            double[] region_threshold, region_threshold_area;
            double tot_region_small_area = 0, total_perimeter = 0, total_threshold = 0;
            region_threshold = new double[n_reg];
            region_threshold_area = new double[n_reg];
            for (i = 0; i < n_reg; i++) {
                total_perimeter += region_perimeter[i];
            }
            for (i = 0; i < n_reg; i++) {
                region_threshold[i] = Math.max((region_perimeter[i] / total_perimeter) * MIN_PERIMETER_FAC, 0.00025);
                if ((target_area[i] / tmp_tot_target_area < region_threshold[i])) {
                    region_small[i] = true;
                    region_small_ctr++;
                    tot_region_small_area += target_area[i];
                }
            }
            for (i = 0; i < n_reg; i++) {
                if (region_small[i]) {
                    total_threshold += region_threshold[i];
                }
            }
            double total_threshold_area = (total_threshold * (tmp_tot_target_area - tot_region_small_area)) / (1 - total_threshold);

            if (region_small_ctr > 0) {
                printError("Enlarging small regions:\n");
            }

            for (i = 0; i < n_reg; i++) {
                if (region_small[i]) {
                    region_threshold_area[i] = (region_threshold[i] / total_threshold) * total_threshold_area;
                    double old_target_area = target_area[i];
                    target_area[i] = region_threshold_area[i];
                    tmp_tot_target_area += target_area[i];
                    tmp_tot_target_area -= old_target_area;
                    printError("{0}: {1}", region_id[i], target_area[i]);
                }
            }
            if (region_small_ctr > 0) {
                printError("\n");
            } else {
                printError("No regions below minimum threshold.\n\n"); // TODO what about washington DC? all its polygons are removed, use the min perimeter config?
            }
        } else {
            printError("Note: Not using scaled perimeter threshold.\n\n");
            double min_area = target_area[0];
            for (i = 1; i < n_reg; i++) {
                if (target_area[i] > 0.0) {
                    if (min_area <= 0.0) {
                        min_area = target_area[i];
                    } else {
                        min_area = Math.min(min_area, target_area[i]);
                    }
                }
            }
            for (i = 0; i < n_reg; i++) {
                if (target_area[i] == 0.0) {
                    target_area[i] = MIN_POP_FAC * min_area;
                }
            }
        }
        displayDoubleArray("target_area ___!___", target_area);

        for (i = 0; i < n_reg; i++) {
            dens[i] = target_area[i] / init_area[i];
        }
        displayDoubleArray("dens", dens);

        for (i = 0, tot_target_area = 0.0; i < n_reg; i++) {
            tot_target_area += target_area[i];
        }
        avg_dens = tot_target_area / tot_init_area;

        context.initRho();

        int lx = context.getLx();
        int ly = context.getLy();
        int[][] xyhalfshift2reg = context.getXyHalfShift2Reg();
        double[] rho_init = context.getRho_init();
        double[] rho_ft = context.getRho_ft();
        for (i = 0; i < lx; i++) {
            for (j = 0; j < ly; j++) {
                if (xyhalfshift2reg[i][j] == -1)
                    rho_init[i * ly + j] = avg_dens;
                else
                    rho_init[i * ly + j] = dens[xyhalfshift2reg[i][j]];
            }
        }
        displayIntArray("xyhalfshift2reg[0]", xyhalfshift2reg[0]);
        displayDoubleArray("(beforeblur) rho_init", rho_init);
        displayDoubleArray("(beforeblur) rho_ft", rho_ft);

        context.initFwdPlan(lx, ly);

        gaussian_blur(tot_init_area, avg_dens);

        context.getPlan_fwd().execute();
        displayDoubleArray("(afterblur) rho_init", rho_init);
        displayDoubleArray("(afterblur) rho_ft", rho_ft);

        return false;
    }

    void fill_with_density2() {
        double avg_dens, tot_target_area, tot_tmp_area;
        double[] dens, tmp_area;
        int i, j;

        int n_poly = context.getN_poly();
        int[] n_polycorn = context.getN_polycorn();
        Point[][] polycorn = context.getPolycorn();
        Point[][] cartcorn = context.getCartcorn();
        double[] rho_init = context.getRho_init();
        int lx = context.getLx();
        int ly = context.getLy();

        for (i = 0; i < n_poly; i++) {
            for (j = 0; j < n_polycorn[i]; j++) {
                polycorn[i][j] = cartcorn[i][j];
            }
        }

        context.initXyHalfShift2Reg();

        int n_reg = context.getN_reg();
        int[] n_polyinreg = context.getN_polyinreg();
        int[][] polyinreg = context.getPolyinreg();
        double[] target_area = context.getTarget_area();
        int[][] xyhalfshift2reg = context.getXyHalfShift2Reg();

        dens = new double[n_reg];
        tmp_area = new double[n_reg];

        interior();

        for (i = 0; i < n_reg; i++)
            for (j = 0; j < n_polyinreg[i]; j++)
                tmp_area[i] +=
                        Polygon.polygon_area(n_polycorn[polyinreg[i][j]], polycorn[polyinreg[i][j]]);
        for (i = 0; i < n_reg; i++) dens[i] = target_area[i] / tmp_area[i];

        for (i = 0, tot_tmp_area = 0.0; i < n_reg; i++)
            tot_tmp_area += tmp_area[i];
        for (i = 0, tot_target_area = 0.0; i < n_reg; i++)
            tot_target_area += target_area[i];
        avg_dens = tot_target_area / tot_tmp_area;

        for (i = 0; i < lx; i++)
            for (j = 0; j < ly; j++) {
                if (xyhalfshift2reg[i][j] == -1)
                    rho_init[i * ly + j] = avg_dens;
                else
                    rho_init[i * ly + j] = dens[xyhalfshift2reg[i][j]];
            }
        context.getPlan_fwd().execute();
    }

    private void interior() {
        int i, j, poly;

        int lx = context.getLx();
        int ly = context.getLy();
        int[] n_polycorn = context.getN_polycorn();
        Point[][] polycorn = context.getPolycorn();
        int[][] xyhalfshift2reg = context.getXyHalfShift2Reg();
        int n_reg = context.getN_reg();
        int[] n_polyinreg = context.getN_polyinreg();
        int[][] polyinreg = context.getPolyinreg();
        for (i = 0; i < lx; i++) {
            for (j = 0; j < ly; j++) {
                xyhalfshift2reg[i][j] = -1;
            }
        }
        displayIntArray("xyhalfshift2reg[0] before init", xyhalfshift2reg[0]);

        for (i = 0; i < n_reg; i++) {
            for (j = 0; j < n_polyinreg[i]; j++) {
                poly = polyinreg[i][j];
                set_inside_values_for_polygon(i, n_polycorn[poly], polycorn[poly],
                        xyhalfshift2reg);
            }
        }
        displayIntArray("xyhalfshift2reg[0]", xyhalfshift2reg[0]);
    }

    void gaussian_blur(double tot_init_area, double avg_dens) {
        FftPlan2D plan_bwd;
        int lx = context.getLx();
        int ly = context.getLy();
        double[] rho_init = context.getRho_init();
        double[] rho_ft = context.getRho_ft();

        plan_bwd = new FftPlanFactory().createDCT3_2D(lx, ly, rho_ft, rho_init);

        for (int i = 0; i < lx * ly; i++)
            rho_init[i] /= 4 * lx * ly;

        context.getPlan_fwd().execute();

        gaussianBlur(lx, ly, rho_ft);
        plan_bwd.execute();

    }

    private void gaussianBlur(int lx, int ly, double[] rho_ft) {
        double prefactor;
        int i;
        double scale_i;
        int j;
        double scale_j;
        prefactor = -0.5 * BLUR_WIDTH * BLUR_WIDTH * Math.PI * Math.PI;
        for (i = 0; i < lx; i++) {
            scale_i = (double) i / lx;
            for (j = 0; j < ly; j++) {
                scale_j = (double) j / ly;
                rho_ft[i * ly + j] *=
                        Math.exp(prefactor * (scale_i * scale_i + scale_j * scale_j));
            }
        }
    }
}
