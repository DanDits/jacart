package dan.dit.cartogram.core.pub;

import dan.dit.cartogram.core.context.Point;

import java.util.List;

public class CartogramResult {
  private final List<ResultRegion> resultRegions;
  private final Point[] gridProjection;
  private final int gridSizeX;
  private final int gridSizeY;
  // TODO add mae

  public CartogramResult(List<ResultRegion> resultRegions, Point[] gridProjection, int gridSizeX, int gridSizeY) {
    this.resultRegions = resultRegions;
    this.gridProjection = gridProjection;
    this.gridSizeX = gridSizeX;
    this.gridSizeY = gridSizeY;
  }

  public List<ResultRegion> getResultRegions() {
    return resultRegions;
  }

  public Point[] getGridProjection() {
    return gridProjection;
  }

  public int getGridSizeX() {
    return gridSizeX;
  }

  public int getGridSizeY() {
    return gridSizeY;
  }
}
