package de.dandit.cartogram.core.context;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dandit.cartogram.core.pub.Region;

public class RegionData {
  private final int[] regionId;
  private final boolean[] regionNaN;
  private final double[] regionPerimeter;
  private final int[][] ringInRegion;
  private final double[] targetArea;
  private final double[][] ringsX;
  private final double[][] ringsY;
  private final double[][] cartogramRingsX;
  private final double[][] cartogramRingsY;
  private final int[][] ringsInPolygonByRegion;
  // TODO make naming consistent: ring, polygon, region,...

  public RegionData(List<Region> regions, int[] polygonId, double[][] ringsX, double[][] ringsY) {
    this.ringsX = ringsX;
    this.ringsY = ringsY;
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
    this.cartogramRingsX = initEmptyCartogramRings(ringsX);
    this.cartogramRingsY = initEmptyCartogramRings(ringsX);
  }

  private static double[][] initEmptyCartogramRings(double[][] rings) {
    int ringCount = rings.length;
    double[][] cartogramRings = new double[ringCount][];
    for (int i = 0; i < ringCount; i++) {
      cartogramRings[i] = new double[rings[i].length];
      Arrays.fill(cartogramRings[i], Double.NaN);
    }
    return cartogramRings;
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

  public double[][] getRingsX() {
    return ringsX;
  }

  public double[][] getRingsY() {
    return ringsY;
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

  public double[][] getCartogramRingsX() {
    return cartogramRingsX;
  }

  public double[][] getCartogramRingsY() {
    return cartogramRingsY;
  }

  public int[][] getRingsInPolygonByRegion() {
    return ringsInPolygonByRegion;
  }
}
