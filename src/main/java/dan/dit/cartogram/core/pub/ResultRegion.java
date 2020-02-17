package dan.dit.cartogram.core.pub;

import dan.dit.cartogram.core.context.Point;

import java.util.List;

public class ResultRegion {
  private final List<Point[]> hullCoordinates;
  private final boolean isNaN;

  public ResultRegion(List<Point[]> hullCoordinates, boolean isNaN) {
    this.hullCoordinates = hullCoordinates;
    this.isNaN = isNaN;
  }

  public List<Point[]> getHullCoordinates() {
    return hullCoordinates;
  }

  public boolean isNaN() {
    return isNaN;
  }
}
