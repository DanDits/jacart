package dan.dit.cartogram.core;

import static dan.dit.cartogram.core.Density.MAX_PERMITTED_AREA_ERROR;

import java.util.Arrays;
import java.util.Objects;

import dan.dit.cartogram.core.context.CartogramContext;
import dan.dit.cartogram.core.context.MapGrid;
import dan.dit.cartogram.core.context.Point;
import dan.dit.cartogram.core.context.RegionData;

public class Cartogram {
  private final Integrate integrate;
  private final Density density;
  private final CartogramContext context;

  public Cartogram(CartogramContext context) {
    this.context = Objects.requireNonNull(context);
    this.integrate = new Integrate(context);
    this.density = new Density(context);
  }

  public CartogramContext calculate() throws ConvergenceGoalFailedException {
    boolean onlyOneRegionExists = context.isSingleRegion();
    if (onlyOneRegionExists) {
      context.getLogging().debug("Hint: Only one region exists, output will only be an affine transformation.");
    }
    MapGrid mapGrid = context.getMapGrid();
    RegionData regionData = context.getRegionData();
    double[] area_err = regionData.getAreaError();
    double[] cart_area = regionData.getCartogramArea();
    double[] init_tot_area = new double[1];
    // also initializes init_tot_area...
    if (max_area_err(area_err, cart_area, regionData.getPolycorn(), init_tot_area) <= MAX_PERMITTED_AREA_ERROR) {
      context.getLogging().debug("Nothing to do, area already correct.");
      Point[][] cartcorn = context.getRegionData().getCartcorn();
      Point[][] polycorn = context.getRegionData().getPolycorn();
      for (int i = 0; i < polycorn.length; i++) {
        cartcorn[i] = Arrays.copyOf(polycorn[i], polycorn[i].length);
      }
      return context;
    }
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    Point[] proj = mapGrid.getProj();

    context.getLogging().debug("Starting integration 1\n");
    integrate.ffb_integrate();
    project(false);

    Point[][] cartcorn = regionData.getCartcorn();
    double[] cartogramTotalArea = new double[1];
    double mae = max_area_err(area_err, cart_area, cartcorn, cartogramTotalArea);
    context.getLogging().debug("max. abs. area error: {0}", mae);

    Point[] proj2 = mapGrid.getProj2();
    int integration = 0;
    double lastMae = Double.POSITIVE_INFINITY;
    while (mae > MAX_PERMITTED_AREA_ERROR && mae < lastMae) {
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
      integrate.ffb_integrate();
      project(true);

      for (int i = 0; i < lx; i++) {
        for (int j = 0; j < ly; j++) {
          proj[i * ly + j].x = proj2[i * ly + j].x;
          proj[i * ly + j].y = proj2[i * ly + j].y;
        }
      }
      lastMae = mae;
      mae = max_area_err(area_err, cart_area, cartcorn, cartogramTotalArea);
      context.getLogging().debug("max. abs. area error: {0}", mae);
      if (lastMae < mae) {
        context.getLogging().error("Did not converge, aborted! Error is: {0}", mae);
        throw new ConvergenceGoalFailedException("Error increased from " + lastMae + " to " + mae);
      }
    }
    scalePolygonsToMatchInitialTotalArea(Math.sqrt(init_tot_area[0] / cartogramTotalArea[0]), lx, ly, cartcorn);

    double final_max_area_error = max_area_err(area_err, cart_area, cartcorn, cartogramTotalArea);
    context.getLogging().debug("Final error: {0}", final_max_area_error);
    return this.context;
  }

  private void scalePolygonsToMatchInitialTotalArea(double correction_factor, int lx, int ly, Point[][] cartcorn) {
    context.getLogging().debug("correction_factor = {0}", correction_factor);
    for (Point[] cartI : cartcorn) {
      for (Point point : cartI) {
        point.x = correction_factor * (point.x - 0.5 * lx) + 0.5 * lx;
        point.y = correction_factor * (point.y - 0.5 * ly) + 0.5 * ly;
      }
    }
  }

