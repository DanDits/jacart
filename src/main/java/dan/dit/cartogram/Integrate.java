package dan.dit.cartogram;

import dan.dit.cartogram.dft.FftPlan2D;

import java.text.MessageFormat;

public class Integrate {
    private static final double INC_AFTER_ACC = 1.1;
    private static final double DEC_AFTER_NOT_ACC = 0.75;
    private final CartogramContext context;

    public Integrate(CartogramContext context) {
        this.context = context;
    }


    double interpolate(double x, double y, double[] grid, char zero) {
        int lx = context.getLx();
        int ly = context.getLy();
        final double x0 = Math.max(0.0, Math.floor(x + 0.5) - 0.5);
        final double x1 = Math.min(lx, Math.floor(x + 0.5) + 0.5);
        final double y0 = Math.max(0.0, Math.floor(y + 0.5) - 0.5);
        final double y1 = Math.min(ly, Math.floor(y + 0.5) + 0.5);
        final double delta_x = (x - x0) / (x1 - x0);
        final double delta_y = (y - y0) / (y1 - y0);
        final double fx0y0 = getFx0y0(x, y, grid, zero, ly, (int) x0, (int) y0);
        final double fx0y1 = getFx0y1(x, y, grid, zero, ly, (int) x0, (int) y1);
        final double fx1y0 = getFx1y0(x, y, grid, zero, lx, ly, (int) x1, (int) y0);
        final double fx1y1 = getFx1y1(x, y, grid, zero, lx, ly, (int) x1, (int) y1);

        return (1 - delta_x) * (1 - delta_y) * fx0y0 + (1 - delta_x) * delta_y * fx0y1
                + delta_x * (1 - delta_y) * fx1y0 + delta_x * delta_y * fx1y1;
    }

    private double getFx1y1(double x, double y, double[] grid, char zero, int lx, int ly, int x1, int y1) {
        if ((x >= lx - 0.5 && y >= ly - 0.5) || (x >= lx - 0.5 && zero == 'x') ||
                (y >= ly - 0.5 && zero == 'y')) {
            return 0.0;
        } else if (x >= lx - 0.5 && y < ly - 0.5 && zero == 'y') {
            return grid[(lx - 1) * ly + y1];
        } else if (x < lx - 0.5 && y >= ly - 0.5 && zero == 'x') {
            return grid[x1 * ly + ly - 1];
        } else {
            return grid[x1 * ly + y1];
        }
    }

    private double getFx1y0(double x, double y, double[] grid, char zero, int lx, int ly, int x1, int y0) {
        if ((x >= lx - 0.5 && y < 0.5) || (x >= lx - 0.5 && zero == 'x') ||
                (y < 0.5 && zero == 'y')) {
            return 0.0;
        } else if (x >= lx - 0.5 && y >= 0.5 && zero == 'y') {
            return grid[(lx - 1) * ly + y0];
        } else {
            return grid[x1 * ly + y0];
        }
    }

    private double getFx0y0(double x, double y, double[] grid, char zero, int ly, int x0, int y0) {
        if ((x < 0.5 && y < 0.5) || (x < 0.5 && zero == 'x') ||
                (y < 0.5 && zero == 'y')) {
            return 0.0;
        } else {
            return grid[x0 * ly + y0];
        }
    }

    private double getFx0y1(double x, double y, double[] grid, char zero, int ly, int x0, int y1) {
        if ((x < 0.5 && y >= ly - 0.5) || (x < 0.5 && zero == 'x') ||
                (y >= ly - 0.5 && zero == 'y')) {
            return 0.0;
        } else if (x >= 0.5 && y >= ly - 0.5 && zero == 'x') {
            return grid[x0 * ly + ly - 1];
        } else {
            return grid[x0 * ly + y1];
        }
    }

    private void printError(String text, Object... parameters) {
        System.err.println(MessageFormat.format(text, parameters));
    }

