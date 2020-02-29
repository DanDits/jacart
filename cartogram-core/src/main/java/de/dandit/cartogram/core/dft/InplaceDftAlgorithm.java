package de.dandit.cartogram.core.dft;

public interface InplaceDftAlgorithm {

  void execute(double[] data, double[] cosTable, double[] sinTable);
}
