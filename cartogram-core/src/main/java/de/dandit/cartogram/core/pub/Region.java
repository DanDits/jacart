package de.dandit.cartogram.core.pub;

/**
 * Describes a region that is the central unit in the cartogram transformation:
 * A region contains a list of oriented closed linear rings. A clockwise oriented ring is expected
 * for exterior rings of at least one or multiple polygons. Counter clockwise oriented rings (if any)
 * are expected for the holes of polygons. Each ring belongs to exactly one polygon.
 * Each region is associated with a value that is later put into relation to the value of other regions
 * to scale the region to make its relative area correspond to its relative value.
 * Each region is associated with a unique id that can be used to later identify the transformed
 * region.
 */
public class Region {
  private final int regionId;
  private final double data;
  private final double[][] polygonRingsX;
  private final double[][] polygonRingsY;
  private final int[] ringsInPolygons;

  /**
   *
   * @param regionId The region's unique id.
   * @param data The data that is associated with the region.
   * @param polygonRingsX A list of values describing the x coordinates of each of the regions' rings.
   * @param polygonRingsY A list of values describing the y coordinates of each of the regions' rings.
   * @param ringsInPolygons Describes a mapping to know if the ring with a given index is A) an exterior or interior ring
   * and B) to which polygon (by index) the ring belongs. For a single input Polygon with 2 holes (in this order) this would be [-1,0,0]
   * with negative values i marking an exterior ring of the polygon with index -(i+1).
   */
  public Region(int regionId, double data, double[][] polygonRingsX, double[][] polygonRingsY, int[] ringsInPolygons) {
    this.regionId = regionId;
    this.data = data;
    this.polygonRingsX = polygonRingsX;
    this.polygonRingsY = polygonRingsY;
    this.ringsInPolygons = ringsInPolygons;
  }

  public double[][] getPolygonRingsX() {
    return polygonRingsX;
  }

  public double[][] getPolygonRingsY() {
    return polygonRingsY;
  }

  public double getData() {
    return data;
  }

  public int getId() {
    return regionId;
  }

  public int[] getRingsInPolygons() {
    return ringsInPolygons;
  }
}
