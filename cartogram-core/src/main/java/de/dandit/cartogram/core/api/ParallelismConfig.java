package de.dandit.cartogram.core.api;

import java.util.stream.IntStream;

public class ParallelismConfig {

  private final boolean runInParallel;

  private ParallelismConfig(boolean runInParallel) {
    this.runInParallel = runInParallel;
  }

  public static ParallelismConfig ofSingleThreaded() {
    return new ParallelismConfig(false);
  }

  public static ParallelismConfig ofCommonPool() {
    return new ParallelismConfig(true);
  }

  public IntStream apply(IntStream stream) {
    if (!runInParallel) {
      return stream;
    }
    return stream.parallel();
  }
}
