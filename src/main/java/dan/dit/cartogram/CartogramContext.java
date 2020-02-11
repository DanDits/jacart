package dan.dit.cartogram;

import dan.dit.cartogram.dft.FftPlan2D;
import dan.dit.cartogram.dft.FftPlanFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * For now this holds all previously global variables with the goal to better encapsulate, group and separate them
 * while making sure that initialization order and data flow becomes more obvious and less error prone than in the
 * C program
 */
public class CartogramContext {
    private final FftPlanFactory fftFactory = new FftPlanFactory();
    private int lx;
    private int ly;
    private double[] rho_ft;
    private double[] rho_init;
    private int[][] xyhalfshift2reg;

    private Point[] proj;
    private Point[] proj2;
    private int[] n_polycorn;
    private Point[][] polycorn;
    private int[] polygonId;

    private int[][] polyinreg;
    private double[] target_area;
    private int[] region_id;
    private Map<Integer, Integer> region_id_inv; // using a map instead of an array to support large ids and sparse ids and non positive ids
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

    public void initPoly(int n_poly, int[] n_polycorn, Point[][] polycorn,
                         int[] polygonId) {
        this.n_polycorn = n_polycorn;
        this.polycorn = polycorn;
        this.polygonId = polygonId;
    }

    public void initMapGrid(int lx, int ly) {
        this.lx = lx;
        this.ly = ly;
        this.absoluteTolerance = Math.min(lx, ly) * 1e-6;
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
        int polygonCount = polycorn.length;
        cartcorn = new Point[polygonCount][];
        for (int i = 0; i < polygonCount; i++) {
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
        int regionsCount = regions.size();
        region_id = regions.stream()
                .mapToInt(Region::getId)
                .toArray();
        region_na = new boolean[regionsCount];
        region_perimeter = new double[regionsCount];
    }

    public int[] getRegionId() {
        return region_id;
    }

    public void initInverseRegionId() {
        region_id_inv = new HashMap<>(polygonId.length);
        for (int i = 0; i < region_id.length; i++) {
            region_id_inv.put(region_id[i], i);
        }
    }

    private int regionIdToIndex(int id) {
        return region_id_inv.getOrDefault(id, -1);
    }

    public void initPolyInRegionAssumesPolygonIdAndRegionIdInv() {
        int n_reg = region_id.length;
        polyinreg = new int[n_reg][];
        int last_id = polygonId[0];
        int[] n_polyinreg = new int[n_reg];
        double polygonCount = polycorn.length;
        for (int j = 0; j < polygonCount; j++) {
            if (polygonId[j] != -99999) {
                n_polyinreg[regionIdToIndex(polygonId[j])]++;
                last_id = polygonId[j];
            } else {
                n_polyinreg[regionIdToIndex(last_id)]++;
            }
        }
        for (int j = 0; j < n_reg; j++) {
            polyinreg[j] = new int[n_polyinreg[j]];
        }
        for (int j = 0; j < n_reg; j++) {
            n_polyinreg[j] = 0;
        }
        last_id = polygonId[0];
        for (int j = 0; j < polygonCount; j++) {
            if (polygonId[j] != -99999) {
                int regionIndex = regionIdToIndex(polygonId[j]);
                polyinreg[regionIndex]
                        [n_polyinreg[regionIndex]++] = j;
                last_id = polygonId[j];
            } else {
                int regionIndex = regionIdToIndex(last_id);
                polyinreg[regionIndex]
                        [n_polyinreg[regionIndex]++] = j;
            }
        }

    }

    public void overridePolygons(int n_non_tiny_poly, Point[][] non_tiny_polycorn, int[] n_non_tiny_polycorn, int[] non_tiny_polygon_id) {
        int n_poly = n_non_tiny_poly;
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
        int n_reg = polyinreg.length;
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
