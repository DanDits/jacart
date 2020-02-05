package dan.dit.cartogram;

import dan.dit.cartogram.dft.FftPlan2D;

import java.text.MessageFormat;

public class DiffIntegrate {
    public static final double INC_AFTER_ACC = 1.1;
    public static final double DEC_AFTER_NOT_ACC = 0.75;
    public static final double MAX_ITER = 10000;
    public static final double MIN_T = 1e3;
    public static final double MAX_T = 1e12;

    private final CartogramContext context;
    private final Integrate integrate;

    public DiffIntegrate(CartogramContext context) {
        this.context = context;
        this.integrate = new Integrate(context);
    }

    public void diff_calcv(double t) {
        double dlx, dly;
        int i, j;

        int lx = context.getLx();
        int ly = context.getLy();
        double[] rho = context.getRho().getOutputData();
        double[] rho_ft = context.getRho_ft();
        dlx = lx;
        dly = ly;

        for (i = 0; i < lx; i++)
            for (j = 0; j < ly; j++)
                rho[i * ly + j] =
                        Math.exp((-(i / dlx) * (i / dlx) - (j / dly) * (j / dly)) * t) * rho_ft[i * ly + j];

        context.getRho().execute();
        double[] gridvx = context.getGridvx().getOutputData();
        double[] gridvy = context.getGridvy().getOutputData();
        for (i = 0; i < lx - 1; i++)
            for (j = 0; j < ly; j++)
                gridvx[i * ly + j] =
                        rho_ft[(i + 1) * ly + j] * (i + 1) *
                                Math.exp((-((i + 1) / dlx) * ((i + 1) / dlx) - (j / dly) * (j / dly)) * t) / (Math.PI * dlx);
        for (j = 0; j < ly; j++)
            gridvx[(lx - 1) * ly + j] = 0.0;
        for (i = 0; i < lx; i++) {
            for (j = 0; j < ly - 1; j++)
                gridvy[i * ly + j] =
                        rho_ft[i * ly + j + 1] * (j + 1) *
                                Math.exp((-(i / dlx) * (i / dlx) - ((j + 1) / dly) * ((j + 1) / dly)) * t) / (Math.PI * dly);
        }
        for (i = 0; i < lx; i++)
            gridvy[i * ly + ly - 1] = 0.0;

        context.getGridvx().execute();
        context.getGridvy().execute();

        for (i = 0; i < lx; i++) {
            for (j = 0; j < ly; j++) {
                if (rho[i * ly + j] <= 0.0) {
                    throw new IllegalStateException(
                            MessageFormat.format("ERROR: division by zero in diff_calcv(): rho[{0}, {1}] = {2}",
                                    i, j, rho[i * ly + j]));
                }
                gridvx[i * ly + j] /= rho[i * ly + j];
                gridvy[i * ly + j] /= rho[i * ly + j];
            }
        }
    }


    public void diff_integrate() {
        boolean accept;
        double delta_t, max_change, t;
        double[] vx_intp, vx_intp_half, vy_intp, vy_intp_half;
        int iter, k;
        Point[] eul, mid;

        int lx = context.getLx();
        int ly = context.getLy();

        context.initRhoPlan();
        context.initGrid();
        FftPlan2D gridvx_plan = context.getGridvx();
        FftPlan2D gridvy_plan = context.getGridvy();

        double[] gridvx = gridvx_plan.getOutputData();
        double[] gridvy = gridvy_plan.getOutputData();

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

        t = 0.0;
        delta_t = 1e-2;
        iter = 0;
        Point[] proj = context.getProj();
        do {
            diff_calcv(t);
            for (k = 0; k < lx * ly; k++) {

                vx_intp[k] = Integrate.interpolateX(lx, ly, proj[k].x, proj[k].y, gridvx);
                vy_intp[k] = Integrate.interpolateY(lx, ly, proj[k].x, proj[k].y, gridvy);
            }

            accept = false;
            while (!accept) {

                eulerStep(delta_t, vx_intp, vy_intp, eul, lx, ly, proj);

                diff_calcv(t + 0.5 * delta_t);

                accept = true;
                for (k = 0; k < lx * ly; k++)
                    if (proj[k].x + 0.5 * delta_t * vx_intp[k] < 0.0 ||
                            proj[k].x + 0.5 * delta_t * vx_intp[k] > lx ||
                            proj[k].y + 0.5 * delta_t * vy_intp[k] < 0.0 ||
                            proj[k].y + 0.5 * delta_t * vy_intp[k] > ly) {
                        accept = false;
                        delta_t *= DEC_AFTER_NOT_ACC;
                        break;
                    }
                if (accept) {
                    for (k = 0; k < lx * ly; k++) {
                        vx_intp_half[k] = Integrate.interpolateX(lx, ly,proj[k].x + 0.5 * delta_t * vx_intp[k],
                                proj[k].y + 0.5 * delta_t * vy_intp[k],
                                gridvx);
                        vy_intp_half[k] = Integrate.interpolateY(lx, ly, proj[k].x + 0.5 * delta_t * vx_intp[k],
                                proj[k].y + 0.5 * delta_t * vy_intp[k],
                                gridvy);
                        mid[k].x = proj[k].x + vx_intp_half[k] * delta_t;
                        mid[k].y = proj[k].y + vy_intp_half[k] * delta_t;

                        if ((mid[k].x - eul[k].x) * (mid[k].x - eul[k].x) +
                                (mid[k].y - eul[k].y) * (mid[k].y - eul[k].y) > context.ABS_TOL() ||
                                mid[k].x < 0.0 || mid[k].x > lx ||
                                mid[k].y < 0.0 || mid[k].y > ly) {
                            accept = false;
                            delta_t *= DEC_AFTER_NOT_ACC;
                            break;
                        }
                    }
                }
            }

            for (k = 0, max_change = 0.0; k < lx * ly; k++)
                max_change = Math.max((mid[k].x - proj[k].x) * (mid[k].x - proj[k].x) +
                                (mid[k].y - proj[k].y) * (mid[k].y - proj[k].y),
                        max_change);
            if (iter % 10 == 0)
                printError("iter = {0}, t = {1}, delta_t = {2}, max_change = {3}",
                        iter, t, delta_t, max_change);

            t += delta_t;
            iter++;
            for (k = 0; k < lx * ly; k++) {
                proj[k].x = mid[k].x;
                proj[k].y = mid[k].y;
            }
            delta_t *= INC_AFTER_ACC;
        } while ((max_change > context.CONV_MAX_CHANGE() && t < MAX_T && iter < MAX_ITER)
                || t < MIN_T);
        printError("Stopped after t = {0} with max_change = {1} and iterations = {2}", t, max_change, iter);

    }

    private void eulerStep(double delta_t, double[] vx_intp, double[] vy_intp, Point[] eul, int lx, int ly, Point[] proj) {
        int k;
        for (k = 0; k < lx * ly; k++) {
            eul[k].x = proj[k].x + vx_intp[k] * delta_t;
            eul[k].y = proj[k].y + vy_intp[k] * delta_t;
        }
    }

    private void printError(String text, Object... parameters) {
        String output = MessageFormat.format(text, parameters);
        System.err.println(output);
    }
}
