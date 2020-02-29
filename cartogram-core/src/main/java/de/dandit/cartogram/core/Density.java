package de.dandit.cartogram.core;

import java.text.MessageFormat;
import java.util.List;

import de.dandit.cartogram.core.dft.FftPlan2D;
import de.dandit.cartogram.core.pub.CartogramConfig;
import de.dandit.cartogram.core.context.CartogramContext;
import de.dandit.cartogram.core.pub.MapFeatureData;
import de.dandit.cartogram.core.context.MapGrid;
import de.dandit.cartogram.core.context.Point;
import de.dandit.cartogram.core.context.PolygonData;
import de.dandit.cartogram.core.context.Region;
import de.dandit.cartogram.core.context.RegionData;
import de.dandit.cartogram.core.pub.FftPlanFactory;
import de.dandit.cartogram.core.pub.Logging;

public class Density {
  /**
   * Defines the grid's resolution. Must be a multiple of 2.
   */
  public static final int L = 512;

  /**
   * Defines a padding for placing the initial polygons within the grid.
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

  private static PolygonData initPolycornAndPolygonId(MapFeatureData featureData) {
    List<Region> regions = featureData.getRegions();
    int ringCount = regions.stream()
      .mapToInt(region -> region.getPolygonRings().length)
      .sum();

    Point[][] rings = new Point[ringCount][];
    int ringCounter = 0;
    int[] polygonId = new int[ringCount];
    for (Region region : regions) {
      for (Point[] ring : region.getPolygonRings()) {
        rings[ringCounter] = ring;
        polygonId[ringCounter] = region.getId();
        ringCounter++;
      }
    }
    return new PolygonData(rings, polygonId);
  }

  private static MapGrid transformMapToLSpace(FftPlanFactory fftPlanFactory, Logging logging, MapFeatureData featureData, Point[][] polycorn) {
    double latt_const, new_maxx, new_maxy, new_minx, new_miny;
    int lx, ly;
    double map_minx = featureData.getMap_minx();
    double map_miny = featureData.getMap_miny();
    double map_maxx = featureData.getMap_maxx();
    double map_maxy = featureData.getMap_maxy();

    new_maxx = 0.5 * ((1.0 + PADDING) * map_maxx + (1.0 - PADDING) * map_minx);
    new_minx = 0.5 * ((1.0 - PADDING) * map_maxx + (1.0 + PADDING) * map_minx);
    new_maxy = 0.5 * ((1.0 + PADDING) * map_maxy + (1.0 - PADDING) * map_miny);
    new_miny = 0.5 * ((1.0 - PADDING) * map_maxy + (1.0 + PADDING) * map_miny);

    // retain aspect ratio, setting either lx or ly to the maximum L
    if (map_maxx - map_minx > map_maxy - map_miny) {
      lx = L;
      latt_const = (new_maxx - new_minx) / L;
      ly = 1 << ((int) Math.ceil(Math.log((new_maxy - new_miny) / latt_const) / Math.log(2)));
      new_maxy = 0.5 * (map_maxy + map_miny) + 0.5 * ly * latt_const;
      new_miny = 0.5 * (map_maxy + map_miny) - 0.5 * ly * latt_const;
    } else {
      ly = L;
      latt_const = (new_maxy - new_miny) / L;
      lx = 1 << ((int) Math.ceil(Math.log((new_maxx - new_minx) / latt_const) / Math.log(2)));
      new_maxx = 0.5 * (map_maxx + map_minx) + 0.5 * lx * latt_const;
      new_minx = 0.5 * (map_maxx + map_minx) - 0.5 * lx * latt_const;
    }
    logging.debug(
      "Using a {0} x {1} lattice with bounding box\n\t({2} {3} {4} {5}).\n",
      lx, ly, new_minx, new_miny, new_maxx, new_maxy);


    for (Point[] points : polycorn) {
      for (Point point : points) {
        point.x = (point.x - new_minx) / latt_const;
        point.y = (point.y - new_miny) / latt_const;
      }
    }
    return new MapGrid(fftPlanFactory, lx, ly, new_minx, new_miny, latt_const);
  }

  public static void set_inside_values_for_polygon(int region, Point[] polycorn, int[][] inside) {
    double poly_minx = polycorn[0].x;
    int n_polycorn = polycorn.length;
    for (Point point : polycorn) {
      poly_minx = Math.min(poly_minx, point.x);
    }
    for (int k = 0, n = n_polycorn - 1; k < n_polycorn; n = k++) {
      set_inside_values_between_points(region, polycorn[k], polycorn[n],
        poly_minx, inside);
    }
  }

  private static void set_inside_values_between_points(int region, Point pk, Point pn,
                                                       double poly_minx, int[][] inside) {
    for (int l = (int) Math.ceil(Math.min(pn.y, pk.y) - 0.5); l < Math.max(pn.y - 0.5, pk.y - 0.5); l++) {
      set_inside_value_at_y(region, pk, pn, l, poly_minx, inside);
    }
  }

  private static void set_inside_value_at_y(int region, Point pk, Point pn, int l,
                                            double poly_minx, int[][] inside) {
    double intersection = (pn.x - 0.5 - (pk.x - 0.5)) * (l - (pk.y - 0.5)) /
      (pn.y - 0.5 - (pk.y - 0.5)) + (pk.x - 0.5);
    for (int m = (int) poly_minx; m < intersection; m++) {
      inside[m][l] = region - inside[m][l] - 1;
    }
  }

  public static CartogramContext initializeContext(MapFeatureData featureData, CartogramConfig config) {
    PolygonData polygonData = initPolycornAndPolygonId(featureData);
    Logging logging = config.getLogging();
    RegionData regionData = PolygonUtilities.processMap(logging, featureData, polygonData);
    logging.debug("Amount of regions: {0}", regionData.getRegionId().length);
    MapGrid mapGrid = transformMapToLSpace(config.getFftPlanFactory(), logging, featureData, regionData.getRings());

    int regionCount = regionData.getRingInRegion().length;
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
    logging.displayDoubleArray( "target_area", targetArea);

    double tempTotalTargetArea = 0.0;
    double totalInitialArea = 0.0;
    int[][] polyinreg = regionData.getRingInRegion();
    Point[][] polycorn = regionData.getRings();
    double[] regionPerimeter = regionData.getRegionPerimeter();
    for (int i = 0; i < regionCount; i++) {
      int[] polyI = polyinreg[i];
      if (!regionHasNaN[i]) {
        tempTotalTargetArea += targetArea[i];
      }
      for (int j = 0; j < polyI.length; j++) {
        initialArea[i] += PolygonUtilities.calculateOrientedArea(polycorn[polyI[j]]);
      }
      totalInitialArea += initialArea[i];
    }
    logging.debug("Total init area= {0}", totalInitialArea);
    logging.displayDoubleArray( "init_area", initialArea);

    for (int i = 0; i < regionCount; i++) {
      int[] polyI = polyinreg[i];
      for (int j = 0; j < polyI.length; j++) {
        regionPerimeter[i] += PolygonUtilities.calculatePolygonPerimeter(polycorn[polyI[j]]);
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

    double total_NA_area = (totalNaNRatio * tempTotalTargetArea) / (1 - totalNaNRatio);
    tempTotalTargetArea += total_NA_area;

    for (int i = 0; i < regionCount; i++) {
      if (regionHasNaN[i]) {
        if (firstRegion) {
          logging.debug("Setting area for NaN regions:");
          firstRegion = false;
        }
        targetArea[i] = (initialArea[i] / totalInitialArea) / totalNaNRatio * total_NA_area;
        logging.debug("\tRegion id {0}: {1}", regionId[i], targetArea[i]);
      }
    }

    if (config.isUsePerimeterThreshold()) {
      logging.debug("Note: Enlarging extremely small regions using scaled perimeter threshold.");
      boolean[] regionIsSmall = new boolean[regionCount];
      int smallRegionCounter = 0;
      double tot_region_small_area = 0, total_perimeter = 0, totalThreshold = 0;
      double[] region_threshold = new double[regionCount];
      double[] region_threshold_area = new double[regionCount];
      for (int i = 0; i < regionCount; i++) {
        total_perimeter += regionPerimeter[i];
      }
      for (int i = 0; i < regionCount; i++) {
        region_threshold[i] = Math.max((regionPerimeter[i] / total_perimeter) * MIN_PERIMETER_FAC, 0.00025);
        if (!regionHasNaN[i] && (targetArea[i] / tempTotalTargetArea < region_threshold[i])) {
          regionIsSmall[i] = true;
          smallRegionCounter++;
          tot_region_small_area += targetArea[i];
        }
      }
      for (int i = 0; i < regionCount; i++) {
        if (regionIsSmall[i]) {
          totalThreshold += region_threshold[i];
        }
      }
      double total_threshold_area = (totalThreshold * (tempTotalTargetArea - tot_region_small_area)) / (1 - totalThreshold);

      if (smallRegionCounter > 0) {
        logging.debug("Enlarging small regions:");
      }

      for (int i = 0; i < regionCount; i++) {
        if (regionIsSmall[i]) {
          region_threshold_area[i] = (region_threshold[i] / totalThreshold) * total_threshold_area;
          double oldTargetArea = targetArea[i];
          targetArea[i] = region_threshold_area[i];
          tempTotalTargetArea += targetArea[i];
          tempTotalTargetArea -= oldTargetArea;
          logging.debug("Enlarging region id {0}: from {1} to {2}", regionId[i], oldTargetArea, targetArea[i]);
        }
      }
      if (smallRegionCounter <= 0) {
        logging.debug("No regions below minimum threshold.\n\n");
      }
    } else {
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
    logging.displayDoubleArray( "target_area", targetArea);

    for (int i = 0; i < regionCount; i++) {
      density[i] = targetArea[i] / initialArea[i];
    }

    double tot_target_area = 0.;
    for (int i = 0; i < regionCount; i++) {
      tot_target_area += targetArea[i];
    }
    double averageDensity = tot_target_area / totalInitialArea;

    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    int[][] gridIndexToRegionIndex = mapGrid.getGridIndexToRegionIndex();
    double[] rho_init = mapGrid.getRhoInit();
    for (int i = 0; i < lx; i++) {
      for (int j = 0; j < ly; j++) {
        if (gridIndexToRegionIndex[i][j] == -1) {
          rho_init[i * ly + j] = averageDensity;
        } else {
          rho_init[i * ly + j] = density[gridIndexToRegionIndex[i][j]];
        }
      }
    }

    gaussian_blur(config.getFftPlanFactory(), mapGrid, totalInitialArea, averageDensity);

    mapGrid.getRho().execute();

    return new CartogramContext(logging, mapGrid, regionData, false);
  }

  void fill_with_density2() {
    double avg_dens, tot_target_area, tot_tmp_area;
    double[] dens, tmp_area;
    int i, j;

    MapGrid mapGrid = context.getMapGrid();
    RegionData regionData = context.getRegionData();
    Point[][] polycorn = regionData.getRings();
    int n_poly = polycorn.length;
    Point[][] cartcorn = regionData.getCartogramRings();
    double[] rho_init = mapGrid.getRhoInit();
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();

    for (i = 0; i < n_poly; i++) {
      for (j = 0; j < polycorn[i].length; j++) {
        polycorn[i][j] = cartcorn[i][j].createCopy();
      }
    }

    int[][] polyinreg = regionData.getRingInRegion();
    int n_reg = polyinreg.length;
    double[] target_area = regionData.getTargetArea();
    int[][] xyhalfshift2reg = mapGrid.getGridIndexToRegionIndex();

    dens = new double[n_reg];
    tmp_area = new double[n_reg];

    interior(mapGrid, regionData);

    for (i = 0; i < n_reg; i++) {
      int[] polyI = polyinreg[i];
      for (j = 0; j < polyI.length; j++) {
        tmp_area[i] += PolygonUtilities.calculateOrientedArea(polycorn[polyI[j]]);
      }
    }
    for (i = 0; i < n_reg; i++) dens[i] = target_area[i] / tmp_area[i];

    for (i = 0, tot_tmp_area = 0.0; i < n_reg; i++)
      tot_tmp_area += tmp_area[i];
    for (i = 0, tot_target_area = 0.0; i < n_reg; i++)
      tot_target_area += target_area[i];
    avg_dens = tot_target_area / tot_tmp_area;

    for (i = 0; i < lx; i++)
      for (j = 0; j < ly; j++) {
        if (xyhalfshift2reg[i][j] == -1)
          rho_init[i * ly + j] = avg_dens;
        else
          rho_init[i * ly + j] = dens[xyhalfshift2reg[i][j]];
      }
    mapGrid.getRho().execute();
  }

  private static void interior(MapGrid mapGrid, RegionData regionData) {
    int i, j, poly;

    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    Point[][] polycorn = regionData.getRings();
    int[][] xyhalfshift2reg = mapGrid.getGridIndexToRegionIndex();
    int[][] polyinreg = regionData.getRingInRegion();
    int n_reg = polyinreg.length;
    for (i = 0; i < lx; i++) {
      for (j = 0; j < ly; j++) {
        xyhalfshift2reg[i][j] = -1;
      }
    }

    for (i = 0; i < n_reg; i++) {
      int[] polyI = polyinreg[i];
      for (j = 0; j < polyI.length; j++) {
        poly = polyI[j];
        set_inside_values_for_polygon(i, polycorn[poly],
          xyhalfshift2reg);
      }
    }
  }

  private static void gaussian_blur(FftPlanFactory fftPlanFactory, MapGrid mapGrid, double tot_init_area, double avg_dens) {
    FftPlan2D plan_bwd;
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    double[] rho_init = mapGrid.getRhoInit();
    double[] rho_ft = mapGrid.getRhoFt();

    plan_bwd = fftPlanFactory.createDCT3_2D(lx, ly, rho_ft, rho_init);

    for (int i = 0; i < lx * ly; i++)
      rho_init[i] /= 4 * lx * ly;

    mapGrid.getRho().execute();

    gaussianBlur(lx, ly, rho_ft);
    plan_bwd.execute();

  }

  private static void gaussianBlur(int lx, int ly, double[] rho_ft) {
    double prefactor;
    int i;
    double scale_i;
    int j;
    double scale_j;
    prefactor = -0.5 * BLUR_WIDTH * BLUR_WIDTH * Math.PI * Math.PI;
    for (i = 0; i < lx; i++) {
      scale_i = (double) i / lx;
      for (j = 0; j < ly; j++) {
        scale_j = (double) j / ly;
        rho_ft[i * ly + j] *=
          Math.exp(prefactor * (scale_i * scale_i + scale_j * scale_j));
      }
    }
  }
}
