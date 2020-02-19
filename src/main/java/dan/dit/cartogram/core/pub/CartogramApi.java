package dan.dit.cartogram.core.pub;

import dan.dit.cartogram.core.Cartogram;
import dan.dit.cartogram.core.ConvergenceGoalFailedException;
import dan.dit.cartogram.core.Density;
import dan.dit.cartogram.core.context.CartogramContext;
import dan.dit.cartogram.core.context.Point;

import java.util.ArrayList;
import java.util.List;

public class CartogramApi {

  public CartogramResult execute(MapFeatureData mapFeatureData, CartogramConfig config) throws ConvergenceGoalFailedException {
    CartogramContext cartogramContext = Density.initializeContext(mapFeatureData, config);
    CartogramContext context = new Cartogram(cartogramContext)
      .calculate();
    // TODO also allow the input+ouput regions to have polygons with holes
    //  those holes need to be considered for area calculation initially, they can be ignored during iteration
    //  and only have to be projected at the end
    int[] regionIds = context.getRegionData().getRegion_id();
    List<ResultRegion> resultRegions = new ArrayList<>();
    int[][] polyinreg = context.getRegionData().getPolyinreg();
    Point[][] cartcorn = context.getRegionData().getCartcorn();
    for (int i = 0; i < regionIds.length; i++) {
      List<Point[]> shells = new ArrayList<>();
      for (int j = 0; j < polyinreg[i].length; j++) {
        Point[] corners = cartcorn[polyinreg[i][j]];
        shells.add(corners);
      }
      resultRegions.add(new ResultRegion(shells, context.getRegionData().getRegion_na()[i]));
    }
    return new CartogramResult(
      resultRegions,
      cartogramContext.getMapGrid().getProj(),
      cartogramContext.getMapGrid().getLx(),
      cartogramContext.getMapGrid().getLy());
  }
}
