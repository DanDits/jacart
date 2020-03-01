package de.dandit.cartogram.core.pub;

import java.util.List;

/**
 * Following is the input and how it is understood:
 * <p>
 * mapMinX: The minimum x value of the bounding box of all features.
 * mapMaxX: The maximum x value of the bounding box of all features.
 * mapMinY: The minimum y value of the bounding box of all features.
 * mapMaxY: The maximum y value of the bounding box of all features.
 * <p>
 */
public class MapFeatureData {

  /* BoundingBox parsed from the input data */
  private final double mapMinX;
  private final double mapMinY;
  private final double mapMaxX;
  private final double mapMaxY;

  private final double[] targetAreaPerRegion; // use negative for invalid areas, use NaN for explicitly set not available value
  private final List<Region> regions;

  public MapFeatureData(double mapMinX, double mapMinY, double mapMaxX, double mapMaxY,
                        List<Region> regions,
                        double[] targetAreaPerRegion) {
    this.mapMinX = mapMinX;
    this.mapMinY = mapMinY;
    this.mapMaxX = mapMaxX;
    this.mapMaxY = mapMaxY;
    this.regions = regions;
    this.targetAreaPerRegion = targetAreaPerRegion;
  }

  public double getMapMinX() {
    return mapMinX;
  }

  public double getMapMinY() {
    return mapMinY;
  }

  public double getMapMaxX() {
    return mapMaxX;
  }

  public double getMapMaxY() {
    return mapMaxY;
  }

  public double[] getTargetAreaPerRegion() {
    return targetAreaPerRegion;
  }

  public List<Region> getRegions() {
    return regions;
  }
}
