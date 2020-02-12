package dan.dit.cartogram;

public class PolygonData {
  private final Point[][] polycorn;
  private final int[] polygonId;

  public PolygonData(Point[][] polycorn, int[] polygonId) {
    this.polycorn = polycorn;
    this.polygonId = polygonId;
  }

  public Point[][] getPolycorn() {
    return polycorn;
  }

  public int[] getPolygonId() {
    return polygonId;
  }
}
