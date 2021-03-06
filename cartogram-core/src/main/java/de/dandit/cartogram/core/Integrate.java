package de.dandit.cartogram.core;

import java.util.stream.IntStream;

import de.dandit.cartogram.core.api.ConvergenceGoalFailedException;
import de.dandit.cartogram.core.api.Logging;
import de.dandit.cartogram.core.api.ParallelismConfig;
import de.dandit.cartogram.core.context.CartogramContext;
import de.dandit.cartogram.core.context.MapGrid;
import de.dandit.cartogram.core.dft.FftPlan2D;

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
    final double deltaX = x - x0;
    final double deltaY = y - y0;
    final int x0I = xRounded >= lx ? (lx - 1) : (int) x0;
    final int x1I = xRounded + 0.5 >= lx ? (lx - 1) : (int) x1;
    final int y0I = yRounded >= ly ? (ly - 1) : (int) y0;
    final int y1I = yRounded + 0.5 >= ly ? (ly - 1) : (int) y1;

    final double scale00 = (1. - deltaX) * (1. - deltaY);
    final double scale01 = (1. - deltaX) * deltaY;
    final double scale10 = deltaX * (1. - deltaY);
    final double scale11 = deltaX * deltaY;
    final int x0Offset = x0I * ly;
    final int x1Offset = x1I * ly;
    outX[outIndex] = scale00 * gridX[x0Offset + y0I] + scale01 * gridX[x0Offset + y1I]
      + scale10 * gridX[x1Offset + y0I] + scale11 * gridX[x1Offset + y1I];
    outY[outIndex] = scale00 * gridY[x0Offset + y0I] + scale01 * gridY[x0Offset + y1I]
      + scale10 * gridY[x1Offset + y0I] + scale11 * gridY[x1Offset + y1I];
  }

  void initGridSpeed() {
    MapGrid mapGrid = context.getMapGrid();
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    double[] rhoFt = mapGrid.getRhoFt();

    int initialRhoFt = 4 * lx * ly;
    for (int i = 0; i < lx * ly; i++) {
      rhoFt[i] /= initialRhoFt;
    }

    FftPlan2D gridFluxInitXPlan = mapGrid.getGridFluxInitX();
    FftPlan2D gridFluxInitYPlan = mapGrid.getGridFluxInitY();
    double[] gridFluxInitX = gridFluxInitXPlan.getOutputData();
    double[] gridFluxInitY = gridFluxInitYPlan.getOutputData();
    for (int i = 0; i < lx - 1; i++) {
      for (int j = 0; j < ly; j++) {
        gridFluxInitX[i * ly + j] =
            -rhoFt[(i + 1) * ly + j] /
                (Math.PI * (((double) i + 1) / (double) lx + (j / ((double) i + 1))
                    * (j / (double) ly)
                    * ((double) lx / (double) ly)));
      }
    }
    for (int j = 0; j < ly; j++) {
      gridFluxInitX[(lx - 1) * ly + j] = 0.0;
    }
    for (int i = 0; i < lx; i++) {
      for (int j = 0; j < ly - 1; j++) {
        gridFluxInitY[i * ly + j] =
            -rhoFt[i * ly + j + 1] /
                (Math.PI * (((double) i / (j + 1)) * ((double) i / (double) lx) * ((double) ly / (double) lx)
                    + (j + 1) / (double) ly));
      }
    }
    for (int i = 0; i < lx; i++) {
      gridFluxInitY[i * ly + ly - 1] = 0.0;
    }

    gridFluxInitXPlan.execute();
    gridFluxInitYPlan.execute();
  }

  void ffbIntegrate(ParallelismConfig parallelismConfig) throws ConvergenceGoalFailedException {
    MapGrid mapGrid = context.getMapGrid();
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    double[] gridProjectionX = mapGrid.getGridProjectionX();
    double[] gridProjectionY = mapGrid.getGridProjectionY();

    double[] gridSpeedX = mapGrid.getGridSpeedX();
    double[] gridSpeedY = mapGrid.getGridSpeedY();

    double[] eulX = new double[lx * ly];
    double[] eulY = new double[lx * ly];
    double[] midX = new double[lx * ly];
    double[] midY = new double[lx * ly];

    double[] interpolatedGridSpeedX = new double[lx * ly];
    double[] interpolatedGridSpeedY = new double[lx * ly];

    double[] interpolatedHalfGridSpeedX = new double[lx * ly];
    double[] interpolatedHalfGridSpeedY = new double[lx * ly];

    initGridSpeed();
    double t = 0.0;
    int iterationCounter = 0;
    int nonAcceptedDtsCount = 0;
    double deltaT = 1E-2;
    Logging logging = context.getLogging();
    do {
      calculateSpeedOnGrid(t, parallelismConfig);
      interpolateSpeed(parallelismConfig,   interpolatedGridSpeedX,   interpolatedGridSpeedY, lx, ly, gridProjectionX, gridProjectionY, gridSpeedX, gridSpeedY);
      boolean accept = false;
      while (!accept) {
        if (deltaT < SLOW_CONVERGENCE_DELTA_T_THRESHOLD) {
          context.getLogging().error("Convergence too slow, time integration step size is {0}", deltaT);
          throw new ConvergenceGoalFailedException("time integration below threshold: " + deltaT);
        }
        double currentTimeStep = deltaT;
        IntStream.range(0, lx * ly)
            .forEach(k -> {
              eulX[k] = gridProjectionX[k] +   interpolatedGridSpeedX[k] * currentTimeStep;
              eulY[k] = gridProjectionY[k] +   interpolatedGridSpeedY[k] * currentTimeStep;
            });

        calculateSpeedOnGrid(t + 0.5 * deltaT, parallelismConfig);

        accept = true;
        for (int k = 0; k < lx * ly; k++) {
          if (gridProjectionX[k] + 0.5 * deltaT *   interpolatedGridSpeedX[k] < 0.0 ||
            gridProjectionX[k] + 0.5 * deltaT *   interpolatedGridSpeedX[k] > lx ||
            gridProjectionY[k] + 0.5 * deltaT *   interpolatedGridSpeedY[k] < 0.0 ||
            gridProjectionY[k] + 0.5 * deltaT *   interpolatedGridSpeedY[k] > ly) {
            accept = false;
            nonAcceptedDtsCount++;
            deltaT *= DEC_AFTER_NOT_ACC;
            break;
          }
        }
        if (accept) {
          accept = integrateAcceptedTimestep(
            parallelismConfig,
            deltaT,
            interpolatedGridSpeedX,
            interpolatedHalfGridSpeedX,
            interpolatedGridSpeedY,
            interpolatedHalfGridSpeedY,
            eulX,
            eulY,
            midX,
            midY,
            lx,
            ly,
            gridProjectionX,
            gridProjectionY,
            gridSpeedX,
            gridSpeedY,
            mapGrid.getAbsoluteTolerance());
          if (!accept) {
            nonAcceptedDtsCount++;
            deltaT *= DEC_AFTER_NOT_ACC;
          }
        }
      }

      if (iterationCounter % 10 == 0) {
        logging.debug("iter = {0}, t = {1,number,#.############}, deltaT = {2,number,#.#############}", iterationCounter, t, deltaT);
      }
      t += deltaT;
      iterationCounter++;
      System.arraycopy(midX, 0, gridProjectionX, 0, lx * ly);
      System.arraycopy(midY, 0, gridProjectionY, 0, lx * ly);
      deltaT *= INC_AFTER_ACC;

    } while (t < 1.0);
    logging.debug(
      "Finished integration with iter = {0}, t = {1}, deltaT = {2}, non accepted dts= {3}",
      iterationCounter,
      t,
      deltaT,
      nonAcceptedDtsCount);
  }

  private static boolean integrateAcceptedTimestep(
    ParallelismConfig parallelismConfig,
    double deltaT,
    double[] interpolatedGridSpeedX,
    double[] interpolatedHalfGridSpeedX,
    double[] interpolatedGridSpeedY,
    double[] interpolatedHalfGridSpeedY,
    double[] eulX,
    double[] eulY,
    double[] midX,
    double[] midY,
    int lx,
    int ly,
    double[] projX,
    double[] projY,
    double[] gridSpeedX,
    double[] gridSpeedY,
    double absoluteTolerance) {
    return parallelismConfig.apply(IntStream.range(0, lx * ly))
      .allMatch(k -> {
        interpolate(
          lx,
          ly,
          projX[k] + 0.5 * deltaT * interpolatedGridSpeedX[k],
          projY[k] + 0.5 * deltaT * interpolatedGridSpeedY[k],
          gridSpeedX,
          gridSpeedY,
          interpolatedHalfGridSpeedX,
          interpolatedHalfGridSpeedY,
          k);
        double midXK = projX[k] + interpolatedHalfGridSpeedX[k] * deltaT;
        boolean notInBoundX = midXK < 0.0 || midXK > lx;
        if (notInBoundX) {
          return false;
        }

        double midYK = projY[k] + interpolatedHalfGridSpeedY[k] * deltaT;
        boolean notInBoundY = midYK < 0.0 || midYK > ly;
        if (notInBoundY) {
          return false;
        }
        midX[k] = midXK;
        midY[k] = midYK;
        double midEulDiffX = midXK - eulX[k];
        double midEulDiffY = midYK - eulY[k];

        return midEulDiffX * midEulDiffX + midEulDiffY * midEulDiffY <= absoluteTolerance;
      });
  }

  private static void interpolateSpeed(
    ParallelismConfig parallelismConfig,
    double[] interpolatedSpeedX,
    double[] interpolatedSpeedY,
    int lx,
    int ly,
    double[] gridProjectionX,
    double[] gridProjectionY,
    double[] gridSpeedX,
    double[] gridSpeedY) {
    parallelismConfig.apply(IntStream.range(0, lx * ly))
      .forEach(k -> interpolate(lx, ly, gridProjectionX[k], gridProjectionY[k], gridSpeedX, gridSpeedY, interpolatedSpeedX, interpolatedSpeedY, k));
  }

  void calculateSpeedOnGrid(double t, ParallelismConfig parallelismConfig) {
    MapGrid mapGrid = context.getMapGrid();
    int lx = mapGrid.getLx();
    int ly = mapGrid.getLy();
    double[] gridSpeedX = mapGrid.getGridSpeedX();
    double[] gridSpeedY = mapGrid.getGridSpeedY();
    double[] rhoInit = mapGrid.getRhoInit();
    double[] gridFluxInitX = mapGrid.getGridFluxInitX().getOutputData();
    double[] gridFluxInitY = mapGrid.getGridFluxInitY().getOutputData();
    double rhoFt0 = -mapGrid.getRhoFt()[0];
    double remainingT = 1. - t;

    parallelismConfig.apply(IntStream.range(0, lx * ly))
      .forEach(k -> {
        double rho = rhoFt0 + remainingT * (-rhoInit[k] - rhoFt0);
        gridSpeedX[k] = gridFluxInitX[k] / rho;
        gridSpeedY[k] = gridFluxInitY[k] / rho;
      });
  }
}
