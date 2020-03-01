package de.dandit.cartogram.core;

import java.util.Arrays;
import java.util.stream.IntStream;

import de.dandit.cartogram.core.context.CartogramContext;
import de.dandit.cartogram.core.context.MapGrid;
import de.dandit.cartogram.core.dft.FftPlan2D;
import de.dandit.cartogram.core.pub.Logging;
import de.dandit.cartogram.core.pub.ParallelismConfig;

public class Integrate {
  private static final double INC_AFTER_ACC = 1.1;
  private static final double DEC_AFTER_NOT_ACC = 0.75;
  private static final double SLOW_CONVERGENCE_DELTA_T_THRESHOLD = 1E-8;
  private final CartogramContext context;

  public Integrate(CartogramContext context) {
    this.context = context;
  }

  // This is the bottleneck, it is in almost every inner loop! Find ways to improve it.
  static void interpolate(int lx, int ly, double x, double y, double[] gridX, double[] gridY, double[] outX, double[] outY, int outIndex) {
    final double xRounded = (long) (x + 0.5) - 0.5;
    final double yRounded = (long) (y + 0.5) - 0.5;
    final double x0 = 0. >= xRounded ? 0. : xRounded;
    final double x1 = lx <= xRounded + 1. ? lx : xRounded + 1.;
    final double y0 = 0. >= yRounded ? 0. : yRounded;
    final double y1 = ly <= yRounded + 1. ? ly : yRounded + 1.;
    final double delta_x = (x - x0) / (x1 - x0);
    final double delta_y = (y - y0) / (y1 - y0);
    final int x0I = xRounded >= lx ? (lx - 1) : (int) x0;
    final int x1I = xRounded + 0.5 >= lx ? (lx - 1) : (int) x1;
    final int y0I = yRounded >= ly ? (ly - 1) : (int) y0;
    final int y1I = yRounded + 0.5 >= ly ? (ly - 1) : (int) y1;

    final double scale00 = (1 - delta_x) * (1 - delta_y);
    final double scale01 = (1 - delta_x) * delta_y;
    final double scale10 = delta_x * (1 - delta_y);
    final double scale11 = delta_x * delta_y;
    final int x0Offset = x0I * lx;
    final int x1Offset = x1I * lx;
    outX[outIndex] = scale00 * gridX[x0Offset + y0I] + scale01 * gridX[x0Offset + y1I]
      + scale10 * gridX[x1Offset + y0I] + scale11 * gridX[x1Offset + y1I];
    outY[outIndex] = scale00 * gridY[x0Offset + y0I] + scale01 * gridY[x0Offset + y1I]
      + scale10 * gridY[x1Offset + y0I] + scale11 * gridY[x1Offset + y1I];
  }

