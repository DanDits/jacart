package de.dandit.cartogram.core;

import de.dandit.cartogram.core.context.CartogramContext;
import de.dandit.cartogram.core.context.MapGrid;
import de.dandit.cartogram.core.context.Point;
import de.dandit.cartogram.core.pub.Logging;
import de.dandit.cartogram.core.pub.ParallelismConfig;
import de.dandit.cartogram.dft.FftPlan2D;

import java.util.stream.IntStream;

public class Integrate {
  private static final double INC_AFTER_ACC = 1.1;
  private static final double DEC_AFTER_NOT_ACC = 0.75;
  private static final double SLOW_CONVERGENCE_DELTA_T_THRESHOLD = 1E-8;
  private final CartogramContext context;

  public Integrate(CartogramContext context) {
    this.context = context;
  }

  static void interpolate(int lx, int ly, double x, double y, double[] gridX, double[] gridY, double[] outX, double[] outY, int outIndex) {
    double xRounded = (long) (x + 0.5);
    double yRounded = (long) (y + 0.5);
    final double x0 = Math.max(0.0, xRounded - 0.5);
    final double x1 = Math.min(lx, xRounded + 0.5);
    final double y0 = Math.max(0.0, yRounded - 0.5);
    final double y1 = Math.min(ly, yRounded + 0.5);
    final double delta_x = (x - x0) / (x1 - x0);
    final double delta_y = (y - y0) / (y1 - y0);
    final int x0I = xRounded - 0.5 >= lx ? (lx - 1) : (int) x0;
    final int x1I = xRounded >= lx ? (lx - 1) : (int) x1;
    final int y0I = yRounded - 0.5 >= ly ? (ly - 1) : (int) y0;
    final int y1I = yRounded >= ly ? (ly - 1) : (int) y1;

    outX[outIndex] = (1 - delta_x) * (1 - delta_y) * gridX[x0I * lx + y0I] + (1 - delta_x) * delta_y * gridX[x0I * lx + y1I]
      + delta_x * (1 - delta_y) * gridX[x1I * lx + y0I] + delta_x * delta_y * gridX[x1I * lx + y1I];
    outY[outIndex] = (1 - delta_x) * (1 - delta_y) * gridY[x0I * lx + y0I] + (1 - delta_x) * delta_y * gridY[x0I * lx + y1I]
      + delta_x * (1 - delta_y) * gridY[x1I * lx + y0I] + delta_x * delta_y * gridY[x1I * lx + y1I];
  }

  static void interpolate(int lx, int ly, double x, double y, double[] gridX, double[] gridY, Point result) {
    double xRounded = (long) (x + 0.5);
    double yRounded = (long) (y + 0.5);
    final double x0 = Math.max(0.0, xRounded - 0.5);
    final double x1 = Math.min(lx, xRounded + 0.5);
    final double y0 = Math.max(0.0, yRounded - 0.5);
    final double y1 = Math.min(ly, yRounded + 0.5);
    final double delta_x = (x - x0) / (x1 - x0);
    final double delta_y = (y - y0) / (y1 - y0);
    final int x0I = xRounded - 0.5 >= lx ? (lx - 1) : (int) x0;
    final int x1I = xRounded >= lx ? (lx - 1) : (int) x1;
    final int y0I = yRounded - 0.5 >= ly ? (ly - 1) : (int) y0;
    final int y1I = yRounded >= ly ? (ly - 1) : (int) y1;

    result.x = (1 - delta_x) * (1 - delta_y) * gridX[x0I * lx + y0I] + (1 - delta_x) * delta_y * gridX[x0I * lx + y1I]
      + delta_x * (1 - delta_y) * gridX[x1I * lx + y0I] + delta_x * delta_y * gridX[x1I * lx + y1I];
    result.y = (1 - delta_x) * (1 - delta_y) * gridY[x0I * lx + y0I] + (1 - delta_x) * delta_y * gridY[x0I * lx + y1I]
      + delta_x * (1 - delta_y) * gridY[x1I * lx + y0I] + delta_x * delta_y * gridY[x1I * lx + y1I];
  }



