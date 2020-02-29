package de.dandit.cartogram.core.context;

import de.dandit.cartogram.core.dft.FftPlan2D;
import de.dandit.cartogram.core.pub.FftPlanFactory;

public class MapGrid {
  private final int lx;
  private final int ly;
  private final double initialDeltaX;
  private final double initialDeltaY;
  private final double initialScalingFactor;
  private final double absoluteTolerance;
  private final double[] gridSpeedX;
  private final double[] gridSpeedY;
  private final Point[] gridProjection;
  private final Point[] gridProjectionSwapper;
  private final double[] rhoFt;
  private final double[] rhoInit;
  private final int[][] gridIndexToRegionIndex;
  private final FftPlan2D gridFluxInitX;
  private final FftPlan2D gridFluxInitY;
  private final FftPlan2D rho;

  public MapGrid(FftPlanFactory fftPlanFactory, int lx, int ly, double initialDeltaX, double initialDeltaY, double initialScalingFactor) {
    this.lx = lx;
    this.ly = ly;
    this.initialDeltaX = initialDeltaX;
    this.initialDeltaY = initialDeltaY;
    this.initialScalingFactor = initialScalingFactor;
    this.absoluteTolerance = Math.min(lx, ly) * 1e-6;
    this.gridSpeedX = new double[lx * ly];
    this.gridSpeedY = new double[lx * ly];
    this.gridProjection = initProjectionOnGrid(lx, ly);
    this.gridProjectionSwapper = initEmptyProjection(lx, ly);
    this.rhoInit = new double[lx * ly];
    this.rhoFt = new double[lx * ly];
    this.gridIndexToRegionIndex = new int[lx][ly];
    this.gridFluxInitX = fftPlanFactory.createDCT3_DST3_2D(lx, ly);
    this.gridFluxInitY = fftPlanFactory.createDST3_DCT3_2D(lx, ly);
    this.rho = fftPlanFactory.createDCT2_2D(lx, ly, rhoInit, rhoFt);
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

  public double getInitialDeltaX() {
    return initialDeltaX;
  }

  public double getInitialDeltaY() {
    return initialDeltaY;
  }

  public double getInitialScalingFactor() {
    return initialScalingFactor;
  }

  public double[] getGridSpeedX() {
    return gridSpeedX;
  }

  public double[] getGridSpeedY() {
    return gridSpeedY;
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

  public Point[] getGridProjection() {
    return gridProjection;
  }

  public Point[] getGridProjectionSwapper() {
    return gridProjectionSwapper;
  }

  public double[] getRhoFt() {
    return rhoFt;
  }

  public double[] getRhoInit() {
    return rhoInit;
  }

  public int[][] getGridIndexToRegionIndex() {
    return gridIndexToRegionIndex;
  }

  public FftPlan2D getGridFluxInitX() {
    return gridFluxInitX;
  }

  public FftPlan2D getGridFluxInitY() {
    return gridFluxInitY;
  }

  public FftPlan2D getRho() {
    return rho;
  }
}
