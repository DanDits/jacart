package de.dandit.cartogram.core.context;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dandit.cartogram.core.api.Region;

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

  /*
   * Describes a mapping to know if the ring with a given index is A) an exterior or interior ring
   * and B) to which polygon (by index) the ring belongs. For a single input Polygon with 2 holes (in this order) this would be [-1,0,0]
   * with negative values i marking an exterior ring of the polygon with index -(i+1).
   */
  private final int[][] ringsInPolygonByRegion;

  public RegionData(int[] regionIdByRing, double[][] ringsX, double[][] ringsY, int[][] ringsInPolygonByRegion) {
    this.ringsX = ringsX;
    this.ringsY = ringsY;
    this.ringsInPolygonByRegion = Arrays.stream(ringsInPolygonByRegion)
      .filter(r -> r.length > 0)
      .toArray(int[][]::new);
    int regionsCount = this.ringsInPolygonByRegion.length;
    this.regionId = Arrays.stream(regionIdByRing)
      .distinct()
      .toArray();

    // using a map instead of an array to support large ids and sparse ids and non positive ids
    Map<Integer, Integer> regionIdInv = new HashMap<>(regionId.length);
    for (int i = 0; i < regionId.length; i++) {
      regionIdInv.put(regionId[i], i);
    }
    this.regionNaN = new boolean[regionsCount];
    this.regionPerimeter = new double[regionsCount];
    this.ringInRegion = initPolygonInRegions(regionIdInv, regionId, regionIdByRing);
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

  private static int[][] initPolygonInRegions(Map<Integer, Integer> regionIdInverse, int[] regionId, int[] regionIdByRing) {
    int regionCount = regionId.length;
    int[][] ringsInRegion = new int[regionCount][];
    int lastId = regionIdByRing[0];
    int[] ringsInRegionCount = new int[regionCount];
    double ringCount = regionIdByRing.length;
    for (int currentId : regionIdByRing) {
      if (currentId != Integer.MIN_VALUE) {
        ringsInRegionCount[regionIdToIndex(regionIdInverse, currentId)]++;
        lastId = currentId;
      } else {
        ringsInRegionCount[regionIdToIndex(regionIdInverse, lastId)]++;
      }
    }
    for (int j = 0; j < regionCount; j++) {
      ringsInRegion[j] = new int[ringsInRegionCount[j]];
    }
    for (int j = 0; j < regionCount; j++) {
      ringsInRegionCount[j] = 0;
    }
    lastId = regionIdByRing[0];
    for (int j = 0; j < ringCount; j++) {
      if (regionIdByRing[j] != Integer.MIN_VALUE) {
        int regionIndex = regionIdToIndex(regionIdInverse, regionIdByRing[j]);
        ringsInRegion[regionIndex]
          [ringsInRegionCount[regionIndex]++] = j;
        lastId = regionIdByRing[j];
      } else {
        int regionIndex = regionIdToIndex(regionIdInverse, lastId);
        ringsInRegion[regionIndex]
          [ringsInRegionCount[regionIndex]++] = j;
      }
    }
    return ringsInRegion;
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

  public int[][] getRingsInRegion() {
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
