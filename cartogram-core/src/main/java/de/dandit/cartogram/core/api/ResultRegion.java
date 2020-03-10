package de.dandit.cartogram.core.api;

import java.util.List;

public class ResultRegion {
  private final List<LightPolygon> polygons;
  private final int regionId;
  private final boolean isNaN;

  public ResultRegion(int regionId, List<LightPolygon> polygons, boolean isNaN) {
    this.regionId = regionId;
    this.polygons = polygons;
    this.isNaN = isNaN;
  }

  public List<LightPolygon> getPolygons() {
    return polygons;
  }

  public boolean isNaN() {
    return isNaN;
  }

  public int getRegionId() {
    return regionId;
  }
}