  public static void project(int gridWidth, int gridHeight, Point[] gridProjection, Point[][] data) {
    double[] xdisp = new double[gridWidth * gridHeight];
    double[] ydisp = new double[gridWidth * gridHeight];
    for (int i = 0; i < gridWidth; i++) {
      for (int j = 0; j < gridHeight; j++) {
        int index = i * gridHeight + j;
        xdisp[index] = gridProjection[index].x - i - 0.5;
        ydisp[index] = gridProjection[index].y - j - 0.5;
      }
    }
    for (Point[] linearRing : data) {
      for (int j = 0; j < linearRing.length; j++) {
        Point pointIJ = linearRing[j].createCopy();
        Point p = linearRing[j];
        Integrate.interpolate(gridWidth, gridHeight, pointIJ.x, pointIJ.y, xdisp, ydisp, p);
        p.x += pointIJ.x;
        p.y += pointIJ.y;
      }
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
    Point[] proj = mapGrid.getProj();

    xdisp = new double[lx * ly];
    ydisp = new double[lx * ly];
    for (i = 0; i < lx; i++) {
      for (j = 0; j < ly; j++) {
        xdisp[i * ly + j] = proj[i * ly + j].x - i - 0.5;
        ydisp[i * ly + j] = proj[i * ly + j].y - j - 0.5;
      }
    }

    Point[][] polycorn = regionData.getPolycorn();
    int n_poly = polycorn.length;
    Point[] proj2 = mapGrid.getProj2();
    Point[][] cartcorn = regionData.getCartcorn();

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

  double max_area_err(double[] area_err, double[] cart_area, Point[][] corn,
                      double[] sum_cart_area) {
    double max, obj_area, sum_target_area;
    int i, j;

    RegionData regionData = context.getRegionData();
    int[][] polyinreg = regionData.getPolyinreg();
    int n_reg = polyinreg.length;
    double[] target_area = regionData.getTarget_area();
    for (i = 0; i < n_reg; i++) {
      // if all polygons in a region were tiny they will be removed and thus it will be impossible for
      // the cartogram area to reach the target area (e.g.: Washington D.C.)
      // or we could also remove the region and ignore it completely
      int[] polyI = polyinreg[i];
      if (polyI.length > 0) {
        cart_area[i] = 0.0;
        for (j = 0; j < polyI.length; j++) {
          cart_area[i] += Polygon.polygon_area(corn[polyI[j]]);
        }
      } else {
        cart_area[i] = -1.0;
      }
    }
    for (i = 0, sum_target_area = 0.0; i < n_reg; i++) {
      sum_target_area += target_area[i];
    }
    for (i = 0, sum_cart_area[0] = 0.0; i < n_reg; i++) {
      if (cart_area[i] >= 0) {
        sum_cart_area[0] += cart_area[i];
      }
    }
    for (i = 0; i < n_reg; i++) {
      if (cart_area[i] >= 0) {
        obj_area = target_area[i] * (sum_cart_area[0]) / sum_target_area;
        area_err[i] = cart_area[i] / obj_area - 1.0;
      } else {
        area_err[i] = 0; // ignore the region
      }
    }
    max = 0.0;
    for (i = 0; i < n_reg; i++) {
      max = Math.max(max, Math.abs(area_err[i]));
    }
    return max;
  }

  Point affine_transf(int triid, Point[] tri, double x, double y) {
    double ainv11, ainv12, ainv13, ainv21, ainv22, ainv23, ainv31, ainv32,
      ainv33, t11, t12, t13, t21, t22, t23, det;
    Point p = null;
    Point pre = null;
    Point q = null;
    Point r = null;

    int ly = context.getMapGrid().getLy();
    switch (triid % 4) {
      case 0:
        p.x = triid / (4 * ly);
        p.y = (triid / 4) % ly;
        q.x = p.x + 0.5;
        q.y = p.y + 0.5;
        r.x = p.x + 1;
        r.y = p.y;
        break;
      case 1:
        p.x = triid / (4 * ly);
        p.y = (triid / 4) % ly;
        q.x = p.x;
        q.y = p.y + 1;
        r.x = p.x + 0.5;
        r.y = p.y + 0.5;
        break;
      case 2:
        p.x = triid / (4 * ly) + 0.5;
        p.y = (triid / 4) % ly + 0.5;
        q.x = p.x + 0.5;
        q.y = p.y + 0.5;
        r.x = q.x;
        r.y = q.y - 1;
        break;
      default:
        p.x = triid / (4 * ly);
        p.y = (triid / 4) % ly + 1;
        q.x = p.x + 1;
        q.y = p.y;
        r.x = p.x + 0.5;
        r.y = p.y - 0.5;
    }

    det = tri[0].x * tri[1].y + tri[1].x * tri[2].y + tri[2].x * tri[0].y
      - tri[1].x * tri[0].y - tri[2].x * tri[1].y - tri[0].x * tri[2].y;

    ainv11 = tri[1].y - tri[2].y;
    ainv12 = tri[2].x - tri[1].x;
    ainv13 = tri[1].x * tri[2].y - tri[1].y * tri[2].x;
    ainv21 = tri[2].y - tri[0].y;
    ainv22 = tri[0].x - tri[2].x;
    ainv23 = tri[0].y * tri[2].x - tri[0].x * tri[2].y;
    ainv31 = tri[0].y - tri[1].y;
    ainv32 = tri[1].x - tri[0].x;
    ainv33 = tri[0].x * tri[1].y - tri[0].y * tri[1].x;

    t11 = p.x * ainv11 + q.x * ainv21 + r.x * ainv31;
    t12 = p.x * ainv12 + q.x * ainv22 + r.x * ainv32;
    t13 = p.x * ainv13 + q.x * ainv23 + r.x * ainv33;
    t21 = p.y * ainv11 + q.y * ainv21 + r.y * ainv31;
    t22 = p.y * ainv12 + q.y * ainv22 + r.y * ainv32;
    t23 = p.y * ainv13 + q.y * ainv23 + r.y * ainv33;

    pre.x = (t11 * x + t12 * y + t13) / det;
    pre.y = (t21 * x + t22 * y + t23) / det;

    return pre;
  }

  double min4(double a, double b, double c, double d) {
    if (a <= b && a <= c && a <= d)
      return a;
    if (b <= a && b <= c && b <= d)
      return b;
    if (c <= a && c <= b && c <= d)
      return c;
    return d;
  }

  double max4(double a, double b, double c, double d) {
    if (a >= b && a >= c && a >= d)
      return a;
    if (b >= a && b >= c && b >= d)
      return b;
    if (c >= a && c >= b && c >= d)
      return c;
    return d;
  }

  public void inv_project() {
    double[] xdisp, ydisp;
    int i, j, k;
    int[][] xyhalfshift2tri;
    Point[] invproj, invproj2;
    Point[][] projgrid, tri;

    MapGrid mapGrid = context.getMapGrid();
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    Point[] proj = mapGrid.getProj();

    xdisp = new double[lx * ly];
    ydisp = new double[lx * ly];
    invproj = new Point[lx * ly];
    invproj2 = new Point[lx * ly];
    projgrid = new Point[lx + 1][ly + 1];
    tri = new Point[4 * lx * ly][3];
    xyhalfshift2tri = new int[lx][ly];

    for (i = 0; i < lx; i++) {
      for (j = 0; j < ly; j++) {
        xdisp[i * ly + j] = proj[i * ly + j].x - i - 0.5;
        ydisp[i * ly + j] = proj[i * ly + j].y - j - 0.5;
      }
    }

    for (i = 0; i <= lx; i++) {
      for (j = 0; j <= ly; j++) {
        Integrate.interpolate(lx, ly, i, j, xdisp, ydisp, projgrid[i][j]);
        projgrid[i][j].x += i;
        projgrid[i][j].y += j;
      }
    }

    for (i = 0; i < lx; i++)
      for (j = 0; j < ly; j++) {
        tri[4 * (i * ly + j)][0].x =
          tri[4 * (i * ly + j) + 1][0].x = projgrid[i][j].x;
        tri[4 * (i * ly + j)][0].y =
          tri[4 * (i * ly + j) + 1][0].y = projgrid[i][j].y;
        tri[4 * (i * ly + j) + 1][1].x =
          tri[4 * (i * ly + j) + 3][0].x = projgrid[i][j + 1].x;
        tri[4 * (i * ly + j) + 1][1].y =
          tri[4 * (i * ly + j) + 3][0].y = projgrid[i][j + 1].y;
        tri[4 * (i * ly + j)][2].x =
          tri[4 * (i * ly + j) + 2][2].x = projgrid[i + 1][j].x;
        tri[4 * (i * ly + j)][2].y =
          tri[4 * (i * ly + j) + 2][2].y = projgrid[i + 1][j].y;
        tri[4 * (i * ly + j) + 2][1].x =
          tri[4 * (i * ly + j) + 3][1].x = projgrid[i + 1][j + 1].x;
        tri[4 * (i * ly + j) + 2][1].y =
          tri[4 * (i * ly + j) + 3][1].y = projgrid[i + 1][j + 1].y;
        tri[4 * (i * ly + j)][1].x =
          tri[4 * (i * ly + j) + 1][2].x =
            tri[4 * (i * ly + j) + 2][0].x =
              tri[4 * (i * ly + j) + 3][2].x = proj[i * ly + j].x;
        tri[4 * (i * ly + j)][1].y =
          tri[4 * (i * ly + j) + 1][2].y =
            tri[4 * (i * ly + j) + 2][0].y =
              tri[4 * (i * ly + j) + 3][2].y = proj[i * ly + j].y;
      }

    for (i = 0; i < lx; i++) {
      for (j = 0; j < ly; j++) {
        xyhalfshift2tri[i][j] = -1;
      }
    }
    for (i = 0; i < 4 * lx * ly; i++) {
      Density.set_inside_values_for_polygon(i, tri[i], xyhalfshift2tri);
    }

    for (i = 0; i < lx; i++) {
      for (j = 0; j < ly; j++) {
        k = xyhalfshift2tri[i][j];
        invproj[i * ly + j] = affine_transf(k, tri[k], i + 0.5, j + 0.5);
      }
    }

    for (j = 0; j < ly - 1; j++) {
      invproj2[j].x = invproj[j].x;
      invproj2[j].y = invproj[j].y;
    }
    for (i = 0; i < lx - 1; i++) {
      invproj2[i * ly + ly - 1].x = invproj[i * ly + ly - 1].x;
      invproj2[i * ly + ly - 1].y = invproj[i * ly + ly - 1].y;
    }
    for (j = 1; j < ly; j++) {
      invproj2[(lx - 1) * ly + j].x = invproj[(lx - 1) * ly + j].x;
      invproj2[(lx - 1) * ly + j].y = invproj[(lx - 1) * ly + j].y;
    }
    for (i = 1; i < lx; i++) {
      invproj2[i * ly].x = invproj[i * ly].x;
      invproj2[i * ly].y = invproj[i * ly].y;
    }
    for (i = 1; i < lx - 1; i++) {
      for (j = 1; j < ly - 1; j++) {
        if (invproj[i * ly + j].x < min4(invproj[i * ly + j - 1].x,
          invproj[i * ly + j + 1].x,
          invproj[(i - 1) * ly + j].x,
          invproj[(i + 1) * ly + j].x) - 1 ||
          invproj[i * ly + j].x > max4(invproj[i * ly + j - 1].x,
            invproj[i * ly + j + 1].x,
            invproj[(i - 1) * ly + j].x,
            invproj[(i + 1) * ly + j].x) + 1 ||
          invproj[i * ly + j].y < min4(invproj[i * ly + j - 1].y,
            invproj[i * ly + j + 1].y,
            invproj[(i - 1) * ly + j].y,
            invproj[(i + 1) * ly + j].y) - 1 ||
          invproj[i * ly + j].y > max4(invproj[i * ly + j - 1].y,
            invproj[i * ly + j + 1].y,
            invproj[(i - 1) * ly + j].y,
            invproj[(i + 1) * ly + j].y) + 1) {
          invproj2[i * ly + j].x =
            0.25 * (invproj[i * ly + j - 1].x + invproj[i * ly + j + 1].x +
              invproj[(i - 1) * ly + j].x + invproj[(i + 1) * ly + j].x);
          invproj2[i * ly + j].y =
            0.25 * (invproj[i * ly + j - 1].y + invproj[i * ly + j + 1].y +
              invproj[(i - 1) * ly + j].y + invproj[(i + 1) * ly + j].y);
        } else {
          invproj2[i * ly + j].x = invproj[i * ly + j].x;
          invproj2[i * ly + j].y = invproj[i * ly + j].y;
        }
      }
    }
  }


}
