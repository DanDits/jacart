package dan.dit.cartogram;

import dan.dit.cartogram.dft.FftPlan2D;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Integrate {
  private static final double INC_AFTER_ACC = 1.1;
  private static final double DEC_AFTER_NOT_ACC = 0.75;
  private final CartogramContext context;

  public Integrate(CartogramContext context) {
    this.context = context;
  }

  @SuppressWarnings("squid:S1244")
  // it is correct to compare with equality as we casted one to long before
  public static double veryFastFloor(double value) {
    long roundedValue = (long) value;
    return roundedValue - ((value < 0. && value != roundedValue) ? 1. : 0.);
  }

  static double interpolateX(int lx, int ly, double x, double y, double[] grid) {
    double xUp = veryFastFloor(x + 0.5);
    double yUp = veryFastFloor(y + 0.5);
    final double x0 = Math.max(0.0, xUp - 0.5);
    final double x1 = Math.min(lx, xUp + 0.5);
    final double y0 = Math.max(0.0, yUp - 0.5);
    final double y1 = Math.min(ly, yUp + 0.5);
    final double delta_x = (x - x0) / (x1 - x0);
    final double delta_y = (y - y0) / (y1 - y0);
    final double fx0y0 = getFx0y0_X(x, y, grid, ly, (int) x0, (int) y0);
    final double fx0y1 = getFx0y1_X(x, y, grid, ly, (int) x0, (int) y1);
    final double fx1y0 = getFx1y0_X(x, y, grid, lx, ly, (int) x1, (int) y0);
    final double fx1y1 = getFx1y1_X(x, y, grid, lx, ly, (int) x1, (int) y1);

    return (1 - delta_x) * (1 - delta_y) * fx0y0 + (1 - delta_x) * delta_y * fx0y1
        + delta_x * (1 - delta_y) * fx1y0 + delta_x * delta_y * fx1y1;
  }

  static double interpolateY(int lx, int ly, double x, double y, double[] grid) {
    double xUp = veryFastFloor(x + 0.5);
    double yUp = veryFastFloor(y + 0.5);
    final double x0 = Math.max(0.0, xUp - 0.5);
    final double x1 = Math.min(lx, xUp + 0.5);
    final double y0 = Math.max(0.0, yUp - 0.5);
    final double y1 = Math.min(ly, yUp + 0.5);
    final double delta_x = (x - x0) / (x1 - x0);
    final double delta_y = (y - y0) / (y1 - y0);
    final double fx0y0 = getFx0y0_Y(x, y, grid, ly, (int) x0, (int) y0);
    final double fx0y1 = getFx0y1_Y(x, y, grid, ly, (int) x0, (int) y1);
    final double fx1y0 = getFx1y0_Y(x, y, grid, lx, ly, (int) x1, (int) y0);
    final double fx1y1 = getFx1y1_Y(x, y, grid, lx, ly, (int) x1, (int) y1);

    return (1 - delta_x) * (1 - delta_y) * fx0y0 + (1 - delta_x) * delta_y * fx0y1
        + delta_x * (1 - delta_y) * fx1y0 + delta_x * delta_y * fx1y1;
  }

  private static double getFx1y1_X(
      double x,
      double y,
      double[] grid,
      int lx,
      int ly,
      int x1,
      int y1) {
    double lxM = lx - 0.5;
    double lyM = ly - 0.5;
    if (x >= lxM) {
      return 0.0;
    } else if (y >= lyM) {
      return grid[x1 * ly + ly - 1];
    } else {
      return grid[x1 * ly + y1];
    }
  }

  private static double getFx1y0_X(
      double x,
      double y,
      double[] grid,
      int lx,
      int ly,
      int x1,
      int y0) {
    if (x >= lx - 0.5) {
      return 0.0;
    } else {
      return grid[x1 * ly + y0];
    }
  }

  private static double getFx0y0_X(double x, double y, double[] grid, int ly, int x0, int y0) {
    if (x < 0.5) {
      return 0.0;
    } else {
      return grid[x0 * ly + y0];
    }
  }

  private static double getFx0y1_X(double x, double y, double[] grid, int ly, int x0, int y1) {
    if (x < 0.5) {
      return 0.0;
    } else if (y >= ly - 0.5) {
      return grid[x0 * ly + ly - 1];
    } else {
      return grid[x0 * ly + y1];
    }
  }

  private static double getFx1y1_Y(
      double x,
      double y,
      double[] grid,
      int lx,
      int ly,
      int x1,
      int y1) {
    double lxM = lx - 0.5;
    double lyM = ly - 0.5;
    if (y >= lyM) {
      return 0.0;
    } else if (x >= lxM) {
      return grid[(lx - 1) * ly + y1];
    } else {
      return grid[x1 * ly + y1];
    }
  }

  private static double getFx1y0_Y(
      double x,
      double y,
      double[] grid,
      int lx,
      int ly,
      int x1,
      int y0) {
    if (y < 0.5) {
      return 0.0;
    } else if (x >= lx - 0.5) {
      return grid[(lx - 1) * ly + y0];
    } else {
      return grid[x1 * ly + y0];
    }
  }

  private static double getFx0y0_Y(double x, double y, double[] grid, int ly, int x0, int y0) {
    if (y < 0.5) {
      return 0.0;
    } else {
      return grid[x0 * ly + y0];
    }
  }

  private static double getFx0y1_Y(double x, double y, double[] grid, int ly, int x0, int y1) {
    if (y >= ly - 0.5) {
      return 0.0;
    } else {
      return grid[x0 * ly + y1];
    }
  }

  private static void printError(String text, Object... parameters) {
    System.err.println(MessageFormat.format(text, parameters));
  }

  void init_gridv() {
    int lx = context.getLx();
    int ly = context.getLy();
    double[] rho_ft = context.getRho_ft();
    double di;
    int i, j;

    displayDoubleArray("rho_ft", rho_ft);
    int rho_ft_initial = 4 * lx * ly;
    for (i = 0; i < lx * ly; i++) {
      rho_ft[i] /= rho_ft_initial;
    }

    FftPlan2D grid_fluxx_init_plan = context.getGrid_fluxx_init();
    FftPlan2D grid_fluxy_init_plan = context.getGrid_fluxy_init();
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


    displayDoubleArray("rho_ft", rho_ft);
    displayDoubleArray("grid_fluxx_init before plan execution", grid_fluxy_init);
    grid_fluxx_init_plan.execute();
    grid_fluxy_init_plan.execute();
  }

  public static void displayIntArray(String text, int[] data) {
    System.out.print(text + " (length=" + data.length + ", sum=" + Arrays.stream(data).sum() + ") First entries= ");
    System.out.println(Arrays.stream(data)
            .limit(10L)
            .mapToObj(Integer::toString)
            .collect(Collectors.joining(", ")));
  }

  public static void displayDoubleArray(String text, double[] data) {
    System.out.print(text + " (length=" + data.length + ", sum=" + Arrays.stream(data).sum() + ") First entries= ");
    System.out.println(Arrays.stream(data)
            .limit(10L)
            .mapToObj(Double::toString)
            .collect(Collectors.joining(", ")));
  }

  void ffb_integrate() {
    boolean accept;
    double delta_t, t;
    double[] vx_intp, vx_intp_half, vy_intp, vy_intp_half;
    int iter, k;
    Point[] eul, mid;

    int lx = context.getLx();
    int ly = context.getLy();
    Point[] proj = context.getProj();

    context.initGrid(); // TODO is this really inniting gridvx here? because it is also initialized in DiffIntegrate
    double[] gridvx = context.getGridvx().getOutputData();
    double[] gridvy = context.getGridvy().getOutputData();

    context.initFluxInitPlan();

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
    delta_t = 1E-2;
    iter = 0;
    int non_accepted_dts_count = 0;
    do {
      ffb_calcv(t);
      interpolateSpeed(vx_intp, vy_intp, lx, ly, proj, gridvx, gridvy);
      accept = false;
      while (!accept) {
        // TODO candidate for parallelization
        for (k = 0; k < lx * ly; k++) {
          eul[k].x = proj[k].x + vx_intp[k] * delta_t;
          eul[k].y = proj[k].y + vy_intp[k] * delta_t;
        }

        ffb_calcv(t + 0.5 * delta_t);

        accept = true;
        for (k = 0; k < lx * ly; k++) {
          if (proj[k].x + 0.5 * delta_t * vx_intp[k] < 0.0 ||
              proj[k].x + 0.5 * delta_t * vx_intp[k] > lx ||
              proj[k].y + 0.5 * delta_t * vy_intp[k] < 0.0 ||
              proj[k].y + 0.5 * delta_t * vy_intp[k] > ly) {
            accept = false;
            printError("NOT ACCEPTED_A: {0}, trying {1}", delta_t, delta_t * DEC_AFTER_NOT_ACC);
            delta_t *= DEC_AFTER_NOT_ACC;
            break;
          }
        }
        if (accept) {
          printError("ACCEPTED_A: {0}", delta_t);
          accept = integrateAcceptedTimestep(
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
              context.ABS_TOL());
        }
        if (!accept) {
          non_accepted_dts_count++;
          printError("NOT ACCEPTED_B: {0}, trying {1}", delta_t, delta_t * DEC_AFTER_NOT_ACC);
          delta_t *= DEC_AFTER_NOT_ACC;
        }
      }

      if (iter % 1 == 0) {
        printError("iter = {0}, t = {1}, delta_t = {2}", iter, t, delta_t);
      }
      t += delta_t;
      iter++;
      for (k = 0; k < lx * ly; k++) {
        proj[k].x = mid[k].x;
        proj[k].y = mid[k].y;
      }
      delta_t *= INC_AFTER_ACC;

    } while (t < 1.0);
    printError(
        "Finished integration with iter = {0}, t = {1}, delta_t = {2}, non accepted dts= {3}",
        iter,
        t,
        delta_t,
        non_accepted_dts_count);
  }

  private static boolean integrateAcceptedTimestep(
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
    // TODO candidate for parallelization
    return IntStream.range(0, lx * ly)
        .parallel()
        .allMatch(k -> {
          vx_intp_half[k] = interpolateX(
              lx,
              ly,
              proj[k].x + 0.5 * delta_t * vx_intp[k],
              proj[k].y + 0.5 * delta_t * vy_intp[k],
              gridvx);
          vy_intp_half[k] = interpolateY(
              lx,
              ly,
              proj[k].x + 0.5 * delta_t * vx_intp[k],
              proj[k].y + 0.5 * delta_t * vy_intp[k],
              gridvy);
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
      double[] vx_intp,
      double[] vy_intp,
      int lx,
      int ly,
      Point[] proj,
      double[] gridvx,
      double[] gridvy) {
    IntStream.range(0, lx * ly)
        .parallel()
        .forEach(k -> {
          vx_intp[k] = interpolateX(lx, ly, proj[k].x, proj[k].y, gridvx);
          vy_intp[k] = interpolateY(lx, ly, proj[k].x, proj[k].y, gridvy);
        });
  }

  void ffb_calcv(double t) {
    int lx = context.getLx();
    int ly = context.getLy();
    double[] gridvx = context.getGridvx().getOutputData();
    double[] gridvy = context.getGridvy().getOutputData();
    double[] rho_ft = context.getRho_ft();
    double[] rho_init = context.getRho_init();
    double[] grid_fluxx_init = context.getGrid_fluxx_init().getOutputData();
    double[] grid_fluxy_init = context.getGrid_fluxy_init().getOutputData();

    // TODO candidate for parallelization
    IntStream.range(0, lx * ly)
        .parallel()
        .forEach(k -> {
          double rho = rho_ft[0] + (1.0 - t) * (rho_init[k] - rho_ft[0]);
          gridvx[k] = -grid_fluxx_init[k] / rho;
          gridvy[k] = -grid_fluxy_init[k] / rho;
        });
  }
}
