package de.dandit.cartogram.core.context;

public class PolygonData {
  private final Point[][] polygonRings;
  private final int[] polygonId;

  public PolygonData(Point[][] polygonRings, int[] polygonId) {
    this.polygonRings = polygonRings;
    this.polygonId = polygonId;
  }

  public Point[][] getPolygonRings() {
    return polygonRings;
  }

  public int[] getPolygonId() {
    return polygonId;
  }
}
