package dan.dit.cartogram.core.pub;

import dan.dit.cartogram.core.context.Point;

import java.util.List;

public class ResultPolygon {
  private final List<Point> exteriorRing; // clockwise orientation
  private final List<List<Point>> interiorRings; // counter-clockwise orientation

  public ResultPolygon(List<Point> exteriorRing, List<List<Point>> interiorRings) {
    this.exteriorRing = exteriorRing;
    this.interiorRings = interiorRings;
  }

  public List<Point> getExteriorRing() {
    return exteriorRing;
  }

  public List<List<Point>> getInteriorRings() {
    return interiorRings;
  }
}
