package dan.dit.cartogram.core;

import dan.dit.cartogram.core.context.Point;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.util.Assert;

class PolygonTest {

  @Test
  public void getCWOrientedArea() {
    double area = Polygon.calculateOrientedArea(new Point[]{new Point(-1, -1), new Point(-1, 1), new Point(1, 1), new Point(1, -1), new Point(-1, -1)});
    Assert.equals(4., area);
  }

  @Test
  public void getCCWOrientedArea() {
    double area = Polygon.calculateOrientedArea(new Point[]{new Point(-1, 0), new Point(1, 0), new Point(0, 1), new Point(-1, 0)});
    Assert.equals(-1., area);
  }
}