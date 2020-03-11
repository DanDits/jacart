package de.dandit.cartogram.core.context;

public class PolygonData {
  private final double[][] polygonRingsX;
  private final double[][] polygonRingsY;
  private final int[] regionIdByRing;
  private final int[][] ringsInPolygonByRegion;

  public PolygonData(double[][] polygonRingsX, double[][] polygonRingsY, int[] regionIdByRing, int[][] ringsInPolygonByRegion) {
    this.polygonRingsX = polygonRingsX;
    this.polygonRingsY = polygonRingsY;
    this.regionIdByRing = regionIdByRing;
    this.ringsInPolygonByRegion = ringsInPolygonByRegion;
  }

  public double[][] getPolygonRingsX() {
    return polygonRingsX;
  }

  public double[][] getPolygonRingsY() {
    return polygonRingsY;
  }

  public int[] getRegionIdByRing() {
    return regionIdByRing;
  }

  public int[][] getRingsInPolygonByRegion() {
    return ringsInPolygonByRegion;
  }
}
