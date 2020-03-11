package de.dandit.cartogram.core.api;

import java.util.List;

/**
 * Describes a Region that was transformed into a cartogram region.
 * The region id corresponds to a region id of a given Region. But note that there are some things that might have
 * gone wrong that need to be anticipated:
 * - The flag isNan() is set if the given target value for this region was NaN. This indicates that the result size
 * may be arbitrary or unexpected and that the region may need to be visualized  differently.<br>
 *   Also the amount of polygons and their interior rings might have changed. If tiny rings were removed this could
 *   have potentially removed some or all interior rings. Some polygons of a multi polygon can have been removed
 *   completely. This needs to be handled accordingly. Note that this is done to
 *   prevent convergence problems with tiny rings.<br>
 *     It is possible that a region is missing in the result. This happens if all its polygons were tiny.
 */
public class ResultRegion {
  private final List<LightPolygon> polygons;
  private final int regionId;
  private final boolean isNaN;

  public ResultRegion(int regionId, List<LightPolygon> polygons, boolean isNaN) {
    this.regionId = regionId;
    this.polygons = polygons;
    this.isNaN = isNaN;
  }

  public List<LightPolygon> getPolygons() {
    return polygons;
  }

  public boolean isNaN() {
    return isNaN;
  }

  public int getRegionId() {
    return regionId;
  }
}