  void init_gridv() {
    MapGrid mapGrid = context.getMapGrid();
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    double[] rho_ft = mapGrid.getRho_ft();
    double di;
    int i, j;

    int rho_ft_initial = 4 * lx * ly;
    for (i = 0; i < lx * ly; i++) {
      rho_ft[i] /= rho_ft_initial;
    }

    FftPlan2D grid_fluxx_init_plan = mapGrid.getGrid_fluxx_init();
    FftPlan2D grid_fluxy_init_plan = mapGrid.getGrid_fluxy_init();
    double[] grid_fluxx_init = grid_fluxx_init_plan.getOutputData();
    double[] grid_fluxy_init = grid_fluxy_init_plan.getOutputData();
    for (i = 0; i < lx - 1; i++) {
      di = i;
      for (j = 0; j < ly; j++)
        grid_fluxx_init[i * ly + j] =
          -rho_ft[(i + 1) * ly + j] /
            (Math.PI * ((di + 1) / (double) lx + (j / (di + 1))
              * (j / (double) ly)
              * ((double) lx / (double) ly)));
    }
    for (j = 0; j < ly; j++)
      grid_fluxx_init[(lx - 1) * ly + j] = 0.0;
    for (i = 0; i < lx; i++) {
      di = i;
      for (j = 0; j < ly - 1; j++)
        grid_fluxy_init[i * ly + j] =
          -rho_ft[i * ly + j + 1] /
            (Math.PI * ((di / (j + 1)) * (di / (double) lx) * ((double) ly / (double) lx)
              + (j + 1) / (double) ly));
    }
    for (i = 0; i < lx; i++)
      grid_fluxy_init[i * ly + ly - 1] = 0.0;


    grid_fluxx_init_plan.execute();
    grid_fluxy_init_plan.execute();
  }

  void ffb_integrate(ParallelismConfig parallelismConfig) throws ConvergenceGoalFailedException {
    boolean accept;
    double delta_t, t;
    double[] vx_intp, vx_intp_half, vy_intp, vy_intp_half;
    int iter;
    Point[] eul, mid;

    MapGrid mapGrid = context.getMapGrid();
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    Point[] proj = mapGrid.getProj();

    double[] gridvx = mapGrid.getGridvx();
    double[] gridvy = mapGrid.getGridvy();

    eul = new Point[lx * ly];
    for (int i = 0; i < eul.length; i++) {
      eul[i] = new Point(Double.NaN, Double.NaN);
    }

    mid = new Point[lx * ly];
    for (int i = 0; i < mid.length; i++) {
      mid[i] = new Point(Double.NaN, Double.NaN);
    }

    vx_intp = new double[lx * ly];
    vy_intp = new double[lx * ly];

    vx_intp_half = new double[lx * ly];
    vy_intp_half = new double[lx * ly];

    init_gridv();
    t = 0.0;
    iter = 0;
    int non_accepted_dts_count = 0;
    delta_t = 1E-2;
    Logging logging = context.getLogging();
    do {
      calculateSpeedOnGrid(t, parallelismConfig);
      interpolateSpeed(parallelismConfig, vx_intp, vy_intp, lx, ly, proj, gridvx, gridvy);
      accept = false;
      while (!accept) {
        if (delta_t < SLOW_CONVERGENCE_DELTA_T_THRESHOLD) {
          context.getLogging().error("Convergence too slow, time integration step size is {0}", delta_t);
          throw new ConvergenceGoalFailedException("time integration below threshold: " + delta_t);
        }
        double currentTimeStep = delta_t;
        IntStream.range(0, lx * ly)
            .forEach(k -> {
              eul[k].x = proj[k].x + vx_intp[k] * currentTimeStep;
              eul[k].y = proj[k].y + vy_intp[k] * currentTimeStep;
            });

        calculateSpeedOnGrid(t + 0.5 * delta_t, parallelismConfig);

        accept = true;
        for (int k = 0; k < lx * ly; k++) {
          if (proj[k].x + 0.5 * delta_t * vx_intp[k] < 0.0 ||
            proj[k].x + 0.5 * delta_t * vx_intp[k] > lx ||
            proj[k].y + 0.5 * delta_t * vy_intp[k] < 0.0 ||
            proj[k].y + 0.5 * delta_t * vy_intp[k] > ly) {
            accept = false;
            non_accepted_dts_count++;
            delta_t *= DEC_AFTER_NOT_ACC;
            break;
          }
        }
        if (accept) {
          accept = integrateAcceptedTimestep(
            parallelismConfig,
            delta_t,
            vx_intp,
            vx_intp_half,
            vy_intp,
            vy_intp_half,
            eul,
            mid,
            lx,
            ly,
            proj,
            gridvx,
            gridvy,
            mapGrid.getAbsoluteTolerance());
          if (!accept) {
            non_accepted_dts_count++;
            delta_t *= DEC_AFTER_NOT_ACC;
          }
        }
      }

      if (iter % 10 == 0) {
        logging.debug("iter = {0}, t = {1,number,#.############}, delta_t = {2,number,#.#############}", iter, t, delta_t);
      }
      t += delta_t;
      iter++;
      for (int k = 0; k < lx * ly; k++) {
        proj[k].x = mid[k].x;
        proj[k].y = mid[k].y;
      }
      delta_t *= INC_AFTER_ACC;

    } while (t < 1.0);
    logging.debug(
      "Finished integration with iter = {0}, t = {1}, delta_t = {2}, non accepted dts= {3}",
      iter,
      t,
      delta_t,
      non_accepted_dts_count);
  }

