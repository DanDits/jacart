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
    private Point[][] polycorn;
    private int[] polygonId;

    private int[][] polyinreg;
    private double[] target_area;
    private int[] region_id;
    private Map<Integer, Integer> region_id_inv; // using a map instead of an array to support large ids and sparse ids and non positive ids
    private boolean[] region_na;
    private double[] region_perimeter;

    private Point[][] cartcorn;

    private int integration;
    private double[] areaError;
    private double[] cartogramArea;
    private MapGrid mapGrid;

    public void initPoly(Point[][] polycorn,
                         int[] polygonId) {
        this.polycorn = polycorn;
        this.polygonId = polygonId;
    }

    public double ABS_TOL() {
        return mapGrid.getAbsoluteTolerance();
    }

    public double CONV_MAX_CHANGE() {
        return mapGrid.getConvergenceMaxChangeThreshold();
    }

    public int getLx() {
        return mapGrid.getLx();
    }

    public int getLy() {
        return mapGrid.getLy();
    }

    public FftPlan2D getRho() {
        return mapGrid.getRho();
    }

    public Point[][] getPolycorn() {
        return polycorn;
    }

    public double[] getRho_init() {
        return mapGrid.getRho_init();
    }

    public double[] getRho_ft() {
        return mapGrid.getRho_ft();
    }

    public FftPlan2D getGridvx() {
        return mapGrid.getGridvx();
    }

    public FftPlan2D getGridvy() {
        return mapGrid.getGridvy();
    }

    public Point[] getProj() {
        return mapGrid.getProj();
    }

    public Point[][] initCartcorn() {
        int polygonCount = polycorn.length;
        cartcorn = new Point[polygonCount][];
        for (int i = 0; i < polygonCount; i++) {
            cartcorn[i] = new Point[polycorn[i].length];
            for (int j = 0; j < cartcorn[i].length; j++) {
                cartcorn[i][j] = new Point(Double.NaN, Double.NaN);
            }
        }
        return getCartcorn();
    }

    public Point[][] getCartcorn() {
        return cartcorn;
    }

    public Point[] getProj2() {
        return mapGrid.getProj2();
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
        int[] n_polycorn = new int[n_poly];
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
        return mapGrid.getXyhalfshift2reg();
    }

    public double[] getRegionPerimeter() {
        return region_perimeter;
    }

    public boolean[] getRegionNa() {
        return region_na;
    }

    public FftPlan2D getGrid_fluxx_init() {
        return mapGrid.getGrid_fluxx_init();
    }

    public FftPlan2D getGrid_fluxy_init() {
        return mapGrid.getGrid_fluxy_init();
    }

    public FftPlan2D getPlan_fwd() {
        return mapGrid.getPlan_fwd();
    }

    public void setMapGrid(MapGrid mapGrid) {
        this.mapGrid = mapGrid;
    }
}
