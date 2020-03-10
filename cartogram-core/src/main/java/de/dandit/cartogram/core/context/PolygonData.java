package de.dandit.cartogram.core.context;

public class PolygonData {
  private final double[][] polygonRingsX;
  private final double[][] polygonRingsY;
  private final int[] polygonId;
  private final int[][] ringsInPolygonByRegion;

  public PolygonData(double[][] polygonRingsX, double[][] polygonRingsY, int[] polygonId, int[][] ringsInPolygonByRegion) {
    this.polygonRingsX = polygonRingsX;
    this.polygonRingsY = polygonRingsY;
    this.polygonId = polygonId;
    this.ringsInPolygonByRegion = ringsInPolygonByRegion;
  }

  public double[][] getPolygonRingsX() {
    return polygonRingsX;
  }

  public double[][] getPolygonRingsY() {
    return polygonRingsY;
  }

  public int[] getPolygonId() {
    return polygonId;
  }

  public int[][] getRingsInPolygonByRegion() {
    return ringsInPolygonByRegion;
  }
}