    void init_gridv() {
        int lx = context.getLx();
        int ly = context.getLy();
        double[] rho_ft = context.getRho_ft();
        double di;
        int i, j;

        final double dlx = lx;
        final double dly = ly;

        for (i = 0; i < lx * ly; i++) {
            rho_ft[i] /= 4 * lx * ly;
        }

        FftPlan2D grid_fluxx_init_plan = context.getGrid_fluxx_init();
        FftPlan2D grid_fluxy_init_plan = context.getGrid_fluxy_init();
        double[] grid_fluxx_init = grid_fluxx_init_plan.getOutputData();
        double[] grid_fluxy_init = grid_fluxy_init_plan.getOutputData();
        for (i = 0; i < lx - 1; i++) {
            di = (double) i;
            for (j = 0; j < ly; j++)
                grid_fluxx_init[i * ly + j] =
                        -rho_ft[(i + 1) * ly + j] /
                                (Math.PI * ((di + 1) / dlx + (j / (di + 1)) * (j / dly) * (dlx / dly)));
        }
        for (j = 0; j < ly; j++)
            grid_fluxx_init[(lx - 1) * ly + j] = 0.0;
        for (i = 0; i < lx; i++) {
            di = (double) i;
            for (j = 0; j < ly - 1; j++)
                grid_fluxy_init[i * ly + j] =
                        -rho_ft[i * ly + j + 1] /
                                (Math.PI * ((di / (j + 1)) * (di / dlx) * (dly / dlx) + (j + 1) / dly));
        }
        for (i = 0; i < lx; i++)
            grid_fluxy_init[i * ly + ly - 1] = 0.0;

        grid_fluxx_init_plan.execute();
        grid_fluxy_init_plan.execute();
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
        delta_t = 1e-2;
        iter = 0;

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
                        delta_t *= DEC_AFTER_NOT_ACC;
                        break;
                    }
                }
                if (accept) {
                    accept = integrateAcceptedTimestep(delta_t, vx_intp, vx_intp_half, vy_intp, vy_intp_half, eul, mid, lx, ly, proj, gridvx, gridvy);
                }
                if (!accept) {
                    delta_t *= DEC_AFTER_NOT_ACC;
                }
            }

            if (iter % 10 == 0) {
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
    }

    private boolean integrateAcceptedTimestep(double delta_t, double[] vx_intp, double[] vx_intp_half, double[] vy_intp, double[] vy_intp_half, Point[] eul, Point[] mid, int lx, int ly, Point[] proj, double[] gridvx, double[] gridvy) {
        // TODO candidate for parallelization
        for (int k = 0; k < lx * ly; k++) {
            vx_intp_half[k] = interpolate(proj[k].x + 0.5 * delta_t * vx_intp[k],
                    proj[k].y + 0.5 * delta_t * vy_intp[k],
                    gridvx, 'x');
            vy_intp_half[k] = interpolate(proj[k].x + 0.5 * delta_t * vx_intp[k],
                    proj[k].y + 0.5 * delta_t * vy_intp[k],
                    gridvy, 'y');
            mid[k].x = proj[k].x + vx_intp_half[k] * delta_t;
            mid[k].y = proj[k].y + vy_intp_half[k] * delta_t;

            if ((mid[k].x - eul[k].x) * (mid[k].x - eul[k].x) +
                    (mid[k].y - eul[k].y) * (mid[k].y - eul[k].y) > context.ABS_TOL() ||
                    mid[k].x < 0.0 || mid[k].x > lx ||
                    mid[k].y < 0.0 || mid[k].y > ly) {
                return false;
            }
        }
        return true;
    }

    private void interpolateSpeed(double[] vx_intp, double[] vy_intp, int lx, int ly, Point[] proj, double[] gridvx, double[] gridvy) {
        int k;// TODO candidate for parallelization
        for (k = 0; k < lx * ly; k++) {
            vx_intp[k] = interpolate(proj[k].x, proj[k].y, gridvx, 'x');
            vy_intp[k] = interpolate(proj[k].x, proj[k].y, gridvy, 'y');
        }
    }

    void ffb_calcv(double t) {
        double rho;
        int k;
        int lx = context.getLx();
        int ly = context.getLy();
        double[] gridvx = context.getGridvx().getOutputData();
        double[] gridvy = context.getGridvy().getOutputData();
        double[] rho_ft = context.getRho_ft();
        double[] rho_init = context.getRho_init();
        double[] grid_fluxx_init = context.getGrid_fluxx_init().getOutputData();
        double[] grid_fluxy_init = context.getGrid_fluxy_init().getOutputData();

        // TODO candidate for parallelization
        for (k = 0; k < lx * ly; k++) {
            rho = rho_ft[0] + (1.0 - t) * (rho_init[k] - rho_ft[0]);
            gridvx[k] = -grid_fluxx_init[k] / rho;
            gridvy[k] = -grid_fluxy_init[k] / rho;
        }
    }
}
