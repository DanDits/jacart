package de.dandit.cartogram.core.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionData {
  private final int[] regionId;
  private final boolean[] regionNaN;
  private final double[] regionPerimeter;
  private final int[][] ringInRegion;
  private final double[] targetArea;
  private final Point[][] rings;
  private final Point[][] cartogramRings;
  private final int[][] ringsInPolygonByRegion;
  // TODO make naming consistent: ring, polygon, region,...

  public RegionData(List<Region> regions, int[] polygonId, Point[][] rings) {
    this.rings = rings;
    this.ringsInPolygonByRegion = regions.stream()
      .map(Region::getRingsInPolygons)
      .toArray(int[][]::new);
    int regionsCount = regions.size();
    this.regionId = regions.stream()
      .mapToInt(Region::getId)
      .toArray();

    // using a map instead of an array to support large ids and sparse ids and non positive ids
    Map<Integer, Integer> regionIdInv = new HashMap<>(regionId.length);
    for (int i = 0; i < regionId.length; i++) {
      regionIdInv.put(regionId[i], i);
    }
    this.regionNaN = new boolean[regionsCount];
    this.regionPerimeter = new double[regionsCount];
    this.ringInRegion = initPolygonInRegions(regionIdInv, regionId, polygonId);
    this.targetArea = new double[regionsCount];
    this.cartogramRings = initCartcorn(rings);
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

  private static int regionIdToIndex(Map<Integer, Integer> regionIdInverse, int id) {
    return regionIdInverse.getOrDefault(id, -1);
  }

  private static int[][] initPolygonInRegions(Map<Integer, Integer> regionIdInverse, int[] regionId, int[] polygonId) {
    int regionCount = regionId.length;
    int[][] polyinreg = new int[regionCount][];
    int lastId = polygonId[0];
    int[] ringsInRegionCount = new int[regionCount];
    double polygonCount = polygonId.length;
    for (int i : polygonId) {
      if (i != -99999) {
        ringsInRegionCount[regionIdToIndex(regionIdInverse, i)]++;
        lastId = i;
      } else {
        ringsInRegionCount[regionIdToIndex(regionIdInverse, lastId)]++;
      }
    }
    for (int j = 0; j < regionCount; j++) {
      polyinreg[j] = new int[ringsInRegionCount[j]];
    }
    for (int j = 0; j < regionCount; j++) {
      ringsInRegionCount[j] = 0;
    }
    lastId = polygonId[0];
    for (int j = 0; j < polygonCount; j++) {
      if (polygonId[j] != -99999) {
        int regionIndex = regionIdToIndex(regionIdInverse, polygonId[j]);
        polyinreg[regionIndex]
          [ringsInRegionCount[regionIndex]++] = j;
        lastId = polygonId[j];
      } else {
        int regionIndex = regionIdToIndex(regionIdInverse, lastId);
        polyinreg[regionIndex]
          [ringsInRegionCount[regionIndex]++] = j;
      }
    }
    return polyinreg;
  }

  public Point[][] getRings() {
    return rings;
  }

  public int[] getRegionId() {
    return regionId;
  }

  public boolean[] getRegionNaN() {
    return regionNaN;
  }

  public double[] getRegionPerimeter() {
    return regionPerimeter;
  }

  public int[][] getRingInRegion() {
    return ringInRegion;
  }

  public double[] getTargetArea() {
    return targetArea;
  }

  public Point[][] getCartogramRings() {
    return cartogramRings;
  }

  public int[][] getRingsInPolygonByRegion() {
    return ringsInPolygonByRegion;
  }
}
