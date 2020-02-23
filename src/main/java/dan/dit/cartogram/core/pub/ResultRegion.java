package dan.dit.cartogram.core.pub;

import dan.dit.cartogram.core.context.Point;

import java.util.List;
import java.util.Map;

public class ResultRegion {
  private final List<ResultPolygon> polygons;
  private final boolean isNaN;

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
