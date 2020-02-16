package dan.dit.cartogram.core.context;

import dan.dit.cartogram.core.pub.FftPlanFactory;
import dan.dit.cartogram.dft.FftPlan2D;

public class MapGrid {
  private final int lx;
  private final int ly;
  private final double absoluteTolerance;
  private final double[] gridvx;
  private final double[] gridvy;
  private final Point[] proj;
  private final Point[] proj2;
  private final double[] rho_ft;
  private final double[] rho_init;
  private final int[][] xyhalfshift2reg;
  private final FftPlan2D grid_fluxx_init;
  private final FftPlan2D grid_fluxy_init;
  private final FftPlan2D plan_fwd;
  private final FftPlan2D rho;

  public MapGrid(FftPlanFactory fftPlanFactory, int lx, int ly) {
    this.lx = lx;
    this.ly = ly;
    this.absoluteTolerance = Math.min(lx, ly) * 1e-6;
    this.gridvx = new double[lx * ly];
    this.gridvy = new double[lx * ly];
    this.proj = initProjectionOnGrid(lx, ly);
    this.proj2 = initEmptyProjection(lx, ly);
    this.rho_init = new double[lx * ly];
    this.rho_ft = new double[lx * ly];
    this.xyhalfshift2reg = new int[lx][ly];
    this.grid_fluxx_init = fftPlanFactory.createDCT3_DST3_2D(lx, ly);
    this.grid_fluxy_init = fftPlanFactory.createDST3_DCT3_2D(lx, ly);
    this.plan_fwd = fftPlanFactory.createDCT2_2D(lx, ly, rho_init, rho_ft);
    this.rho = fftPlanFactory.createDCT3_2D(lx, ly);
  }

  private static Point[] initEmptyProjection(int lx, int ly) {
    Point[] proj = new Point[lx * ly];
    for (int i = 0; i < proj.length; i++) {
      proj[i] = new Point(Double.NaN, Double.NaN);
    }
    return proj;
  }

  private static Point[] initProjectionOnGrid(int lx, int ly) {
    Point[] proj = new Point[lx * ly];
    for (int i = 0; i < lx; i++) {
      for (int j = 0; j < ly; j++) {
        proj[i * ly + j] = new Point(i + 0.5, j + 0.5);
      }
    }
    return proj;
  }

  public double[] getGridvx() {
    return gridvx;
  }

  public double[] getGridvy() {
    return gridvy;
  }

  public int getLx() {
    return lx;
  }

  public int getLy() {
    return ly;
  }

  public double getAbsoluteTolerance() {
    return absoluteTolerance;
  }

  public Point[] getProj() {
    return proj;
  }

  public Point[] getProj2() {
    return proj2;
  }

  public double[] getRho_ft() {
    return rho_ft;
  }

  public double[] getRho_init() {
    return rho_init;
  }

  public int[][] getXyhalfshift2reg() {
    return xyhalfshift2reg;
  }

  public FftPlan2D getGrid_fluxx_init() {
    return grid_fluxx_init;
  }

  public FftPlan2D getGrid_fluxy_init() {
    return grid_fluxy_init;
  }

  public FftPlan2D getPlan_fwd() {
    return plan_fwd;
  }

  public FftPlan2D getRho() {
    return rho;
  }


}