  void init_gridv() {
    MapGrid mapGrid = context.getMapGrid();
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    double[] rho_ft = mapGrid.getRhoFt();
    double di;
    int i, j;

    int rho_ft_initial = 4 * lx * ly;
    for (i = 0; i < lx * ly; i++) {
      rho_ft[i] /= rho_ft_initial;
    }

    FftPlan2D grid_fluxx_init_plan = mapGrid.getGridFluxInitX();
    FftPlan2D grid_fluxy_init_plan = mapGrid.getGridFluxInitY();
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

    MapGrid mapGrid = context.getMapGrid();
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    double[] projX = mapGrid.getGridProjectionX();
    double[] projY = mapGrid.getGridProjectionY();

    double[] gridvx = mapGrid.getGridSpeedX();
    double[] gridvy = mapGrid.getGridSpeedY();

    double[] eulX = new double[lx * ly];
    Arrays.fill(eulX, Double.NaN);
    double[] eulY = new double[lx * ly];
    Arrays.fill(eulY, Double.NaN);
    double[] midX = new double[lx * ly];
    Arrays.fill(midX, Double.NaN);
    double[] midY = new double[lx * ly];
    Arrays.fill(midY, Double.NaN);

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
      interpolateSpeed(parallelismConfig, vx_intp, vy_intp, lx, ly, projX, projY, gridvx, gridvy);
      accept = false;
      while (!accept) {
        if (delta_t < SLOW_CONVERGENCE_DELTA_T_THRESHOLD) {
          context.getLogging().error("Convergence too slow, time integration step size is {0}", delta_t);
          throw new ConvergenceGoalFailedException("time integration below threshold: " + delta_t);
        }
        double currentTimeStep = delta_t;
        IntStream.range(0, lx * ly)
            .forEach(k -> {
              eulX[k] = projX[k] + vx_intp[k] * currentTimeStep;
              eulY[k] = projY[k] + vy_intp[k] * currentTimeStep;
            });

        calculateSpeedOnGrid(t + 0.5 * delta_t, parallelismConfig);

        accept = true;
        for (int k = 0; k < lx * ly; k++) {
          if (projX[k] + 0.5 * delta_t * vx_intp[k] < 0.0 ||
            projX[k] + 0.5 * delta_t * vx_intp[k] > lx ||
            projY[k] + 0.5 * delta_t * vy_intp[k] < 0.0 ||
            projY[k] + 0.5 * delta_t * vy_intp[k] > ly) {
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
            eulX,
            eulY,
            midX,
            midY,
            lx,
            ly,
            projX,
            projY,
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
      System.arraycopy(midX, 0, projX, 0, lx * ly);
      System.arraycopy(midY, 0, projY, 0, lx * ly);
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
    double[] eulX,
    double[] eulY,
    double[] midX,
    double[] midY,
    int lx,
    int ly,
    double[] projX,
    double[] projY,
    double[] gridvx,
    double[] gridvy,
    double absTol) {
    return parallelismConfig.apply(IntStream.range(0, lx * ly))
      .allMatch(k -> {
        interpolate(
          lx,
          ly,
          projX[k] + 0.5 * delta_t * vx_intp[k],
          projY[k] + 0.5 * delta_t * vy_intp[k],
          gridvx,
          gridvy,
          vx_intp_half,
          vy_intp_half,
          k);
        midX[k] = projX[k] + vx_intp_half[k] * delta_t;
        midY[k] = projY[k] + vy_intp_half[k] * delta_t;

        double midXK = midX[k];
        double midYK = midY[k];
        double midEulDiffX = midXK - eulX[k];
        double midEulDiffY = midYK - eulY[k];
        boolean withinTolerance = !(midEulDiffX * midEulDiffX +
          midEulDiffY * midEulDiffY > absTol);
        boolean inBoundX = !(midXK < 0.0) && !(midXK > lx);
        boolean inBoundY = !(midYK < 0.0) && !(midYK > ly);
        return withinTolerance && inBoundX && inBoundY;
      });
  }

  private static void interpolateSpeed(
    ParallelismConfig parallelismConfig,
    double[] vx_intp,
    double[] vy_intp,
    int lx,
    int ly,
    double[] projX,
    double[] projY,
    double[] gridvx,
    double[] gridvy) {
    parallelismConfig.apply(IntStream.range(0, lx * ly))
      .forEach(k -> interpolate(lx, ly, projX[k], projY[k], gridvx, gridvy, vx_intp, vy_intp, k));
  }

  void calculateSpeedOnGrid(double t, ParallelismConfig parallelismConfig) {
    MapGrid mapGrid = context.getMapGrid();
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    double[] gridvx = mapGrid.getGridSpeedX();
    double[] gridvy = mapGrid.getGridSpeedY();
    double[] rho_ft = mapGrid.getRhoFt();
    double[] rho_init = mapGrid.getRhoInit();
    double[] grid_fluxx_init = mapGrid.getGridFluxInitX().getOutputData();
    double[] grid_fluxy_init = mapGrid.getGridFluxInitY().getOutputData();

    parallelismConfig.apply(IntStream.range(0, lx * ly))
      .forEach(k -> {
        double rho = rho_ft[0] + (1.0 - t) * (rho_init[k] - rho_ft[0]);
        gridvx[k] = -grid_fluxx_init[k] / rho;
        gridvy[k] = -grid_fluxy_init[k] / rho;
      });
  }
}
