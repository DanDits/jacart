package de.dandit.cartogram.core;


import java.util.Arrays;
import java.util.Objects;

import de.dandit.cartogram.core.context.CartogramContext;
import de.dandit.cartogram.core.context.MapGrid;
import de.dandit.cartogram.core.context.Point;
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
      regionData.getRings());
    if (initialAreaError.maximumAreaError <= maxPermittedAreaError) {
      context.getLogging().debug("Nothing to do, area already correct.");
      Point[][] cartcorn = context.getRegionData().getCartogramRings();
      Point[][] polycorn = context.getRegionData().getRings();
      for (int i = 0; i < polycorn.length; i++) {
        cartcorn[i] = Arrays.copyOf(polycorn[i], polycorn[i].length);
      }
      return context;
    }
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    Point[] proj = mapGrid.getGridProjection();

    context.getLogging().debug("Starting integration 1\n");
    integrate.ffb_integrate(parallelismConfig);
    project(false);

    Point[][] cartcorn = regionData.getCartogramRings();
    AreaErrorResult error = calculateMaximumAreaError(
      context.getRegionData().getTargetArea(),
      context.getRegionData().getRingInRegion(),
      cartcorn);
    double mae = error.maximumAreaError;
    context.getLogging().debug("max. abs. area error: {0}", mae);

    Point[] proj2 = mapGrid.getGridProjectionSwapper();
    int integration = 0;
    double lastMae = Double.POSITIVE_INFINITY;
    while (mae > maxPermittedAreaError && mae < lastMae) {
      density.fill_with_density2();

      for (int i = 0; i < lx; i++) {
        for (int j = 0; j < ly; j++) {
          proj2[i * ly + j].x = proj[i * ly + j].x;
          proj2[i * ly + j].y = proj[i * ly + j].y;
        }
      }
      for (int i = 0; i < lx; i++) {
        for (int j = 0; j < ly; j++) {
          proj[i * ly + j].x = i + 0.5;
          proj[i * ly + j].y = j + 0.5;
        }
      }
      integration++;
      context.getLogging().debug("Starting integration {0}", integration);
      integrate.ffb_integrate(parallelismConfig);
      project(true);

      for (int i = 0; i < lx; i++) {
        for (int j = 0; j < ly; j++) {
          proj[i * ly + j].x = proj2[i * ly + j].x;
          proj[i * ly + j].y = proj2[i * ly + j].y;
        }
      }
      lastMae = mae;
      error = calculateMaximumAreaError(
        context.getRegionData().getTargetArea(),
        context.getRegionData().getRingInRegion(),
        cartcorn);
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
    for (Point[] points : cartcorn) {
      scalePolygonsToMatchInitialTotalArea(correctionFactor, lx, ly, points);
    }
    if (scaleToOriginalPolygonRegion) {
      double scalingFactor = mapGrid.getInitialScalingFactor();
      double offsetX = mapGrid.getInitialDeltaX();
      double offsetY = mapGrid.getInitialDeltaY();
      for (Point[] points : cartcorn) {
        for (Point point : points) {
          point.x = point.x * scalingFactor + offsetX;
          point.y = point.y * scalingFactor + offsetY;
        }
      }
    }

    double final_max_area_error = calculateMaximumAreaError(
      context.getRegionData().getTargetArea(),
      context.getRegionData().getRingInRegion(),
      cartcorn).maximumAreaError;
    context.getLogging().debug("Final error: {0}", final_max_area_error);
    return this.context;
  }

  private void scalePolygonsToMatchInitialTotalArea(double correction_factor, int lx, int ly, Point[] points) {
    for (Point point : points) {
      point.x = correction_factor * (point.x - 0.5 * lx) + 0.5 * lx;
      point.y = correction_factor * (point.y - 0.5 * ly) + 0.5 * ly;
    }
  }

  void project(boolean proj_graticule) {
    double x2, y2;
    double[] xdisp, ydisp;
    int i, j;
    MapGrid mapGrid = context.getMapGrid();
    RegionData regionData = context.getRegionData();
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    Point[] proj = mapGrid.getGridProjection();

    xdisp = new double[lx * ly];
    ydisp = new double[lx * ly];
    for (i = 0; i < lx; i++) {
      for (j = 0; j < ly; j++) {
        xdisp[i * ly + j] = proj[i * ly + j].x - i - 0.5;
        ydisp[i * ly + j] = proj[i * ly + j].y - j - 0.5;
      }
    }

    Point[][] polycorn = regionData.getRings();
    int n_poly = polycorn.length;
    Point[] proj2 = mapGrid.getGridProjectionSwapper();
    Point[][] cartcorn = regionData.getCartogramRings();

    for (i = 0; i < n_poly; i++) {
      Point[] polyI = polycorn[i];
      for (j = 0; j < polyI.length; j++) {
        Point pointIJ = polyI[j];
        Point p = cartcorn[i][j];
        Integrate.interpolate(lx, ly, pointIJ.x, pointIJ.y, xdisp, ydisp, p);
        p.x += pointIJ.x;
        p.y += pointIJ.y;
      }
    }
    if (proj_graticule) {

      for (i = 0; i < lx * ly; i++) {
        x2 = proj2[i].x;
        y2 = proj2[i].y;
        Integrate.interpolate(lx, ly, x2, y2, xdisp, ydisp, proj2[i]);
        proj2[i].x += x2;
        proj2[i].y += y2;
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

    public double getSummedPolygonArea() {
      return summedCartogramArea;
    }
  }

  public static AreaErrorResult calculateMaximumAreaError(double[] target_area, int[][] polyinreg, Point[][] corn) {
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
          cart_area[i] += PolygonUtilities.calculateOrientedArea(corn[polyI[j]]);
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
