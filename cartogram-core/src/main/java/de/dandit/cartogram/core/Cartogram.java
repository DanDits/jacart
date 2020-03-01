package de.dandit.cartogram.core;

import java.util.Arrays;
import java.util.Objects;

import de.dandit.cartogram.core.context.CartogramContext;
import de.dandit.cartogram.core.context.MapGrid;
import de.dandit.cartogram.core.context.RegionData;
import de.dandit.cartogram.core.pub.ParallelismConfig;

public class Cartogram {
  private final Integrate integrate;
  private final Density density;
  private final CartogramContext context;

  public Cartogram(CartogramContext context) {
    this.context = Objects.requireNonNull(context);
    this.integrate = new Integrate(context);
    this.density = new Density(context);
  }

  public CartogramContext calculate(ParallelismConfig parallelismConfig, boolean scaleToOriginalPolygonRegion, double maxPermittedAreaError) throws ConvergenceGoalFailedException {
    boolean onlyOneRegionExists = context.isSingleRegion();
    if (onlyOneRegionExists) {
      context.getLogging().debug("Hint: Only one region exists, output will only be an affine transformation.");
    }
    MapGrid mapGrid = context.getMapGrid();
    RegionData regionData = context.getRegionData();
    AreaErrorResult initialAreaError = calculateMaximumAreaError(
      context.getRegionData().getTargetArea(),
      context.getRegionData().getRingInRegion(),
      regionData.getRingsX(),
      regionData.getRingsY());
    if (initialAreaError.maximumAreaError <= maxPermittedAreaError) {
      context.getLogging().debug("Nothing to do, area already correct.");
      double[][] cartogramRingsX = context.getRegionData().getCartogramRingsX();
      double[][] cartogramRingsY = context.getRegionData().getCartogramRingsY();
      double[][] polygonRingsX = context.getRegionData().getRingsX();
      double[][] polygonRingsY = context.getRegionData().getRingsY();
      for (int i = 0; i < polygonRingsX.length; i++) {
        cartogramRingsX[i] = Arrays.copyOf(polygonRingsX[i], polygonRingsX[i].length);
        cartogramRingsY[i] = Arrays.copyOf(polygonRingsY[i], polygonRingsY[i].length);
      }
      return context;
    }
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    double[] gridProjectionX = mapGrid.getGridProjectionX();
    double[] gridProjectionY = mapGrid.getGridProjectionY();

    context.getLogging().debug("Starting integration 1");
    integrate.ffbIntegrate(parallelismConfig);
    project(false);

    double[][] cartogramRingsX = regionData.getCartogramRingsX();
    double[][] cartogramRingsY = regionData.getCartogramRingsY();
    AreaErrorResult error = calculateMaximumAreaError(
      context.getRegionData().getTargetArea(),
      context.getRegionData().getRingInRegion(),
      cartogramRingsX,
      cartogramRingsY);
    double maximumAreaError = error.maximumAreaError;
    context.getLogging().debug("max. abs. area error: {0}", maximumAreaError);

    double[] gridProjectionXSwapper = mapGrid.getGridProjectionXSwapper();
    double[] gridProjectionYSwapper = mapGrid.getGridProjectionYSwapper();
    int integrationCounter = 0;
    double lastMaximumAreaError = Double.POSITIVE_INFINITY;
    while (maximumAreaError > maxPermittedAreaError && maximumAreaError < lastMaximumAreaError) {
      density.fillWithDensity();

      System.arraycopy(gridProjectionX, 0, gridProjectionXSwapper, 0, lx * ly);
      System.arraycopy(gridProjectionY, 0, gridProjectionYSwapper, 0, lx * ly);
      for (int i = 0; i < lx; i++) {
        for (int j = 0; j < ly; j++) {
          gridProjectionX[i * ly + j] = i + 0.5;
          gridProjectionY[i * ly + j] = j + 0.5;
        }
      }
      integrationCounter++;
      context.getLogging().debug("Starting integration {0}", integrationCounter);
      integrate.ffbIntegrate(parallelismConfig);
      project(true);

      System.arraycopy(gridProjectionXSwapper, 0, gridProjectionX, 0, lx * ly);
      System.arraycopy(gridProjectionYSwapper, 0, gridProjectionY, 0, lx * ly);
      lastMaximumAreaError = maximumAreaError;
      error = calculateMaximumAreaError(
        context.getRegionData().getTargetArea(),
        context.getRegionData().getRingInRegion(),
        cartogramRingsX,
        cartogramRingsY);
      maximumAreaError = error.maximumAreaError;
      context.getLogging().debug("Maximum absolute area error: {0}", maximumAreaError);
      if (lastMaximumAreaError < maximumAreaError) {
        context.getLogging().error("Did not converge, aborted! Error is: {0}", maximumAreaError);
        throw new ConvergenceGoalFailedException("Error increased from " + lastMaximumAreaError + " to " + maximumAreaError);
      }
    }
    double initialArea = initialAreaError.summedCartogramArea;
    double correctionFactor = Math.sqrt(initialArea / error.summedCartogramArea);
    context.getLogging().debug("Scaling result with factor = {0}", correctionFactor);
    for (int i = 0; i < cartogramRingsX.length; i++) {
      scalePolygonsToMatchInitialTotalArea(correctionFactor, lx, ly, cartogramRingsX[i], cartogramRingsY[i]);
    }
    if (scaleToOriginalPolygonRegion) {
      scaleToOriginalPolygonRegion(mapGrid, cartogramRingsX, cartogramRingsY);
    }

    double finalMaxAreaError = calculateMaximumAreaError(
      context.getRegionData().getTargetArea(),
      context.getRegionData().getRingInRegion(),
      cartogramRingsX,
      cartogramRingsY).maximumAreaError;
    context.getLogging().debug("Final error: {0}", finalMaxAreaError);
    return this.context;
  }

