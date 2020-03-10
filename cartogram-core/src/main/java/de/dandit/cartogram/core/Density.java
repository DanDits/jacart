package de.dandit.cartogram.core;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import de.dandit.cartogram.core.api.*;
import de.dandit.cartogram.core.context.CartogramContext;
import de.dandit.cartogram.core.context.MapGrid;
import de.dandit.cartogram.core.context.PolygonData;
import de.dandit.cartogram.core.context.RegionData;
import de.dandit.cartogram.core.dft.FftPlan2D;

public class Density {
  /**
   * Defines the grid's resolution. Must be a multiple of 2.
   */
  public static final int L = 512;

  /**
   * Defines a padding for placing the initial polygons within the grid.
   * If too low (<=1) it forces data to be almost at the border which will reject many
   * integration time steps anc convergence will be slow. If too big (<=3) convergence will also be slow
   * because there is a huge padding and a lot of the grid is wasted since only some parts actually
   * participate to transform the data.
   */
  private static final double PADDING = 1.5;

  private static final double BLUR_WIDTH = 5e0;

  /**
   * Defines a factor to replace a target area of 0 with.
   * Will use the positive minimum target area multiplied by this factor.
   */
  private static final double MIN_POP_FAC = 0.2;
  private static final double MIN_PERIMETER_FAC = 0.025;
  private final CartogramContext context;

  public Density(CartogramContext context) {
    this.context = context;
  }

  private static PolygonData initPolygonData(MapFeatureData featureData) {
    List<Region> regions = featureData.getRegions();
    int ringCount = regions.stream()
      .mapToInt(region -> getRingCount(region.getPolygons()))
      .sum();

    double[][] ringsX = new double[ringCount][];
    double[][] ringsY = new double[ringCount][];
    int[] polygonId = new int[ringCount];
    int[][] ringsInPolygonByRegion = new int[regions.size()][];
    int ringCounter = 0;
    int regionCounter = 0;
    for (Region region : regions) {
      int[] ringsInPolygon = new int[getRingCount(region.getPolygons())];
      int currentPolygonIndex = 0;
      int currentRingCounter = 0;
      for (LightPolygon polygon : region.getPolygons()) {
        ringsX[ringCounter] = polygon.getExteriorRingX();
        ringsY[ringCounter] = polygon.getExteriorRingY();
        polygonId[ringCounter] = region.getId();
        ringsInPolygon[currentRingCounter] = -(currentPolygonIndex + 1);
        ringCounter++;
        currentRingCounter++;

        for (int i = 0; i < polygon.getInteriorRingsX().size(); i++) {
          ringsX[ringCounter] = polygon.getInteriorRingsX().get(i);
          ringsY[ringCounter] = polygon.getInteriorRingsY().get(i);
          polygonId[ringCounter] = region.getId();
          ringsInPolygon[currentRingCounter] = currentPolygonIndex;
          ringCounter++;
          currentRingCounter++;
        }
        currentPolygonIndex++;
      }
      ringsInPolygonByRegion[regionCounter] = ringsInPolygon;
      regionCounter++;
    }
    return new PolygonData(ringsX, ringsY, polygonId, ringsInPolygonByRegion);
  }

  private static int getRingCount(List<LightPolygon> polygons) {
    return polygons.stream()
      .mapToInt(polygon -> polygon.getInteriorRingsX().size() + 1)
      .sum();
  }

