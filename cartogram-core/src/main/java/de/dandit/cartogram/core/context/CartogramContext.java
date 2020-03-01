package de.dandit.cartogram.core.context;

import de.dandit.cartogram.core.pub.Logging;

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
