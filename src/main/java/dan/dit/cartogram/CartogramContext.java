package dan.dit.cartogram;

/**
 * For now this holds all previously global variables with the goal to better encapsulate, group and separate them
 * while making sure that initialization order and data flow becomes more obvious and less error prone than in the
 * C program
 */
public class CartogramContext {

  private final MapGrid mapGrid;
  private final RegionData regionData;
  private final boolean isSingleRegion;

  public CartogramContext(MapGrid mapGrid, RegionData regionData, boolean isSingleRegion) {
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
}