  private static MapGrid transformMapToLSpace(FftPlanFactory fftPlanFactory, Logging logging, MapFeatureData featureData, double[][] ringsX, double[][] ringsY) {
    double mapMinX = featureData.getMapMinX();
    double mapMinY = featureData.getMapMinY();
    double mapMaxX = featureData.getMapMaxX();
    double mapMaxY = featureData.getMapMaxY();

    double newMaxX = 0.5 * ((1.0 + PADDING) * mapMaxX + (1.0 - PADDING) * mapMinX);
    double newMinX = 0.5 * ((1.0 - PADDING) * mapMaxX + (1.0 + PADDING) * mapMinX);
    double newMaxY = 0.5 * ((1.0 + PADDING) * mapMaxY + (1.0 - PADDING) * mapMinY);
    double newMinY = 0.5 * ((1.0 - PADDING) * mapMaxY + (1.0 + PADDING) * mapMinY);

    // retain aspect ratio, setting either lx or ly to the maximum L
    double scale;
    int lx, ly;
    if (mapMaxX - mapMinX > mapMaxY - mapMinY) {
      lx = L;
      scale = (newMaxX - newMinX) / L;
      ly = 1 << ((int) Math.ceil(Math.log((newMaxY - newMinY) / scale) / Math.log(2)));
      newMaxY = 0.5 * (mapMaxY + mapMinY) + 0.5 * ly * scale;
      newMinY = 0.5 * (mapMaxY + mapMinY) - 0.5 * ly * scale;
    } else {
      ly = L;
      scale = (newMaxY - newMinY) / L;
      lx = 1 << ((int) Math.ceil(Math.log((newMaxX - newMinX) / scale) / Math.log(2)));
      newMaxX = 0.5 * (mapMaxX + mapMinX) + 0.5 * lx * scale;
      newMinX = 0.5 * (mapMaxX + mapMinX) - 0.5 * lx * scale;
    }
    logging.debug(
      "Using a {0} x {1} lattice with bounding box\n\t({2} {3} {4} {5}).\n",
      lx, ly, newMinX, newMinY, newMaxX, newMaxY);


    for (double[] pointsX : ringsX) {
      for (int i = 0; i < pointsX.length; i++) {
        pointsX[i] = (pointsX[i] - newMinX) / scale;
      }
    }
    for (double[] pointsY : ringsY) {
      for (int i = 0; i < pointsY.length; i++) {
        pointsY[i] = (pointsY[i] - newMinY) / scale;
      }
    }
    return new MapGrid(fftPlanFactory, lx, ly, newMinX, newMinY, scale);
  }

  public static void setInsideValuesForPolygon(int region, double[] ringX, double[] ringY, int[][] inside) {
    double minX = ringX[0];
    int pointCount = ringX.length;
    for (double point : ringX) {
      minX = Math.min(minX, point);
    }
    for (int k = 0, n = pointCount - 1; k < pointCount; n = k++) {
      setInsideValuesBetweenPoints(region, ringX[k], ringY[k], ringX[n], ringY[n],
        minX, inside);
    }
  }

  private static void setInsideValuesBetweenPoints(int region, double pkX, double pkY, double pnX, double pnY,
                                                       double ringMinX, int[][] inside) {
    for (int l = (int) Math.ceil(Math.min(pnY, pkY) - 0.5); l < Math.max(pnY - 0.5, pkY - 0.5); l++) {
      setInsideValueAtY(region, pkX, pkY, pnX, pnY, l, ringMinX, inside);
    }
  }

  private static void setInsideValueAtY(int region, double pkX, double pkY, double pnX, double pnY, int l,
                                            double ringMinX, int[][] inside) {
    double intersection = (pnX - 0.5 - (pkX - 0.5)) * (l - (pkY - 0.5)) /
      (pnY - 0.5 - (pkY - 0.5)) + (pkX - 0.5);
    for (int m = (int) ringMinX; m < intersection; m++) {
      inside[m][l] = region - inside[m][l] - 1;
    }
  }

