package de.dandit.cartogram.core.pub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dandit.cartogram.core.Cartogram;
import de.dandit.cartogram.core.ConvergenceGoalFailedException;
import de.dandit.cartogram.core.Density;
import de.dandit.cartogram.core.context.CartogramContext;

public class CartogramApi {

  public CartogramResult calculateGastnerCartogram(MapFeatureData mapFeatureData, CartogramConfig config) throws ConvergenceGoalFailedException {
    CartogramContext cartogramContext = Density.initializeContext(mapFeatureData, config);
    CartogramContext context = new Cartogram(cartogramContext)
      .calculate(config.getParallelismConfig(), config.isScaleToOriginalPolygonRegion(), config.getMaxPermittedAreaError());
    int[] regionIds = context.getRegionData().getRegionId();
    List<ResultRegion> resultRegions = new ArrayList<>();
    int[][] polyinreg = context.getRegionData().getRingInRegion();
    double[][] cartcornX = context.getRegionData().getCartogramRingsX();
    double[][] cartcornY = context.getRegionData().getCartogramRingsY();
    int[][] ringsInPolygonByRegion = context.getRegionData().getRingsInPolygonByRegion();
    boolean[] regionNa = context.getRegionData().getRegionNaN();
    for (int i = 0; i < regionIds.length; i++) {
      ResultRegion resultRegion = createResultRegion(polyinreg[i], ringsInPolygonByRegion[i], cartcornX, cartcornY,
        regionNa[i]);
      resultRegions.add(resultRegion);
    }
    return new CartogramResult(
      resultRegions,
      cartogramContext.getMapGrid().getGridProjectionX(),
      cartogramContext.getMapGrid().getGridProjectionY(),
      cartogramContext.getMapGrid().getLx(),
      cartogramContext.getMapGrid().getLy());
  }

  private ResultRegion createResultRegion(int[] polyIndices,
                                          int[] ringIsHoleOfPolygon,
                                          double[][] cartcornX,
                                          double[][] cartcornY,
                                          boolean regionNaN) {
    Map<Integer, double[]> shellsX = new HashMap<>();
    Map<Integer, double[]> shellsY = new HashMap<>();
    Map<Integer, List<double[]>> holesX = new HashMap<>();
    Map<Integer, List<double[]>> holesY = new HashMap<>();
    for (int j = 0; j < polyIndices.length; j++) {
      double[] cornersX = cartcornX[polyIndices[j]];
      double[] cornersY = cartcornY[polyIndices[j]];
      if (ringIsHoleOfPolygon[j] < 0) {
        int index = -(ringIsHoleOfPolygon[j] + 1);
        shellsX.put(index, cornersX);
        shellsY.put(index, cornersY);
      } else {
        List<double[]> holesXForShell = holesX.computeIfAbsent(ringIsHoleOfPolygon[j], unused -> new ArrayList<>());
        holesXForShell.add(cornersX);
        List<double[]> holesYForShell = holesY.computeIfAbsent(ringIsHoleOfPolygon[j], unused -> new ArrayList<>());
        holesYForShell.add(cornersY);
      }
    }
    List<ResultPolygon> polygons = new ArrayList<>();
    for (var indexedShellX : shellsX.entrySet()) {
      polygons.add(new ResultPolygon(
        indexedShellX.getValue(),
        shellsY.get(indexedShellX.getKey()),
        holesX.getOrDefault(indexedShellX.getKey(), List.of()),
        holesY.getOrDefault(indexedShellX.getKey(), List.of())));
    }

    return new ResultRegion(polygons, regionNaN);
  }
}
