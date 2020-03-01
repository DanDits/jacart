package de.dandit.cartogram.core.pub;

import java.util.List;

public class CartogramResult {
  private final List<ResultRegion> resultRegions;
  private final double[] gridProjectionX;
  private final double[] gridProjectionY;
  private final int gridSizeX;
  private final int gridSizeY;
  // TODO add mae

  public CartogramResult(List<ResultRegion> resultRegions, double[] gridProjectionX, double[] gridProjectionY, int gridSizeX, int gridSizeY) {
    this.resultRegions = resultRegions;
    this.gridProjectionX = gridProjectionX;
    this.gridProjectionY = gridProjectionY;
    this.gridSizeX = gridSizeX;
    this.gridSizeY = gridSizeY;
  }

  public List<ResultRegion> getResultRegions() {
    return resultRegions;
  }

  public double[] getGridProjectionX() {
    return gridProjectionX;
  }

  public double[] getGridProjectionY() {
    return gridProjectionY;
  }

  public int getGridSizeX() {
    return gridSizeX;
  }

  public int getGridSizeY() {
    return gridSizeY;
  }
}