  public static CartogramContext initializeContext(MapFeatureData featureData, CartogramConfig config) {
    PolygonData polygonData = initPolygonData(featureData);
    Logging logging = config.getLogging();
    RegionData regionData = PolygonUtilities.processMap(logging, featureData, polygonData);
    logging.debug("Amount of regions: {0}", regionData.getRegionId().length);
    MapGrid mapGrid = transformMapToLSpace(config.getFftPlanFactory(), logging, featureData, regionData.getRingsX(), regionData.getRingsY());

    int regionCount = regionData.getRingsInRegion().length;
    double[] targetArea = regionData.getTargetArea();
    boolean[] regionHasNaN = regionData.getRegionNaN();
    if (regionCount == 1) {
      targetArea[0] = 1.0;
      return new CartogramContext(logging, mapGrid, regionData, true);
    }

    double[] density = new double[regionCount];
    double[] initialArea = new double[regionCount];

    interior(mapGrid, regionData);

    double[] featureTargetArea = featureData.getTargetAreaPerRegion();

    for (int i = 0; i < regionCount; i++) {
      targetArea[i] = featureTargetArea[i];
      if (Double.isNaN(targetArea[i])) {
        regionHasNaN[i] = true;
        targetArea[i] = 0.;
      }
    }

    int[] regionId = regionData.getRegionId();
    for (int i = 0; i < regionCount; i++) {
      if (targetArea[i] < 0.0 && !Double.isNaN(targetArea[i])) {
        throw new IllegalArgumentException(
          MessageFormat.format("ERROR: No target area for region {0}", regionId[i]));
      }
    }
    logging.displayDoubleArray( "targetArea", targetArea);

    double tempTotalTargetArea = 0.0;
    double totalInitialArea = 0.0;
    int[][] ringInRegion = regionData.getRingsInRegion();
    double[][] ringsX = regionData.getRingsX();
    double[][] ringsY = regionData.getRingsY();
    double[] regionPerimeter = regionData.getRegionPerimeter();
    for (int i = 0; i < regionCount; i++) {
      int[] polyI = ringInRegion[i];
      if (!regionHasNaN[i]) {
        tempTotalTargetArea += targetArea[i];
      }
      for (int value : polyI) {
        initialArea[i] += PolygonUtilities.calculateOrientedArea(
            ringsX[value],
            ringsY[value]);
      }
      totalInitialArea += initialArea[i];
    }
    logging.debug("Total init area= {0}", totalInitialArea);
    logging.displayDoubleArray( "initial area", initialArea);

    for (int i = 0; i < regionCount; i++) {
      int[] polyI = ringInRegion[i];
      for (int value : polyI) {
        regionPerimeter[i] += PolygonUtilities.calculatePolygonPerimeter(
            ringsX[value],
            ringsY[value]);
      }
    }
    logging.displayDoubleArray( "region perimeter", regionPerimeter);
    boolean firstRegion = true;
    double totalNaNRatio = 0;

    for (int i = 0; i < regionCount; i++) {
      if (regionHasNaN[i]) {
        totalNaNRatio += initialArea[i] / totalInitialArea;
      }
    }

    double totalNanArea = (totalNaNRatio * tempTotalTargetArea) / (1 - totalNaNRatio);
    tempTotalTargetArea += totalNanArea;

    for (int i = 0; i < regionCount; i++) {
      if (regionHasNaN[i]) {
        if (firstRegion) {
          logging.debug("Setting area for NaN regions:");
          firstRegion = false;
        }
        targetArea[i] = (initialArea[i] / totalInitialArea) / totalNaNRatio * totalNanArea;
        logging.debug("\tRegion id {0}: {1}", regionId[i], targetArea[i]);
      }
    }

    if (config.isUsePerimeterThreshold()) {
      applyScaledPerimeterThreshold(
          logging,
          regionCount,
          targetArea,
          regionHasNaN,
          regionId,
          tempTotalTargetArea,
          regionPerimeter);
    } else {
      applyRegularTargetArea(logging, regionCount, targetArea);
    }
    logging.displayDoubleArray( "targetArea", targetArea);

    for (int i = 0; i < regionCount; i++) {
      density[i] = targetArea[i] / initialArea[i];
    }

    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();

    initializeRhoWithDensity(
        mapGrid,
        regionCount,
        targetArea,
        density,
        totalInitialArea,
        lx,
        ly);
    gaussianBlur(config.getFftPlanFactory(), lx, ly, mapGrid.getRhoInit(), mapGrid.getRhoFt(), mapGrid.getRho());
    mapGrid.getRho().execute();
    return new CartogramContext(logging, mapGrid, regionData, false);
  }

