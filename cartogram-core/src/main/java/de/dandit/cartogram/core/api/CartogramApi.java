package de.dandit.cartogram.core.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dandit.cartogram.core.Cartogram;
import de.dandit.cartogram.core.Density;
import de.dandit.cartogram.core.context.CartogramContext;

/**
 * Offers an API to calculate cartograms. Note that the input and output are not OGC geometries
 * to avoid having depedencies to a certain geotools/jts version. Using the cartogram-geo project
 * they can be easily converted from and to jts geometries and geotools simple features.
 */
public class CartogramApi {

  /**
   * Starts calculation of a area continuous cartogram using the input map feature data and configuration.
   * Algorithm used is the flow-based Gastner-Seguy-More algorithm (GaSeMo). For reference see
   * "Fast flow-based algorithm for creating density-equalizing map projections" (DOI:
   * 10.1073/pnas.1712674115).
   * Note that this implementation will retain the topology of the input regions and in most cases
   * produce results that can be converted to valid OGC geometries. In some cases the results can
   * contain self intersections or holes that are not contained within the polygon. Usually this means
   * that some LineStrings of the input have too long segments (relatively speaking) that should be split into multiple
   * segments that allow for better bending and transformations.
   * @param mapFeatureData Specifies the regions, the bounding box and target values.
   * @param config Specifies details of the execution, processing and allows adapting behavior like Logging.
   * @return A successfully processed cartogram, the used projection and error.
   * @throws ConvergenceGoalFailedException If convergence fails or is too slow. Can happen when tiny regions
   * need to be scaled to huge regions or vice versa.
   */
  public CartogramResult calculateGaSeMo(MapFeatureData mapFeatureData, CartogramConfig config) throws ConvergenceGoalFailedException {
    CartogramContext cartogramContext = Density.initializeContext(mapFeatureData, config);
    CartogramContext context = new Cartogram(cartogramContext)
      .calculate(config.getParallelismConfig(), config.isScaleToOriginalPolygonRegion(), config.getMaxPermittedAreaError());

    double maximumAreaError = Cartogram.calculateMaximumAreaError(
        context.getRegionData().getTargetArea(),
        context.getRegionData().getRingsInRegion(),
        context.getRegionData().getCartogramRingsX(),
        context.getRegionData().getCartogramRingsY())
        .getMaximumAreaError();

    int[] regionIds = context.getRegionData().getRegionId();
    List<ResultRegion> resultRegions = new ArrayList<>();
    int[][] ringsInRegion = context.getRegionData().getRingsInRegion();
    double[][] cartogramRingsX = context.getRegionData().getCartogramRingsX();
    double[][] cartogramRingsY = context.getRegionData().getCartogramRingsY();
    int[][] ringsInPolygonByRegion = context.getRegionData().getRingsInPolygonByRegion();
    boolean[] regionNaN = context.getRegionData().getRegionNaN();
    for (int i = 0; i < regionIds.length; i++) {
      ResultRegion resultRegion = createResultRegion(regionIds[i], ringsInRegion[i], ringsInPolygonByRegion[i], cartogramRingsX, cartogramRingsY,
        regionNaN[i]);
      resultRegions.add(resultRegion);
    }
    return new CartogramResult(
      maximumAreaError,
      resultRegions,
      cartogramContext.getMapGrid().getGridProjectionX(),
      cartogramContext.getMapGrid().getGridProjectionY(),
      cartogramContext.getMapGrid().getLx(),
      cartogramContext.getMapGrid().getLy());
  }

  private ResultRegion createResultRegion(int regionId, int[] ringsInRegion,
                                          int[] ringsInPolygon,
                                          double[][] cartogramRingsX,
                                          double[][] cartogramRingsY,
                                          boolean regionNaN) {
    Map<Integer, double[]> ringsX = new HashMap<>();
    Map<Integer, double[]> ringsY = new HashMap<>();
    Map<Integer, List<double[]>> holesX = new HashMap<>();
    Map<Integer, List<double[]>> holesY = new HashMap<>();
    for (int j = 0; j < ringsInRegion.length; j++) {
      double[] currentRingX = cartogramRingsX[ringsInRegion[j]];
      double[] currentRingY = cartogramRingsY[ringsInRegion[j]];
      if (ringsInPolygon[j] < 0) {
        int index = -(ringsInPolygon[j] + 1);
        ringsX.put(index, currentRingX);
        ringsY.put(index, currentRingY);
      } else {
        List<double[]> interiorRingsX = holesX.computeIfAbsent(ringsInPolygon[j], unused -> new ArrayList<>());
        interiorRingsX.add(currentRingX);
        List<double[]> interiorRingsY = holesY.computeIfAbsent(ringsInPolygon[j], unused -> new ArrayList<>());
        interiorRingsY.add(currentRingY);
      }
    }
    List<LightPolygon> polygons = new ArrayList<>();
    for (var indexedRingX : ringsX.entrySet()) {
      Integer key = indexedRingX.getKey();
      polygons.add(new LightPolygon(
        indexedRingX.getValue(),
        ringsY.get(key),
        holesX.getOrDefault(key, List.of()),
        holesY.getOrDefault(key, List.of())));
    }

    return new ResultRegion(regionId, polygons, regionNaN);
  }
}
