package dan.dit.cartogram.dft;

import java.util.function.Consumer;
import java.util.stream.IntStream;

public class FftPlan2D {

    private final int width;
    private final int height;
    private final double[] inputTabularData; // row-major layout (so: first width elements are the first row)
    private final double[] outputTabularData; // row-major layout (so: first width elements are the first row)
    private final Consumer<double[]> inplaceAlgorithm; // TODO avoid allocations and re-use the cos/sin tables in the plan!

    public FftPlan2D(
      int width, int height,
      double[] inputTabularData,
      double[] outputTabularData,
      Consumer<double[]> inplaceAlgorithm) {
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
        this.inplaceAlgorithm = inplaceAlgorithm;
    }

    public void execute() {
        if (inputTabularData != outputTabularData) {
            System.arraycopy(inputTabularData, 0, outputTabularData, 0, inputTabularData.length);
        }
        executePerRow();
        executePerColumn();
    }

    private void executePerColumn() {
    IntStream.range(0, width)
        .parallel()
        .forEach(col -> {
          double[] heightBuffer = new double[height];
          for (int row = 0; row < height; row++) {
            heightBuffer[row] = outputTabularData[row * width + col];
          }
          inplaceAlgorithm.accept(heightBuffer);
          for (int row = 0; row < height; row++) {
            outputTabularData[row * width + col] = heightBuffer[row];
          }
        });
    }

    private void executePerRow() {
    IntStream.range(0, height)
        .parallel()
        .forEach(row -> {
          double[] widthBuffer = new double[width];
          int indexOffset = row * width;
            System.arraycopy(outputTabularData, indexOffset, widthBuffer, 0, widthBuffer.length);
            inplaceAlgorithm.accept(widthBuffer); // TODO could optimize by letting that work in place without having to copy content two times, not sure if worth it
            System.arraycopy(widthBuffer, 0, outputTabularData, indexOffset, widthBuffer.length);
        });
    }

    public double[] getOutputData() {
        return outputTabularData;
    }
}
