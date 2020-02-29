package de.dandit.cartogram.core.pub;

import de.dandit.cartogram.core.context.Region;

import java.util.List;

/**
 * Following is the input and how it is understood:
 * <p>
 * map_minx: The minimum x value of the bounding box of all features.
 * map_maxx: The maximum x value of the bounding box of all features.
 * map_miny: The minimum y value of the bounding box of all features.
 * map_maxy: The maximum y value of the bounding box of all features.
 * <p>
 * n_poly: The amount of polygons. More exactly the amount of individual closed linear rings. Example: A simple Polygon will contribute 1. A Polygon with 2 holes will contribute 3. A MultiPolygon with 2 simple Polygons will contribute 2.
 * n_poly_corn: The amount of corners the i-th polygon has. More exactly the amount of points (>=4) that connect to a linear ring.
 * polycorn: The corners of a polygon. More exactly the points (>=4) that connect to a linear ring. The first point is identical to the last point. Can be part of a MultiPolygon or a hole of some polygon.
 * polygon_id: The ids of the polygons. More exactly the feature id that uniquely identifies a Polygon or MultiPolygon. Will be identical for all linear rings of a Polygon and MultiPolygon.
 */
public class MapFeatureData {

  /* BoundingBox parsed from the input data */
  private final double map_minx;
  private final double map_miny;
  private final double map_maxx;
  private final double map_maxy;

  // TODO this is not really an area value, this is more of a weighting area, create a test for that statement
  private final double[] targetAreaPerRegion; // use negative for invalid areas, use NaN for explicitly set not available value, see region_na
  private final List<Region> regions;

  /* The feature data describing a value belonging to each region that will define the target area
   * for the region that consists of multiple polygons minus their holes */

  public MapFeatureData(double map_minx, double map_miny, double map_maxx, double map_maxy,
                        List<Region> regions,
                        double[] targetAreaPerRegion) {
    this.map_minx = map_minx;
    this.map_miny = map_miny;
    this.map_maxx = map_maxx;
    this.map_maxy = map_maxy;
    this.regions = regions;
    this.targetAreaPerRegion = targetAreaPerRegion;
  }

  public double getMap_minx() {
    return map_minx;
  }

  public double getMap_miny() {
    return map_miny;
  }

  public double getMap_maxx() {
    return map_maxx;
  }

  public double getMap_maxy() {
    return map_maxy;
  }

  public double[] getTargetAreaPerRegion() {
    return targetAreaPerRegion;
  }

  public List<Region> getRegions() {
    return regions;
  }
}
