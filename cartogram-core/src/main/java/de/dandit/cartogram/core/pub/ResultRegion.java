package de.dandit.cartogram.core.pub;

import java.util.List;

public class ResultRegion {
  private final List<ResultPolygon> polygons;
  private final boolean isNaN; // TODO create a test for NaN

  public ResultRegion(List<ResultPolygon> polygons, boolean isNaN) {
    this.polygons = polygons;
    this.isNaN = isNaN;
  }

  public List<ResultPolygon> getPolygons() {
    return polygons;
  }

  public boolean isNaN() {
    return isNaN;
  }
}
