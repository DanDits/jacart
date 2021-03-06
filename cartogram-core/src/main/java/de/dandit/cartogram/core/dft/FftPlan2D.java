package de.dandit.cartogram.core.dft;

import de.dandit.cartogram.core.api.ParallelismConfig;

import java.util.stream.IntStream;

public class FftPlan2D {

  private final int width;
  private final int height;
  private final double[] inputTabularData; // column-major layout (so: first height elements are the first column)
  private final double[] outputTabularData; // column-major layout (so: first height elements are the first column)
  private final double[] cosTableWidth;
  private final double[] sinTableWidth;
  private final double[] cosTableHeight;
  private final double[] sinTableHeight;
  private final InPlaceDftAlgorithm inplaceAlgorithmRows;
  private final InPlaceDftAlgorithm inplaceAlgorithmColumns;
  private final ParallelismConfig parallelismConfig;

  public FftPlan2D(
    ParallelismConfig parallelismConfig,
    int width,
    int height,
    double[] inputTabularData,
    double[] outputTabularData,
    InPlaceDftAlgorithm inplaceAlgorithmRows,
    InPlaceDftAlgorithm inplaceAlgorithmColumns) {
    this.parallelismConfig = parallelismConfig;
    if (width * height != inputTabularData.length) {
      throw new IllegalArgumentException("Array size does not match width*height!");
    }
    if (inputTabularData.length != outputTabularData.length) {
      throw new IllegalArgumentException("Input array size does not match output array size");
    }
    this.width = width;
    this.height = height;
    this.inputTabularData = inputTabularData;
    this.outputTabularData = outputTabularData;
    this.inplaceAlgorithmRows = inplaceAlgorithmRows;
    this.inplaceAlgorithmColumns = inplaceAlgorithmColumns;
    this.cosTableWidth = initCosTable(width);
    this.sinTableWidth = initSinTable(width);
    this.cosTableHeight = width == height ? cosTableWidth : initCosTable(height);
    this.sinTableHeight = width == height ? sinTableWidth : initSinTable(height);
  }

  private static double[] initCosTable(int n) {
    double[] cosTable = new double[n / 2];
    for (int i = 0; i < n / 2; i++) {
      cosTable[i] = Math.cos(2 * Math.PI * i / n);
    }
    return cosTable;
  }

  private static double[] initSinTable(int n) {
    double[] sinTable = new double[n / 2];
    for (int i = 0; i < n / 2; i++) {
      sinTable[i] = Math.sin(2 * Math.PI * i / n);
    }
    return sinTable;
  }

  public void execute() {
    if (inputTabularData != outputTabularData) {
      System.arraycopy(inputTabularData, 0, outputTabularData, 0, inputTabularData.length);
    }
    executePerRow();
    executePerColumn();
  }

  private void executePerColumn() {
    parallelismConfig.apply(IntStream.range(0, width))
        .forEach(col -> {
          double[] heightBuffer = new double[height];
          int indexOffset = col * height;
          System.arraycopy(outputTabularData, indexOffset, heightBuffer, 0, heightBuffer.length);
          inplaceAlgorithmColumns.execute(heightBuffer, cosTableHeight, sinTableHeight);
          System.arraycopy(heightBuffer, 0, outputTabularData, indexOffset, heightBuffer.length);
        });
  }

  private void executePerRow() {
    parallelismConfig.apply(IntStream.range(0, height))
        .forEach(row -> {
          double[] widthBuffer = new double[width];
          for (int col = 0; col < width; col++) {
            widthBuffer[col] = outputTabularData[col * height + row];
          }
          inplaceAlgorithmRows.execute(
              widthBuffer,
              cosTableWidth,
              sinTableWidth);
          for (int col = 0; col < width; col++) {
            outputTabularData[col * height + row] = widthBuffer[col];
          }
        });
  }

  public double[] getOutputData() {
    return outputTabularData;
  }
}
