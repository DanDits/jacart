package dan.dit.cartogram;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.util.function.Consumer;

import static dan.dit.cartogram.Density.MAX_PERMITTED_AREA_ERROR;

public class Cartogram {

    private final CartogramContext context;
    private final Integrate integrate;
    private final DiffIntegrate diffIntegrate;
    private final Density density;
    private final CartogramConfig config;

    public Cartogram() {
        this.context = new CartogramContext();
        this.integrate = new Integrate(context);
        this.diffIntegrate = new DiffIntegrate(context);
        this.density = new Density(context);
        // TODO diff=true seems to be very very slow right now, investigate
        this.config = new CartogramConfig(false, true, true, true);
    }

    public CartogramContext calculate(MapFeatureData featureData, Consumer<CartogramContext> contextConsumer) throws FileNotFoundException {
        boolean onlyOneRegionExists = density.fill_with_density1(featureData, config);
        if (onlyOneRegionExists) {
            System.out.println("Hint: Only one region exists, output will only be an affine transformation.");
        }
        double[] area_err = context.getAreaError();
        double[] cart_area = context.getCartogramArea();
        double[] init_tot_area = new double[1];
        // also initializes init_tot_area...
        if (max_area_err(area_err, cart_area, context.getPolycorn(), init_tot_area) <= MAX_PERMITTED_AREA_ERROR) {
            System.out.println("Nothing to do, area already correct.");
            return null;
        }
        int lx = context.getLx();
        int ly = context.getLy();
        Point[] proj = context.initProj();
        contextConsumer.accept(context);
        Point[][] cartcorn = context.initCartcorn();
        contextConsumer.accept(context);


        printDebug("Starting integration 1\n");
        boolean diff = config.isDiff();
        if (!diff) {
            integrate.ffb_integrate();
        } else {
            diffIntegrate.diff_integrate();
        }
        project(false);

        FileOutputStream epsOut = new FileOutputStream(new File("/home/daniel/cartogram/java/src/main/resources/dan/dit/cartogram/main/initial.eps"));
        new EpsWriter().ps_figure(
                epsOut,
                context.getLx(),
                context.getLy(),
                context.getPolyinreg(),
                context.getRegionNa(),
                context.getCartcorn(),
                context.getProj(),
                true);

        double[] cartogramTotalArea = new double[1];
        double mae = max_area_err(area_err, cart_area, cartcorn, cartogramTotalArea);
        printDebug("max. abs. area error: {0}", mae);

        Point[] proj2 = context.initProj2();
        context.initIntegration();
        contextConsumer.accept(context);
        while (mae > MAX_PERMITTED_AREA_ERROR) {
            density.fill_with_density2();

            for (int i = 0; i < lx; i++) {
                for (int j = 0; j < ly; j++) {
                    proj2[i * ly + j].x = proj[i * ly + j].x;
                    proj2[i * ly + j].y = proj[i * ly + j].y;
                }
            }
            for (int i = 0; i < lx; i++)
                for (int j = 0; j < ly; j++) {
                    proj[i * ly + j].x = i + 0.5;
                    proj[i * ly + j].y = j + 0.5;
                }
            int integration = context.onIntegration();
            printDebug("Starting integration {0}", integration);
            if (!diff) {
                integrate.ffb_integrate();
            } else {
                diffIntegrate.diff_integrate();
            }
            project(true);

            for (int i = 0; i < lx; i++) {
                for (int j = 0; j < ly; j++) {
                    proj[i * ly + j].x = proj2[i * ly + j].x;
                    proj[i * ly + j].y = proj2[i * ly + j].y;
                }
            }
            mae = max_area_err(area_err, cart_area, cartcorn, cartogramTotalArea);
            printDebug("max. abs. area error: {0}", mae);
        }


        scalePolygonsToMatchInitialTotalArea(Math.sqrt(init_tot_area[0] / cartogramTotalArea[0]), lx, ly, cartcorn);

        double final_max_area_error = max_area_err(area_err, cart_area, cartcorn, cartogramTotalArea);
        printDebug("Final error: {0}", final_max_area_error);
        return context;
    }

