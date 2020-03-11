package de.dandit.cartogram.core;

import de.dandit.cartogram.core.api.Region;
import de.dandit.cartogram.core.context.PolygonData;
import de.dandit.cartogram.core.context.RegionData;
import de.dandit.cartogram.core.api.Logging;
import de.dandit.cartogram.core.api.MapFeatureData;

import java.util.*;
import java.util.stream.Collectors;


public class PolygonUtilities {
  /**
   * Polygons are considered to have a tiny area if their summed absolute area is smaller than
   * this threshold, scaled to fit assumption that all given polygons in the map fit into a rectangle
   * with a unit area (area=1).
   */
  private static final double AREA_THRESHOLD = 1e-12;

  public static RegionData processMap(Logging logging, MapFeatureData mapData, PolygonData polygonData) {
    polygonData = removeTinyPolygonsInNonLSpace(logging, mapData, polygonData);
    return createRegionData(polygonData);
  }

  // positive for clockwise oriented order, negative for ccw oriented order
  public static double calculateOrientedArea(double[] ringX, double[] ringY) {
    double area = 0.0;
    int pointCount = ringX.length;
    for (int i = 0; i < pointCount - 1; i++) {
      area -= 0.5 * (ringX[i] + ringX[i + 1]) * (ringY[i + 1] - ringY[i]);
    }
    area -= 0.5 * (ringX[pointCount - 1] + ringX[0]) *
      (ringY[0] - ringY[pointCount - 1]);
    return area;
  }

  public static double calculatePolygonPerimeter(double[] ringX, double[] ringY) {
    int pointCount = ringX.length;
    double perimeter = 0.0;
    for (int i = 0; i < pointCount - 1; i++) {
      perimeter += Math.sqrt((ringX[i + 1] - ringX[i]) * (ringX[i + 1] - ringX[i]) +
        (ringY[i + 1] - ringY[i]) * (ringY[i + 1] - ringY[i]));
    }
    return perimeter + Math.sqrt((ringX[0] - ringX[pointCount - 1]) * (ringX[0] - ringX[pointCount - 1]) +
      (ringY[0] - ringY[pointCount - 1]) * (ringY[0] - ringY[pointCount - 1]));
  }

  private static RegionData createRegionData(PolygonData polygonData) {
    return new RegionData(polygonData.getRegionIdByRing(), polygonData.getPolygonRingsX(), polygonData.getPolygonRingsY(), polygonData.getRingsInPolygonByRegion());
  }

