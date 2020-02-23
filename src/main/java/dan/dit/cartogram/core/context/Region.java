package dan.dit.cartogram.core.context;

import java.util.List;
import java.util.Map;

public class Region {
  private final int regionId;
  private final double data;
  private final Point[][] hullCoordinates;
  private final int[] ringsInPolygons;

  public Region(int regionId, double data, Point[][] hullCoordinates, int[] ringsInPolygons) {
    this.regionId = regionId;
    this.data = data;
    this.hullCoordinates = hullCoordinates;
    this.ringsInPolygons = ringsInPolygons;
  }

  public Point[][] getHullCoordinates() {
    return hullCoordinates;
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
