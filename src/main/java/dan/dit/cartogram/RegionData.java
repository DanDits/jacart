package dan.dit.cartogram;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionData {
  private final int[] region_id;
  private final boolean[] region_na;
  private final double[] region_perimeter;
  // using a map instead of an array to support large ids and sparse ids and non positive ids
  private final Map<Integer, Integer> region_id_inv;
  private final int[][] polyinreg;
  private final double[] cartogramArea;
  private final double[] areaError;
  private final double[] target_area;
  private final Point[][] polycorn;
  private final Point[][] cartcorn;

  public RegionData(List<Region> regions, int[] polygonId, Point[][] polycorn) {
    this.polycorn = polycorn;
    int regionsCount = regions.size();
    this.region_id = regions.stream()
      .mapToInt(Region::getId)
      .toArray();
    this.region_id_inv = new HashMap<>(region_id.length);
    for (int i = 0; i < region_id.length; i++) {
      region_id_inv.put(region_id[i], i);
    }
    this.region_na = new boolean[regionsCount];
    this.region_perimeter = new double[regionsCount];
    this.polyinreg = initPolygonInRegions(region_id_inv, region_id, polygonId);
    this.cartogramArea = new double[regionsCount];
    this.areaError = new double[regionsCount];
    this.target_area = new double[regionsCount];
    this.cartcorn = initCartcorn(polycorn);
  }

  private static Point[][] initCartcorn(Point[][] polycorn) {
    int polygonCount = polycorn.length;
    Point[][] cartcorn = new Point[polygonCount][];
    for (int i = 0; i < polygonCount; i++) {
      cartcorn[i] = new Point[polycorn[i].length];
      for (int j = 0; j < cartcorn[i].length; j++) {
        cartcorn[i][j] = new Point(Double.NaN, Double.NaN);
      }
    }
    return cartcorn;
  }

  private static int regionIdToIndex(Map<Integer, Integer> region_id_inv, int id) {
    return region_id_inv.getOrDefault(id, -1);
  }

  private static int[][] initPolygonInRegions(Map<Integer, Integer> region_id_inv, int[] region_id, int[] polygonId) {
    int n_reg = region_id.length;
    int[][] polyinreg = new int[n_reg][];
    int last_id = polygonId[0];
    int[] n_polyinreg = new int[n_reg];
    double polygonCount = polygonId.length;
    for (int j = 0; j < polygonCount; j++) {
      if (polygonId[j] != -99999) {
        n_polyinreg[regionIdToIndex(region_id_inv, polygonId[j])]++;
        last_id = polygonId[j];
      } else {
        n_polyinreg[regionIdToIndex(region_id_inv, last_id)]++;
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
        int regionIndex = regionIdToIndex(region_id_inv, polygonId[j]);
        polyinreg[regionIndex]
          [n_polyinreg[regionIndex]++] = j;
        last_id = polygonId[j];
      } else {
        int regionIndex = regionIdToIndex(region_id_inv, last_id);
        polyinreg[regionIndex]
          [n_polyinreg[regionIndex]++] = j;
      }
    }
    return polyinreg;
  }

  public Point[][] getPolycorn() {
    return polycorn;
  }

  public int[] getRegionId() {
    return region_id;
  }

  public int[] getRegion_id() {
    return region_id;
  }

  public boolean[] getRegion_na() {
    return region_na;
  }

  public double[] getRegion_perimeter() {
    return region_perimeter;
  }

  public Map<Integer, Integer> getRegion_id_inv() {
    return region_id_inv;
  }

  public int[][] getPolyinreg() {
    return polyinreg;
  }

  public double[] getCartogramArea() {
    return cartogramArea;
  }

  public double[] getAreaError() {
    return areaError;
  }

  public double[] getTarget_area() {
    return target_area;
  }

  public Point[][] getCartcorn() {
    return cartcorn;
  }
}
