package de.dandit.cartogram.core;

import de.dandit.cartogram.core.context.Point;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PolygonTest {

  @Test
  public void getCWOrientedArea() {
    double area = Polygon.calculateOrientedArea(new Point[]{new Point(-1, -1), new Point(-1, 1), new Point(1, 1), new Point(1, -1), new Point(-1, -1)});
    Assertions.assertEquals(4., area);
  }

  @Test
  public void getCCWOrientedArea() {
    double area = Polygon.calculateOrientedArea(new Point[]{new Point(-1, 0), new Point(1, 0), new Point(0, 1), new Point(-1, 0)});
    Assertions.assertEquals(-1., area);
  }
}