package dan.dit.cartogram;

import dan.dit.cartogram.dft.FftPlan2D;
import dan.dit.cartogram.dft.FftPlanFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * For now this holds all previously global variables with the goal to better encapsulate, group and separate them
 * while making sure that initialization order and data flow becomes more obvious and less error prone than in the
 * C program
 */
public class CartogramContext {
    private final FftPlanFactory fftFactory = new FftPlanFactory();
    // related to density
    private int lx;
    private int ly;
    private double[] rho_ft;
    private double[] rho_init;
    private int[][] xyhalfshift2reg;

    private Point[] proj;
    private Point[] proj2;
    private int n_poly;
    private int[] n_polycorn;
    private Point[][] polycorn;
    private int[] polygonId;

    private Point[][] origcorn; // original parsed (and rescaled) polygon
    private int n_reg; // amount of regions
    private int[] n_polyinreg; // the amount of polygons within a region
    private int[][] polyinreg;
    private int last_id;
    private double[] target_area;
    private int[] region_id;
    private int[] region_id_inv;
    private boolean[] region_na;
    private double[] region_perimeter;

    private Point[][] cartcorn;
    private FftPlan2D gridvx;
    private FftPlan2D gridvy;
    private FftPlan2D rho;

    private int integration;
    private double[] areaError;
    private double[] cartogramArea;
    private FftPlan2D grid_fluxx_init;
    private FftPlan2D grid_fluxy_init;
    private FftPlan2D plan_fwd;
    private double absoluteTolerance;

    public void initPoly(int lx, int ly, int n_poly, int[] n_polycorn, Point[][] polycorn, Point[][] origcorn,
                         int[] polygonId) {
        this.lx = lx;
        this.ly = ly;
        this.absoluteTolerance = Math.min(lx, ly) * 1e-6;
        this.n_poly = n_poly;
        this.n_polycorn = n_polycorn;
        this.polycorn = polycorn;
        this.origcorn = origcorn;
        this.polygonId = polygonId;
    }

    public double ABS_TOL() {
        return absoluteTolerance;
    }

    public double CONV_MAX_CHANGE() {
        return Math.min(lx, ly) * 1e-9;
    }

    public int getLx() {
        return lx;
    }

    public int getLy() {
        return ly;
    }

    public FftPlan2D getRho() {
        return rho;
    }

    public int getN_poly() {
        return n_poly;
    }

    public int[] getN_polycorn() {
        return n_polycorn;
    }

    public Point[][] getPolycorn() {
        return polycorn;
    }

    public double[] getRho_init() {
        return rho_init;
    }

    public double[] getRho_ft() {
        return rho_ft;
    }

    public FftPlan2D getGridvx() {
        return gridvx;
    }

    public FftPlan2D getGridvy() {
        return gridvy;
    }

    public Point[] getProj() {
        return proj;
    }

    public void initGrid() {
        gridvx = fftFactory.createDCT3_2D(lx, ly);
        gridvy = fftFactory.createDCT3_2D(lx, ly);
    }

    public Point[] initProj() {
        proj = new Point[lx * ly];
        for (int i = 0; i < lx; i++) {
            for (int j = 0; j < ly; j++) {
                proj[i * ly + j] = new Point(i + 0.5, j + 0.5);
            }
        }
        return getProj();
    }

    public Point[][] initCartcorn() {
        cartcorn = new Point[n_poly][];
        for (int i = 0; i < n_poly; i++) {
            cartcorn[i] = new Point[n_polycorn[i]];
            for (int j = 0; j < n_polycorn[i]; j++) {
                cartcorn[i][j] = new Point(Double.NaN, Double.NaN);
            }
        }
        return getCartcorn();
    }

    public Point[][] getCartcorn() {
        return cartcorn;
    }

    public Point[] initProj2() {
        proj2 = new Point[lx * ly];
        for (int i = 0; i < proj2.length; i++) {
            proj2[i] = new Point(Double.NaN, Double.NaN);
        }
        return getProj2();
    }

    public Point[] getProj2() {
        return proj2;
    }

    public int getN_reg() {
        return n_reg;
    }

    public int[] getN_polyinreg() {
        return n_polyinreg;
    }

    public int[][] getPolyinreg() {
        return polyinreg;
    }

    public double[] getTarget_area() {
        return target_area;
    }

    public void initIntegration() {
        integration = 1;
    }

    public int onIntegration() {
        integration++;
        return integration;
    }