    private void scalePolygonsToMatchInitialTotalArea(double correction_factor, int lx, int ly, Point[][] cartcorn) {
        int n_poly = cartcorn.length;
        int[] n_polycorn = context.getN_polycorn();
        printDebug("correction_factor = {0}", correction_factor);
        for (int i = 0; i < n_poly; i++) {
            for (int j = 0; j < n_polycorn[i]; j++) {
                cartcorn[i][j].x =
                        correction_factor * (cartcorn[i][j].x - 0.5 * lx) + 0.5 * lx;
                cartcorn[i][j].y =
                        correction_factor * (cartcorn[i][j].y - 0.5 * ly) + 0.5 * ly;
            }
        }
    }

    private void printDebug(String text, Object... parameters) {
        System.out.println(MessageFormat.format(text, parameters));
    }


    void project(boolean proj_graticule) {
        double x2, y2;
        double[] xdisp, ydisp;
        int i, j;
        int lx = context.getLx();
        int ly = context.getLy();
        Point[] proj = context.getProj();

        xdisp = new double[lx * ly];
        ydisp = new double[lx * ly];
        for (i = 0; i < lx; i++) {
            for (j = 0; j < ly; j++) {
                xdisp[i * ly + j] = proj[i * ly + j].x - i - 0.5;
                ydisp[i * ly + j] = proj[i * ly + j].y - j - 0.5;
            }
        }

        int[] n_polycorn = context.getN_polycorn();
        Point[][] polycorn = context.getPolycorn();
        int n_poly = polycorn.length;
        Point[] proj2 = context.getProj2();
        Point[][] cartcorn = context.getCartcorn();

        for (i = 0; i < n_poly; i++) {
            for (j = 0; j < n_polycorn[i]; j++) {
                cartcorn[i][j].x =
                    Integrate.interpolateX(lx, ly, polycorn[i][j].x, polycorn[i][j].y, xdisp)
                                + polycorn[i][j].x;
                cartcorn[i][j].y =
                    Integrate.interpolateY(lx, ly, polycorn[i][j].x, polycorn[i][j].y, ydisp)
                                + polycorn[i][j].y;
            }
        }
        if (proj_graticule) {

            for (i = 0; i < lx * ly; i++) {
                x2 = proj2[i].x;
                y2 = proj2[i].y;
                proj2[i].x = Integrate.interpolateX(lx, ly, x2, y2, xdisp) + x2;
                proj2[i].y = Integrate.interpolateY(lx, ly, x2, y2, ydisp) + y2;
            }
        }
    }

    double max_area_err(double[] area_err, double[] cart_area, Point[][] corn,
                        double[] sum_cart_area) {
        double max, obj_area, sum_target_area;
        int i, j;

        int[] n_polycorn = context.getN_polycorn();
        int[][] polyinreg = context.getPolyinreg();
        int n_reg = polyinreg.length;
        double[] target_area = context.getTarget_area();
        for (i = 0; i < n_reg; i++) {
            // if all polygons in a region were tiny they will be removed and thus it will be impossible for
            // the cartogram area to reach the target area (e.g.: Washington D.C.)
            // or we could also remove the region and ignore it completely
            int[] polyI = polyinreg[i];
            if (polyI.length > 0) {
                cart_area[i] = 0.0;
                for (j = 0; j < polyI.length; j++) {
                    cart_area[i] += Polygon.polygon_area(n_polycorn[polyI[j]], corn[polyI[j]]);
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

        int lx = context.getLx();
        int ly = context.getLy();
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

        int lx = context.getLx();
        int ly = context.getLy();
        Point[] proj = context.getProj();

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
                projgrid[i][j].x = Integrate.interpolateX(lx, ly, i, j, xdisp) + i;
                projgrid[i][j].y = Integrate.interpolateY(lx, ly, i, j, ydisp) + j;
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
            Density.set_inside_values_for_polygon(i, 3, tri[i], xyhalfshift2tri);
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
