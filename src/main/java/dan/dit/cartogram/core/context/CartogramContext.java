package dan.dit.cartogram.core.context;

import dan.dit.cartogram.core.pub.Logging;

/**
 * For now this holds all previously global variables with the goal to better encapsulate, group and separate them
 * while making sure that initialization order and data flow becomes more obvious and less error prone than in the
 * C program
 */
public class CartogramContext {

  private final MapGrid mapGrid;
  private final RegionData regionData;
  private final boolean isSingleRegion;
  private final double originalSummedArea;
  private final Logging logging;

  public CartogramContext(Logging logging, MapGrid mapGrid, RegionData regionData, boolean isSingleRegion, double originalSummedArea) {
    this.logging = logging;
    this.mapGrid = mapGrid;
    this.regionData = regionData;
    this.isSingleRegion = isSingleRegion;
    this.originalSummedArea = originalSummedArea;
  }

  public boolean isSingleRegion() {
    return isSingleRegion;
  }

  public MapGrid getMapGrid() {
    return mapGrid;
  }

  public RegionData getRegionData() {
    return regionData;
  }

  public Logging getLogging() {
    return logging;
  }

  public double getOriginalSummedArea() {
    return originalSummedArea;
  }
}
