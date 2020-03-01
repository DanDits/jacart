package de.dandit.cartogram.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PolygonUtilitiesTest {

  @Test
  public void getCWOrientedArea() {
    double area = PolygonUtilities.calculateOrientedArea(new double[] {-1, -1, 1, 1, -1}, new double[] {-1, 1, 1, -1, -1});
    Assertions.assertEquals(4., area);
  }

  @Test
  public void getCCWOrientedArea() {
    double area = PolygonUtilities.calculateOrientedArea(new double[] {-1, 1, 0, -1}, new double[] {0, 0, 1, 0});
    Assertions.assertEquals(-1., area);
  }
}