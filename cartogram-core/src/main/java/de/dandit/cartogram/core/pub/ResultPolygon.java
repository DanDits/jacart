package de.dandit.cartogram.core.pub;

import java.util.List;

public class ResultPolygon {
  private final double[] exteriorRingX; // clockwise orientation
  private final double[] exteriorRingY; // clockwise orientation
  private final List<double[]> interiorRingsX; // counter-clockwise orientation
  private final List<double[]> interiorRingsY; // counter-clockwise orientation

  public ResultPolygon(double[] exteriorRingX, double[] exteriorRingY, List<double[]> interiorRingsX, List<double[]> interiorRingsY) {
    this.exteriorRingX = exteriorRingX;
    this.exteriorRingY = exteriorRingY;
    this.interiorRingsX = interiorRingsX;
    this.interiorRingsY = interiorRingsY;
  }

  public double[] getExteriorRingX() {
    return exteriorRingX;
  }

  public double[] getExteriorRingY() {
    return exteriorRingY;
  }

  public List<double[]> getInteriorRingsX() {
    return interiorRingsX;
  }

  public List<double[]> getInteriorRingsY() {
    return interiorRingsY;
  }
}