    public double[] getAreaError() {
        return areaError;
    }

    public double[] getCartogramArea() {
        return cartogramArea;
    }

    public int[] getPolygonId() {
        return polygonId;
    }

    public void initRegions(List<Region> regions) {
        this.n_reg = regions.size();
        region_id = regions.stream()
                .mapToInt(Region::getId)
                .toArray();
        region_na = new boolean[n_reg];
        region_perimeter = new double[n_reg];
    }

    public int[] getRegionId() {
        return region_id;
    }

    public void initInverseRegionId() {
        int max_id = IntStream.of(polygonId)
                .max()
                .orElseThrow();
        region_id_inv = new int[max_id + 1];
        Arrays.fill(region_id_inv, -1);
        for (int i = 0; i < n_reg; i++) {
            region_id_inv[region_id[i]] = i;
        }
    }

    public void initPolyInRegion() {
        n_polyinreg = new int[n_reg];
        polyinreg = new int[n_reg][];
        last_id = polygonId[0];
        for (int j = 0; j < n_poly; j++) {
            if (polygonId[j] != -99999) {
                n_polyinreg[region_id_inv[polygonId[j]]]++;
                last_id = polygonId[j];
            } else {
                n_polyinreg[region_id_inv[last_id]]++;
            }
        }
        for (int j = 0; j < n_reg; j++) {
            polyinreg[j] = new int[n_polyinreg[j]];
        }
        for (int j = 0; j < n_reg; j++) {
            n_polyinreg[j] = 0;
        }
        last_id = polygonId[0];
        for (int j = 0; j < n_poly; j++) {
            if (polygonId[j] != -99999) {
                polyinreg[region_id_inv[polygonId[j]]]
                        [n_polyinreg[region_id_inv[polygonId[j]]]++] = j;
                last_id = polygonId[j];
            } else {
                polyinreg[region_id_inv[last_id]]
                        [n_polyinreg[region_id_inv[last_id]]++] = j;
            }
        }

    }

    public void overridePolygons(int n_non_tiny_poly, Point[][] non_tiny_polycorn, int[] n_non_tiny_polycorn, int[] non_tiny_polygon_id) {
        n_poly = n_non_tiny_poly;
        polygonId = new int[n_poly];
        n_polycorn = new int[n_poly];
        for (int poly_indx = 0; poly_indx < n_poly; poly_indx++) {
            polygonId[poly_indx] = non_tiny_polygon_id[poly_indx];
            n_polycorn[poly_indx] = n_non_tiny_polycorn[poly_indx];
        }
        polycorn = new Point[n_poly][];
        for (int poly_indx = 0; poly_indx < n_poly; poly_indx++) {
            polycorn[poly_indx] = new Point[n_polycorn[poly_indx]];
        }
        for (int poly_indx = 0; poly_indx < n_poly; poly_indx++) {
            for (int corn_indx = 0; corn_indx < n_polycorn[poly_indx]; corn_indx++) {
                polycorn[poly_indx][corn_indx] =
                        non_tiny_polycorn[poly_indx][corn_indx].createCopy();
            }
        }
    }

    public void initArea() {
        cartogramArea = new double[n_reg];
        areaError = new double[n_reg];
        target_area = new double[n_reg];
    }

    public int[][] getXyHalfShift2Reg() {
        return xyhalfshift2reg;
    }

    public void initXyHalfShift2Reg() {
        xyhalfshift2reg = new int[lx][ly];
    }

    public void initRho() {
        rho_ft = new double[lx * ly];
        rho_init = new double[lx * ly];
    }

    public double[] getRegionPerimeter() {
        return region_perimeter;
    }

    public boolean[] getRegionNa() {
        return region_na;
    }

    public FftPlan2D getGrid_fluxx_init() {
        return grid_fluxx_init;
    }

    public FftPlan2D getGrid_fluxy_init() {
        return grid_fluxy_init;
    }

    public void initFluxInitPlan() {
        grid_fluxx_init = fftFactory.createDCT3_2D(lx, ly);
        grid_fluxy_init = fftFactory.createDCT3_2D(lx, ly);
    }

    public void initFwdPlan(int lx, int ly) {
        plan_fwd = fftFactory.createDCT2_2D(lx, ly, rho_init, rho_ft);
    }

    public FftPlan2D getPlan_fwd() {
        return plan_fwd;
    }

    public void initRhoPlan() {
        rho = fftFactory.createDCT3_2D(lx, ly);
    }
}