  private void scaleToOriginalPolygonRegion(
      MapGrid mapGrid,
      double[][] cartogramRingsX,
      double[][] cartogramRingsY) {
    double scalingFactor = mapGrid.getInitialScalingFactor();
    double offsetX = mapGrid.getInitialDeltaX();
    double offsetY = mapGrid.getInitialDeltaY();
    for (int i = 0; i < cartogramRingsX.length; i++) {
      double[] pX = cartogramRingsX[i];
      double[] pY = cartogramRingsY[i];
      for (int j = 0; j < pX.length; j++) {
        pX[j] = pX[j] * scalingFactor + offsetX;
        pY[j] = pY[j] * scalingFactor + offsetY;
      }
    }
  }

  private void scalePolygonsToMatchInitialTotalArea(double correctionFactor, int lx, int ly, double[] pointsX, double[] pointsY) {
    for (int i = 0; i < pointsX.length; i++) {
      pointsX[i] = correctionFactor * (pointsX[i] - 0.5 * lx) + 0.5 * lx;
      pointsY[i] = correctionFactor * (pointsY[i] - 0.5 * ly) + 0.5 * ly;
    }
  }

  void project(boolean projectGraticule) {
    MapGrid mapGrid = context.getMapGrid();
    RegionData regionData = context.getRegionData();
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    double[] gridProjectionX = mapGrid.getGridProjectionX();
    double[] gridProjectionY = mapGrid.getGridProjectionY();

    double[] displacementX = new double[lx * ly];
    double[] displacementY = new double[lx * ly];
    for (int i = 0; i < lx; i++) {
      for (int j = 0; j < ly; j++) {
        displacementX[i * ly + j] = gridProjectionX[i * ly + j] - i - 0.5;
        displacementY[i * ly + j] = gridProjectionY[i * ly + j] - j - 0.5;
      }
    }

    double[][] ringsX = regionData.getRingsX();
    double[][] ringsY = regionData.getRingsY();
    int ringsCount = ringsX.length;
    double[] gridProjectionXSwapper = mapGrid.getGridProjectionXSwapper();
    double[] gridProjectionYSwapper = mapGrid.getGridProjectionYSwapper();
    double[][] cartogramRingsX = regionData.getCartogramRingsX();
    double[][] cartogramRingsY = regionData.getCartogramRingsY();

    for (int i = 0; i < ringsCount; i++) {
      double[] polyIX = ringsX[i];
      double[] polyIY = ringsY[i];
      for (int j = 0; j < polyIX.length; j++) {
        double pointIJX = polyIX[j];
        double pointIJY = polyIY[j];
        Integrate.interpolate(lx, ly, pointIJX, pointIJY, displacementX, displacementY, cartogramRingsX[i], cartogramRingsY[i], j);
        cartogramRingsX[i][j] += pointIJX;
        cartogramRingsY[i][j] += pointIJY;
      }
    }
    if (projectGraticule) {
      projectGraticule(displacementX, displacementY, lx, ly, gridProjectionXSwapper, gridProjectionYSwapper);
    }
  }

  private void projectGraticule(
      double[] displacementX,
      double[] displacementY,
      int lx,
      int ly,
      double[] projectionX,
      double[] projectionY) {
    for (int i = 0; i < lx * ly; i++) {
      double x = projectionX[i];
      double y = projectionY[i];
      Integrate.interpolate(lx, ly, x, y, displacementX, displacementY, projectionX, projectionY, i);
      projectionX[i] += x;
      projectionY[i] += y;
    }
  }

  public static class AreaErrorResult {
    private final double maximumAreaError;
    private final double summedCartogramArea;

    private AreaErrorResult(double maximumAreaError, double summedCartogramArea) {
      this.maximumAreaError = maximumAreaError;
      this.summedCartogramArea = summedCartogramArea;
    }
  }

  public static AreaErrorResult calculateMaximumAreaError(double[] targetArea, int[][] ringInRegion, double[][] cornX, double[][] cornY) {
    int ringCount = ringInRegion.length;
    double[] areaError = new double[ringCount];
    double[] cartogramArea = new double[ringCount];
    for (int i = 0; i < ringCount; i++) {
      // if all polygons in a region were tiny they will be removed and thus it will be impossible for
      // the cartogram area to reach the target area (e.g.: Washington D.C.)
      // or we could also remove the region and ignore it completely
      int[] polyI = ringInRegion[i];
      if (polyI.length > 0) {
        cartogramArea[i] = 0.0;
        for (int value : polyI) {
          cartogramArea[i] += PolygonUtilities.calculateOrientedArea(cornX[value], cornY[value]);
        }
      } else {
        cartogramArea[i] = -1.;
      }
    }
    double summedTargetArea = 0.;
    for (int i = 0; i < ringCount; i++) {
      summedTargetArea += targetArea[i];
    }
    double summedCartogramArea = 0.;
    for (int i = 0; i < ringCount; i++) {
      if (cartogramArea[i] >= 0) {
        summedCartogramArea += cartogramArea[i];
      }
    }
    for (int i = 0; i < ringCount; i++) {
      if (cartogramArea[i] >= 0) {
        double relativeArea = targetArea[i] * (summedCartogramArea) / summedTargetArea;
        areaError[i] = cartogramArea[i] / relativeArea - 1.;
      } else {
        areaError[i] = 0; // ignore the region
      }
    }
    double max = 0.;
    for (int i = 0; i < ringCount; i++) {
      max = Math.max(max, Math.abs(areaError[i]));
    }
    return new AreaErrorResult(max, summedCartogramArea);
  }
}