  public static PolygonData removeTinyPolygonsInNonLSpace(Logging logging, MapFeatureData mapData, PolygonData polygonData) {
    double[][] ringsX = polygonData.getPolygonRingsX();
    double[][] ringsY = polygonData.getPolygonRingsY();
    int ringCount = ringsX.length;

    double mapMinX = mapData.getMapMinX();
    double mapMinY = mapData.getMapMinY();
    double mapMaxX = mapData.getMapMaxX();
    double mapMaxY = mapData.getMapMaxY();

    boolean[] ringHasTinyArea = new boolean[ringCount];
    double relativeTinyAreaThreshold = AREA_THRESHOLD * (mapMaxX - mapMinX) * (mapMaxY - mapMinY);
    logging.debug("Amount of polygons: {0}", ringCount);
    logging.debug("Relative area threshold: {0}", relativeTinyAreaThreshold);
    for (int ringIndex = 0; ringIndex < ringCount; ringIndex++) {
      double orientedArea = calculateOrientedArea(ringsX[ringIndex], ringsY[ringIndex]);
      double currentArea = Math.abs(orientedArea);
      logging.debug("Polygon {3} (id= {0}) with {1} points has area |{2,number,#.######E0}|",
        polygonData.getRegionIdByRing()[ringIndex], ringsX[ringIndex].length, orientedArea, ringIndex);
      ringHasTinyArea[ringIndex] = currentArea < relativeTinyAreaThreshold;
    }
    int nonTinyRingCount = 0;
    for (int ringIndex = 0; ringIndex < ringCount; ringIndex++) {
      boolean nonTinyArea = !(ringHasTinyArea[ringIndex]);
      if (nonTinyArea) {
        nonTinyRingCount++;
      }
    }
    if (nonTinyRingCount < ringCount) {
      logging.debug("Removing tiny polygons.");

      int[] nonTinyRingsCount = new int[nonTinyRingCount];
      int[] nonTinyRegionIdByRing = new int[nonTinyRingCount];
      nonTinyRingCount = 0;
      int[] regionIdByRing = polygonData.getRegionIdByRing();
      int[][] ringsInPolygonByRegion = polygonData.getRingsInPolygonByRegion();
      Map<Integer, Integer> regionIdToRegionPolygonCount = new HashMap<>(mapData.getRegions().size());
      Map<Integer, Integer> regionIdToRegionIndex = new HashMap<>(mapData.getRegions().size());
      List<List<Integer>> nonTinyRingsInPolygonByRegion = new ArrayList<>(mapData.getRegions().size());
      int currRegionIndex = 0;
      for (Region region : mapData.getRegions()) {
        regionIdToRegionIndex.put(region.getId(), currRegionIndex);
        nonTinyRingsInPolygonByRegion.add(new ArrayList<>());
        currRegionIndex++;
      }

      for (int ringIndex = 0; ringIndex < ringCount; ringIndex++) {
        int regionId = regionIdByRing[ringIndex];
        // assumes order of rings fits order of rings in the ringsInPolygonByRegion mapping but allows for rings of different regions to mix up
        Integer polygonIndex = regionIdToRegionPolygonCount.compute(regionId, (key, old) -> old == null ? 0 : old + 1);
        int regionIndex = regionIdToRegionIndex.get(regionId);
        int[] ringsInPolygon = ringsInPolygonByRegion[regionIndex];
        if (!ringHasTinyArea[ringIndex]) {
          nonTinyRingsInPolygonByRegion.get(regionIndex).add(ringsInPolygon[polygonIndex]);
          nonTinyRingsCount[nonTinyRingCount] = ringsX[ringIndex].length;
          nonTinyRegionIdByRing[nonTinyRingCount] = regionId;
          nonTinyRingCount++;
        }
      }
      double[][] nonTinyRingsX = new double[nonTinyRingCount][];
      double[][] nonTinyRingsY = new double[nonTinyRingCount][];
      for (int ringIndex = 0; ringIndex < nonTinyRingCount; ringIndex++) {
        nonTinyRingsX[ringIndex] = new double[nonTinyRingsCount[ringIndex]];
        nonTinyRingsY[ringIndex] = new double[nonTinyRingsCount[ringIndex]];
      }
      nonTinyRingCount = 0;
      for (int ringIndex = 0; ringIndex < ringCount; ringIndex++) {
        if (!ringHasTinyArea[ringIndex]) {
          for (int pointIndex = 0;
               pointIndex < ringsX[ringIndex].length;
               pointIndex++) {
            nonTinyRingsX[nonTinyRingCount][pointIndex]
              = ringsX[ringIndex][pointIndex];
            nonTinyRingsY[nonTinyRingCount][pointIndex]
                = ringsY[ringIndex][pointIndex];
          }
          nonTinyRingCount++;
        }
      }

      return createOverriddenPolygons(nonTinyRingCount, nonTinyRingsX, nonTinyRingsY, nonTinyRingsCount, nonTinyRegionIdByRing,
        nonTinyRingsInPolygonByRegion.stream()
          .map(a -> a.stream().mapToInt(b -> b).toArray())
          .toArray(int[][]::new));
    }
    return polygonData;
  }

  private static PolygonData createOverriddenPolygons(int nonTinyRingCount, double[][] nonTinyRingX,
                                                      double[][] nonTinyRingY, int[] nonTinyRingsCount, int[] nonTinyRegionIdByRing, int[][] ringsInPolygonByRegion) {
    int[] regionIdByRing = new int[nonTinyRingCount];
    int[] ringCount = new int[nonTinyRingCount];
    for (int ringIndex = 0; ringIndex < nonTinyRingCount; ringIndex++) {
      regionIdByRing[ringIndex] = nonTinyRegionIdByRing[ringIndex];
      ringCount[ringIndex] = nonTinyRingsCount[ringIndex];
    }
    double[][] ringsX = new double[nonTinyRingCount][];
    double[][] ringsY = new double[nonTinyRingCount][];
    for (int ringIndex = 0; ringIndex < nonTinyRingCount; ringIndex++) {
      ringsX[ringIndex] = new double[ringCount[ringIndex]];
      ringsY[ringIndex] = new double[ringCount[ringIndex]];
    }
    for (int ringIndex = 0; ringIndex < nonTinyRingCount; ringIndex++) {
      for (int pointIndex = 0; pointIndex < ringCount[ringIndex]; pointIndex++) {
        ringsX[ringIndex][pointIndex] = nonTinyRingX[ringIndex][pointIndex];
        ringsY[ringIndex][pointIndex] = nonTinyRingY[ringIndex][pointIndex];
      }
    }
    return new PolygonData(ringsX, ringsY, regionIdByRing, ringsInPolygonByRegion);
  }
}
