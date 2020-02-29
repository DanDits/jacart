package de.dandit.cartogram.core.context;

import de.dandit.cartogram.core.pub.Logging;

/**
 * For now this holds all previously global variables with the goal to better encapsulate, group and separate them
 * while making sure that initialization order and data flow becomes more obvious and less error prone than in the
 * C program
 */
public class CartogramContext {

  private final MapGrid mapGrid;
  private final RegionData regionData;
  private final boolean isSingleRegion;
  private final Logging logging;

  public CartogramContext(Logging logging, MapGrid mapGrid, RegionData regionData, boolean isSingleRegion) {
    this.logging = logging;
    this.mapGrid = mapGrid;
    this.regionData = regionData;
    this.isSingleRegion = isSingleRegion;
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

}