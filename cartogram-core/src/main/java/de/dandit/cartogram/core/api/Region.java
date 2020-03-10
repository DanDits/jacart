package de.dandit.cartogram.core.api;

import java.util.List;

/**
 * Describes a region that is the central unit in the cartogram transformation:
 * A region contains a list of polygons (at least one).<br>
 * Each region is associated with a value that is later put into relation to the value of other regions
 * to scale the region to make its relative area correspond to its relative value.
 * Each region is associated with a unique id that can be used to later identify the transformed
 * region.
 */
public class Region {
  private final int regionId;
  private final double data;
  private final List<LightPolygon> polygons;

  /**
   *
   * @param regionId The region's unique id.
   * @param data The data that is associated with the region.
   * @param polygons A list of polygons.
   */
  public Region(int regionId, double data, List<LightPolygon> polygons) {
    this.regionId = regionId;
    this.data = data;
    this.polygons = polygons;
  }

  public double getData() {
    return data;
  }

  public int getId() {
    return regionId;
  }

  public List<LightPolygon> getPolygons() {
    return polygons;
  }
}
