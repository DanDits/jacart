package dan.dit.cartogram.core.pub;

import dan.dit.cartogram.core.Cartogram;
import dan.dit.cartogram.core.ConvergenceGoalFailedException;
import dan.dit.cartogram.core.Density;
import dan.dit.cartogram.core.context.CartogramContext;
import dan.dit.cartogram.core.context.Point;
import dan.dit.cartogram.core.context.Region;

import java.util.*;

public class CartogramApi {

  public CartogramResult execute(MapFeatureData mapFeatureData, CartogramConfig config) throws ConvergenceGoalFailedException {
    CartogramContext cartogramContext = Density.initializeContext(mapFeatureData, config);
    CartogramContext context = new Cartogram(cartogramContext)
      .calculate(config.isScaleToOriginalPolygonSize(), config.getMaxPermittedAreaError());
    int[] regionIds = context.getRegionData().getRegion_id();
    List<ResultRegion> resultRegions = new ArrayList<>();
    int[][] polyinreg = context.getRegionData().getPolyinreg();
    Point[][] cartcorn = context.getRegionData().getCartcorn();
    int[][] ringsInPolygonByRegion = context.getRegionData().getRingsInPolygonByRegion();
    boolean[] regionNa = context.getRegionData().getRegion_na();
    for (int i = 0; i < regionIds.length; i++) {
      ResultRegion resultRegion = createResultRegion(polyinreg[i], ringsInPolygonByRegion[i], cartcorn,
        regionNa[i]);
      resultRegions.add(resultRegion);
    }
    return new CartogramResult(
      resultRegions,
      cartogramContext.getMapGrid().getProj(),
      cartogramContext.getMapGrid().getLx(),
      cartogramContext.getMapGrid().getLy());
  }

  private Region getRegionById(int regionId, List<Region> regions) {
    return regions.stream()
      .filter(region -> region.getId() == regionId)
      .findFirst()
      .orElseThrow();
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
