package de.dandit.cartogram.core.context;

public class Region {
  private final int regionId;
  private final double data;
  private final Point[][] polygonRings;
  private final int[] ringsInPolygons;

  public Region(int regionId, double data, Point[][] polygonRings, int[] ringsInPolygons) {
    this.regionId = regionId;
    this.data = data;
    this.polygonRings = polygonRings;
    this.ringsInPolygons = ringsInPolygons;
  }

  public Point[][] getPolygonRings() {
    return polygonRings;
  }

  public double getData() {
    return data;
  }

  public int getId() {
    return regionId;
  }

  public int[] getRingsInPolygons() {
    return ringsInPolygons;
  }
}
