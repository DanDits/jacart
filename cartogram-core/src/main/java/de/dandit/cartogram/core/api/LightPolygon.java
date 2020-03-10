package de.dandit.cartogram.core.api;

import java.util.List;

/**
 * Defines a lightweight representation of a 2D-Polygon with x and y components split
 * into separate double arrays. The polygon is defined by its clockwise oriented exterior
 * ring and a (potentially empty) list of counter-clockwise oriented interior rings. Rings
 * are closed which means that the first and last coordinate are identical. <br>
 * There is no validation done on the orientation, you might need to normalize your geometry
 * beforehand.<br>
 *   This class is not doing safety copies of given arrays or lists, so
 * if you need to make sure those arrays stay untouched you need to make copies yourself.
 */
public class LightPolygon {
  private final double[] exteriorRingX; // clockwise orientation
  private final double[] exteriorRingY; // clockwise orientation
  private final List<double[]> interiorRingsX; // counter-clockwise orientation
  private final List<double[]> interiorRingsY; // counter-clockwise orientation

  public LightPolygon(double[] exteriorRingX, double[] exteriorRingY, List<double[]> interiorRingsX, List<double[]> interiorRingsY) {
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