  private static boolean integrateAcceptedTimestep(
    ParallelismConfig parallelismConfig,
    double delta_t,
    double[] vx_intp,
    double[] vx_intp_half,
    double[] vy_intp,
    double[] vy_intp_half,
    Point[] eul,
    Point[] mid,
    int lx,
    int ly,
    Point[] proj,
    double[] gridvx,
    double[] gridvy,
    double absTol) {
    return parallelismConfig.apply(IntStream.range(0, lx * ly))
      .allMatch(k -> {
        interpolate(
          lx,
          ly,
          proj[k].x + 0.5 * delta_t * vx_intp[k],
          proj[k].y + 0.5 * delta_t * vy_intp[k],
          gridvx,
          gridvy,
          vx_intp_half,
          vy_intp_half,
          k);
        mid[k].x = proj[k].x + vx_intp_half[k] * delta_t;
        mid[k].y = proj[k].y + vy_intp_half[k] * delta_t;

        double midX = mid[k].x;
        double midY = mid[k].y;
        double midEulDiffX = midX - eul[k].x;
        double midEulDiffY = midY - eul[k].y;
        boolean withinTolerance = !(midEulDiffX * midEulDiffX +
          midEulDiffY * midEulDiffY > absTol);
        boolean inBoundX = !(midX < 0.0) && !(midX > lx);
        boolean inBoundY = !(midY < 0.0) && !(midY > ly);
        return withinTolerance && inBoundX && inBoundY;
      });
  }

  private static void interpolateSpeed(
    ParallelismConfig parallelismConfig,
    double[] vx_intp,
    double[] vy_intp,
    int lx,
    int ly,
    Point[] proj,
    double[] gridvx,
    double[] gridvy) {
    parallelismConfig.apply(IntStream.range(0, lx * ly))
      .forEach(k -> interpolate(lx, ly, proj[k].x, proj[k].y, gridvx, gridvy, vx_intp, vy_intp, k));
  }

  void calculateSpeedOnGrid(double t, ParallelismConfig parallelismConfig) {
    MapGrid mapGrid = context.getMapGrid();
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    double[] gridvx = mapGrid.getGridvx();
    double[] gridvy = mapGrid.getGridvy();
    double[] rho_ft = mapGrid.getRho_ft();
    double[] rho_init = mapGrid.getRho_init();
    double[] grid_fluxx_init = mapGrid.getGrid_fluxx_init().getOutputData();
    double[] grid_fluxy_init = mapGrid.getGrid_fluxy_init().getOutputData();

    parallelismConfig.apply(IntStream.range(0, lx * ly))
      .forEach(k -> {
        double rho = rho_ft[0] + (1.0 - t) * (rho_init[k] - rho_ft[0]);
        gridvx[k] = -grid_fluxx_init[k] / rho;
        gridvy[k] = -grid_fluxy_init[k] / rho;
      });
  }
}