  private static void initializeRhoWithDensity(
      MapGrid mapGrid,
      int regionCount,
      double[] targetArea, double[] density, double totalInitialArea, int lx, int ly) {
    double summedTargetArea = 0.;
    for (int i = 0; i < regionCount; i++) {
      summedTargetArea += targetArea[i];
    }
    double averageDensity = summedTargetArea / totalInitialArea;

    int[][] gridIndexToRegionIndex = mapGrid.getGridIndexToRegionIndex();
    double[] rhoInit = mapGrid.getRhoInit();
    for (int i = 0; i < lx; i++) {
      for (int j = 0; j < ly; j++) {
        if (gridIndexToRegionIndex[i][j] == -1) {
          rhoInit[i * ly + j] = averageDensity;
        } else {
          rhoInit[i * ly + j] = density[gridIndexToRegionIndex[i][j]];
        }
      }
    }
  }

  private static void applyRegularTargetArea(
      Logging logging,
      int regionCount,
      double[] targetArea) {
    logging.debug("Note: Not using scaled perimeter threshold.");
    double minimumArea = Double.MAX_VALUE;
    for (int i = 1; i < regionCount; i++) {
      if (targetArea[i] > 0.0) {
        if (minimumArea <= 0.0) {
          minimumArea = targetArea[i];
        } else {
          minimumArea = Math.min(minimumArea, targetArea[i]);
        }
      }
    }
    for (int i = 0; i < regionCount; i++) {
      if (targetArea[i] == 0.0) {
        targetArea[i] = MIN_POP_FAC * minimumArea;
      }
    }
  }

  private static void applyScaledPerimeterThreshold(
      Logging logging,
      int regionCount,
      double[] targetArea,
      boolean[] regionHasNaN,
      int[] regionId,
      double tempTotalTargetArea,
      double[] regionPerimeter) {
    logging.debug("Note: Enlarging extremely small regions using scaled perimeter threshold.");
    boolean[] regionIsSmall = new boolean[regionCount];
    int smallRegionCounter = 0;
    double totRegionSmallArea = 0, summedPerimeter = 0, summedThreshold = 0;
    double[] regionThreshold = new double[regionCount];
    double[] regionThresholdArea = new double[regionCount];
    for (int i = 0; i < regionCount; i++) {
      summedPerimeter += regionPerimeter[i];
    }
    for (int i = 0; i < regionCount; i++) {
      regionThreshold[i] = Math.max((regionPerimeter[i] / summedPerimeter) * MIN_PERIMETER_FAC, 0.00025);
      if (!regionHasNaN[i] && (targetArea[i] / tempTotalTargetArea < regionThreshold[i])) {
        regionIsSmall[i] = true;
        smallRegionCounter++;
        totRegionSmallArea += targetArea[i];
      }
    }
    for (int i = 0; i < regionCount; i++) {
      if (regionIsSmall[i]) {
        summedThreshold += regionThreshold[i];
      }
    }
    double totalThresholdArea = (summedThreshold * (tempTotalTargetArea - totRegionSmallArea)) / (1 - summedThreshold);

    if (smallRegionCounter > 0) {
      logging.debug("Enlarging small regions:");
    }

    for (int i = 0; i < regionCount; i++) {
      if (regionIsSmall[i]) {
        regionThresholdArea[i] = (regionThreshold[i] / summedThreshold) * totalThresholdArea;
        double oldTargetArea = targetArea[i];
        targetArea[i] = regionThresholdArea[i];
        tempTotalTargetArea += targetArea[i];
        tempTotalTargetArea -= oldTargetArea;
        logging.debug("Enlarging region id {0}: from {1} to {2}", regionId[i], oldTargetArea, targetArea[i]);
      }
    }
    if (smallRegionCounter <= 0) {
      logging.debug("No regions below minimum threshold.\n\n");
    }
  }

