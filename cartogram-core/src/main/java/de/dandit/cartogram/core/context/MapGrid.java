package de.dandit.cartogram.core.context;

import java.util.Arrays;

import de.dandit.cartogram.core.dft.FftPlan2D;
import de.dandit.cartogram.core.api.FftPlanFactory;

public class MapGrid {
  private final int lx;
  private final int ly;
  private final double initialDeltaX;
  private final double initialDeltaY;
  private final double initialScalingFactor;
  private final double absoluteTolerance;
  private final double[] gridSpeedX;
  private final double[] gridSpeedY;
  private final double[] gridProjectionX;
  private final double[] gridProjectionY;
  private final double[] gridProjectionXSwapper;
  private final double[] gridProjectionYSwapper;
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
    this.gridProjectionX = initProjectionXOnGrid(lx, ly);
    this.gridProjectionY = initProjectionYOnGrid(lx, ly);
    this.gridProjectionXSwapper = initEmptyProjection(lx, ly);
    this.gridProjectionYSwapper = initEmptyProjection(lx, ly);
    this.rhoInit = new double[lx * ly];
    this.rhoFt = new double[lx * ly];
    this.gridIndexToRegionIndex = new int[lx][ly];
    this.gridFluxInitX = fftPlanFactory.createDCT3_DST3_2D(lx, ly);
    this.gridFluxInitY = fftPlanFactory.createDST3_DCT3_2D(lx, ly);
    this.rho = fftPlanFactory.createDCT2_2D(lx, ly, rhoInit, rhoFt);
  }

  private static double[] initEmptyProjection(int lx, int ly) {
    double[] projection = new double[lx * ly];
    Arrays.fill(projection, Double.NaN);
    return projection;
  }

  private static double[] initProjectionXOnGrid(int lx, int ly) {
    double[] projection = new double[lx * ly];
    for (int i = 0; i < lx; i++) {
      for (int j = 0; j < ly; j++) {
        projection[i * ly + j] = i + 0.5;
      }
    }
    return projection;
  }

  private static double[] initProjectionYOnGrid(int lx, int ly) {
    double[] proj = new double[lx * ly];
    for (int i = 0; i < lx; i++) {
      for (int j = 0; j < ly; j++) {
        proj[i * ly + j] = j + 0.5;
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

  public double[] getGridProjectionX() {
    return gridProjectionX;
  }

  public double[] getGridProjectionY() {
    return gridProjectionY;
  }

  public double[] getGridProjectionXSwapper() {
    return gridProjectionXSwapper;
  }

  public double[] getGridProjectionYSwapper() {
    return gridProjectionYSwapper;
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
