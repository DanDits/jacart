package de.dandit.cartogram.core.context;

public class Region {
  private final int regionId;
  private final double data;
  private final double[][] polygonRingsX;
  private final double[][] polygonRingsY;
  private final int[] ringsInPolygons;

  public Region(int regionId, double data, double[][] polygonRingsX, double[][] polygonRingsY, int[] ringsInPolygons) {
    this.regionId = regionId;
    this.data = data;
    this.polygonRingsX = polygonRingsX;
    this.polygonRingsY = polygonRingsY;
    this.ringsInPolygons = ringsInPolygons;
  }

  public double[][] getPolygonRingsX() {
    return polygonRingsX;
  }

  public double[][] getPolygonRingsY() {
    return polygonRingsY;
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
