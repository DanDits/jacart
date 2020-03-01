package de.dandit.cartogram.core.context;

public class PolygonData {
  private final double[][] polygonRingsX;
  private final double[][] polygonRingsY;
  private final int[] polygonId;

  public PolygonData(double[][] polygonRingsX, double[][] polygonRingsY, int[] polygonId) {
    this.polygonRingsX = polygonRingsX;
    this.polygonRingsY = polygonRingsY;
    this.polygonId = polygonId;
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
}
