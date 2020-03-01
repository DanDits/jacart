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
      double[][] cartcornX = context.getRegionData().getCartogramRingsX();
      double[][] cartcornY = context.getRegionData().getCartogramRingsY();
      double[][] polycornX = context.getRegionData().getRingsX();
      double[][] polycornY = context.getRegionData().getRingsY();
      for (int i = 0; i < polycornX.length; i++) {
        cartcornX[i] = Arrays.copyOf(polycornX[i], polycornX[i].length);
        cartcornY[i] = Arrays.copyOf(polycornY[i], polycornY[i].length);
      }
      return context;
    }
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    double[] projX = mapGrid.getGridProjectionX();
    double[] projY = mapGrid.getGridProjectionY();

    context.getLogging().debug("Starting integration 1\n");
    integrate.ffb_integrate(parallelismConfig);
    project(false);

    double[][] cartcornX = regionData.getCartogramRingsX();
    double[][] cartcornY = regionData.getCartogramRingsY();
    AreaErrorResult error = calculateMaximumAreaError(
      context.getRegionData().getTargetArea(),
      context.getRegionData().getRingInRegion(),
      cartcornX,
      cartcornY);
    double mae = error.maximumAreaError;
    context.getLogging().debug("max. abs. area error: {0}", mae);

    double[] proj2X = mapGrid.getGridProjectionXSwapper();
    double[] proj2Y = mapGrid.getGridProjectionYSwapper();
    int integration = 0;
    double lastMae = Double.POSITIVE_INFINITY;
    while (mae > maxPermittedAreaError && mae < lastMae) {
      density.fill_with_density2();

      System.arraycopy(projX, 0, proj2X, 0, lx * ly);
      System.arraycopy(projY, 0, proj2Y, 0, lx * ly);
      for (int i = 0; i < lx; i++) {
        for (int j = 0; j < ly; j++) {
          projX[i * ly + j] = i + 0.5;
          projY[i * ly + j] = j + 0.5;
        }
      }
      integration++;
      context.getLogging().debug("Starting integration {0}", integration);
      integrate.ffb_integrate(parallelismConfig);
      project(true);

      System.arraycopy(proj2X, 0, projX, 0, lx * ly);
      System.arraycopy(proj2Y, 0, projY, 0, lx * ly);
      lastMae = mae;
      error = calculateMaximumAreaError(
        context.getRegionData().getTargetArea(),
        context.getRegionData().getRingInRegion(),
        cartcornX,
        cartcornY);
      mae = error.maximumAreaError;
      context.getLogging().debug("max. abs. area error: {0}", mae);
      if (lastMae < mae) {
        context.getLogging().error("Did not converge, aborted! Error is: {0}", mae);
        throw new ConvergenceGoalFailedException("Error increased from " + lastMae + " to " + mae);
      }
    }
    double initialArea = initialAreaError.summedCartogramArea;
    double correctionFactor = Math.sqrt(initialArea / error.summedCartogramArea);
    context.getLogging().debug("Scaling result with factor = {0}", correctionFactor);
    for (int i = 0; i < cartcornX.length; i++) {
      scalePolygonsToMatchInitialTotalArea(correctionFactor, lx, ly, cartcornX[i], cartcornY[i]);
    }
    if (scaleToOriginalPolygonRegion) {
      double scalingFactor = mapGrid.getInitialScalingFactor();
      double offsetX = mapGrid.getInitialDeltaX();
      double offsetY = mapGrid.getInitialDeltaY();
      for (int i = 0; i < cartcornX.length; i++) {
        double[] pX = cartcornX[i];
        double[] pY = cartcornY[i];
        for (int j = 0; j < pX.length; j++) {
          pX[j] = pX[j] * scalingFactor + offsetX;
          pY[j] = pY[j] * scalingFactor + offsetY;
        }
      }
    }

    double final_max_area_error = calculateMaximumAreaError(
      context.getRegionData().getTargetArea(),
      context.getRegionData().getRingInRegion(),
      cartcornX,
      cartcornY).maximumAreaError;
    context.getLogging().debug("Final error: {0}", final_max_area_error);
    return this.context;
  }

  private void scalePolygonsToMatchInitialTotalArea(double correction_factor, int lx, int ly, double[] pointsX, double[] pointsY) {
    for (int i = 0; i < pointsX.length; i++) {
      pointsX[i] = correction_factor * (pointsX[i] - 0.5 * lx) + 0.5 * lx;
      pointsY[i] = correction_factor * (pointsY[i] - 0.5 * ly) + 0.5 * ly;
    }
  }

  void project(boolean proj_graticule) {
    double[] xdisp, ydisp;
    int i, j;
    MapGrid mapGrid = context.getMapGrid();
    RegionData regionData = context.getRegionData();
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    double[] projX = mapGrid.getGridProjectionX();
    double[] projY = mapGrid.getGridProjectionY();

    xdisp = new double[lx * ly];
    ydisp = new double[lx * ly];
    for (i = 0; i < lx; i++) {
      for (j = 0; j < ly; j++) {
        xdisp[i * ly + j] = projX[i * ly + j] - i - 0.5;
        ydisp[i * ly + j] = projY[i * ly + j] - j - 0.5;
      }
    }

    double[][] polycornX = regionData.getRingsX();
    double[][] polycornY = regionData.getRingsY();
    int n_poly = polycornX.length;
    double[] proj2X = mapGrid.getGridProjectionXSwapper();
    double[] proj2Y = mapGrid.getGridProjectionYSwapper();
    double[][] cartcornX = regionData.getCartogramRingsX();
    double[][] cartcornY = regionData.getCartogramRingsY();

    for (i = 0; i < n_poly; i++) {
      double[] polyIX = polycornX[i];
      double[] polyIY = polycornY[i];
      for (j = 0; j < polyIX.length; j++) {
        double pointIJX = polyIX[j];
        double pointIJY = polyIY[j];
        Integrate.interpolate(lx, ly, pointIJX, pointIJY, xdisp, ydisp, cartcornX[i], cartcornY[i], j);
        cartcornX[i][j] += pointIJX;
        cartcornY[i][j] += pointIJY;
      }
    }
    if (proj_graticule) {
      for (i = 0; i < lx * ly; i++) {
        double x2 = proj2X[i];
        double y2 = proj2Y[i];
        Integrate.interpolate(lx, ly, x2, y2, xdisp, ydisp, proj2X, proj2Y, i);
        proj2X[i] += x2;
        proj2Y[i] += y2;
      }
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

  public static AreaErrorResult calculateMaximumAreaError(double[] target_area, int[][] polyinreg, double[][] cornX, double[][] cornY) {
    double obj_area, sum_target_area;
    int i, j;

    int n_reg = polyinreg.length;
    double[] area_err = new double[n_reg];
    double[] cart_area = new double[n_reg];
    for (i = 0; i < n_reg; i++) {
      // if all polygons in a region were tiny they will be removed and thus it will be impossible for
      // the cartogram area to reach the target area (e.g.: Washington D.C.)
      // or we could also remove the region and ignore it completely
      int[] polyI = polyinreg[i];
      if (polyI.length > 0) {
        cart_area[i] = 0.0;
        for (j = 0; j < polyI.length; j++) {
          cart_area[i] += PolygonUtilities.calculateOrientedArea(cornX[polyI[j]], cornY[polyI[j]]);
        }
      } else {
        cart_area[i] = -1.0;
      }
    }
    for (i = 0, sum_target_area = 0.0; i < n_reg; i++) {
      sum_target_area += target_area[i];
    }
    double sum_cart_area = 0.;
    for (i = 0; i < n_reg; i++) {
      if (cart_area[i] >= 0) {
        sum_cart_area += cart_area[i];
      }
    }
    for (i = 0; i < n_reg; i++) {
      if (cart_area[i] >= 0) {
        obj_area = target_area[i] * (sum_cart_area) / sum_target_area;
        area_err[i] = cart_area[i] / obj_area - 1.0;
      } else {
        area_err[i] = 0; // ignore the region
      }
    }
    double max = 0.0;
    for (i = 0; i < n_reg; i++) {
      max = Math.max(max, Math.abs(area_err[i]));
    }
    return new AreaErrorResult(max, sum_cart_area);
  }
}
