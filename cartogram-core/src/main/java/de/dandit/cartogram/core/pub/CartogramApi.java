package de.dandit.cartogram.core.pub;

import de.dandit.cartogram.core.Cartogram;
import de.dandit.cartogram.core.ConvergenceGoalFailedException;
import de.dandit.cartogram.core.Density;
import de.dandit.cartogram.core.context.CartogramContext;
import de.dandit.cartogram.core.context.Point;

import java.util.*;

public class CartogramApi {

  public CartogramResult calculateGastnerCartogram(MapFeatureData mapFeatureData, CartogramConfig config) throws ConvergenceGoalFailedException {
    CartogramContext cartogramContext = Density.initializeContext(mapFeatureData, config);
    CartogramContext context = new Cartogram(cartogramContext)
      .calculate(config.getParallelismConfig(), config.isScaleToOriginalPolygonRegion(), config.getMaxPermittedAreaError());
    int[] regionIds = context.getRegionData().getRegionId();
    List<ResultRegion> resultRegions = new ArrayList<>();
    int[][] polyinreg = context.getRegionData().getRingInRegion();
    Point[][] cartcorn = context.getRegionData().getCartogramRings();
    int[][] ringsInPolygonByRegion = context.getRegionData().getRingsInPolygonByRegion();
    boolean[] regionNa = context.getRegionData().getRegionNaN();
    for (int i = 0; i < regionIds.length; i++) {
      ResultRegion resultRegion = createResultRegion(polyinreg[i], ringsInPolygonByRegion[i], cartcorn,
        regionNa[i]);
      resultRegions.add(resultRegion);
    }
    return new CartogramResult(
      resultRegions,
      cartogramContext.getMapGrid().getGridProjection(),
      cartogramContext.getMapGrid().getLx(),
      cartogramContext.getMapGrid().getLy());
  }

  private ResultRegion createResultRegion(int[] polyIndices,
                                          int[] ringIsHoleOfPolygon,
                                          Point[][] cartcorn,
                                          boolean regionNa) {
    Map<Integer, List<Point>> shells = new HashMap<>();
    Map<Integer, List<List<Point>>> holes = new HashMap<>();
    for (int j = 0; j < polyIndices.length; j++) {
      Point[] corners = cartcorn[polyIndices[j]];
      if (ringIsHoleOfPolygon[j] < 0) {
        shells.put(-(ringIsHoleOfPolygon[j] + 1), Arrays.asList(corners));
      } else {
        List<List<Point>> holesForShell = holes.computeIfAbsent(ringIsHoleOfPolygon[j], unused -> new ArrayList<>());
        holesForShell.add(Arrays.asList(corners));
      }
    }
    List<ResultPolygon> polygons = new ArrayList<>();
    for (var indexedShell : shells.entrySet()) {
      polygons.add(new ResultPolygon(
        indexedShell.getValue(),
        holes.getOrDefault(indexedShell.getKey(), List.of())));
    }

    return new ResultRegion(polygons, regionNa);
  }
}
