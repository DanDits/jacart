package de.dandit.cartogram.dft;

public interface InplaceDftAlgorithm {

  void execute(double[] data, double[] cosTable, double[] sinTable);
}