  void fillWithDensity() {
    MapGrid mapGrid = context.getMapGrid();
    RegionData regionData = context.getRegionData();
    double[][] ringsX = regionData.getRingsX();
    double[][] ringsY = regionData.getRingsY();
    int ringsCount = ringsX.length;
    double[][] cartogramRingsX = regionData.getCartogramRingsX();
    double[][] cartogramRingsY = regionData.getCartogramRingsY();
    double[] rhoInit = mapGrid.getRhoInit();
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();

    for (int i = 0; i < ringsCount; i++) {
      for (int j = 0; j < ringsX[i].length; j++) {
        ringsX[i][j] = cartogramRingsX[i][j];
        ringsY[i][j] = cartogramRingsY[i][j];
      }
    }

    int[][] ringInRegion = regionData.getRingsInRegion();
    int regionCount = ringInRegion.length;
    double[] targetArea = regionData.getTargetArea();
    int[][] gridIndexToRegionIndex = mapGrid.getGridIndexToRegionIndex();

    double[] dens = new double[regionCount];
    double[] tempArea = new double[regionCount];

    interior(mapGrid, regionData);

    for (int i = 0; i < regionCount; i++) {
      int[] polyI = ringInRegion[i];
      for (int value : polyI) {
        tempArea[i] += PolygonUtilities.calculateOrientedArea(ringsX[value], ringsY[value]);
      }
    }
    for (int i = 0; i < regionCount; i++) dens[i] = targetArea[i] / tempArea[i];

    double summedTempArea = 0.0;
    for (int i = 0; i < regionCount; i++) {
      summedTempArea += tempArea[i];
    }
    double totalTargetArea = 0.0;
    for (int i = 0; i < regionCount; i++) {
      totalTargetArea += targetArea[i];
    }
    double averageDensity = totalTargetArea / summedTempArea;

    for (int i = 0; i < lx; i++) {
      for (int j = 0; j < ly; j++) {
        if (gridIndexToRegionIndex[i][j] == -1) {
          rhoInit[i * ly + j] = averageDensity;
        } else {
          rhoInit[i * ly + j] = dens[gridIndexToRegionIndex[i][j]];
        }
      }
    }
    mapGrid.getRho().execute();
  }

  private static void interior(MapGrid mapGrid, RegionData regionData) {
    double[][] ringsX = regionData.getRingsX();
    double[][] ringsY = regionData.getRingsY();
    int[][] gridIndexToRegionIndex = mapGrid.getGridIndexToRegionIndex();
    int[][] ringsInRegion = regionData.getRingsInRegion();
    int regionCount = ringsInRegion.length;
    for (int[] indexToRegionIndex : gridIndexToRegionIndex) {
      Arrays.fill(indexToRegionIndex, -1);
    }

    for (int i = 0; i < regionCount; i++) {
      int[] polyI = ringsInRegion[i];
      for (int poly : polyI) {
        setInsideValuesForPolygon(i, ringsX[poly], ringsY[poly],
            gridIndexToRegionIndex);
      }
    }
  }

  private static void gaussianBlur(FftPlanFactory fftPlanFactory,
      int lx, int ly, double[] rhoInit, double[] rhoFt, FftPlan2D rho) {
    FftPlan2D backwardPlan = fftPlanFactory.createDCT3_2D(lx, ly, rhoFt, rhoInit);
    for (int i = 0; i < lx * ly; i++) {
      rhoInit[i] /= 4 * lx * ly;
    }
    rho.execute();
    gaussianBlur(lx, ly, rhoFt);
    backwardPlan.execute();
  }

  private static void gaussianBlur(int lx, int ly, double[] rhoFt) {
    double scale = -0.5 * BLUR_WIDTH * BLUR_WIDTH * Math.PI * Math.PI;
    for (int i = 0; i < lx; i++) {
      double scaleI = (double) i / lx;
      for (int j = 0; j < ly; j++) {
        double scaleJ = (double) j / ly;
        rhoFt[i * ly + j] *=
          Math.exp(scale * (scaleI * scaleI + scaleJ * scaleJ));
      }
    }
  }
}
