package dan.dit.cartogram.core.pub;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Logging {

  private final Consumer<String> debugConsumer;
  private final Consumer<String> errorConsumer;

  private Logging(Consumer<String> debugConsumer, Consumer<String> errorConsumer) {
    this.debugConsumer = Objects.requireNonNull(debugConsumer);
    this.errorConsumer = Objects.requireNonNull(errorConsumer);
  }

  public static Logging disabled() {
    return new Logging(ignored -> {}, ignored -> {});
  }

  public static Logging of(Logger logger) {
    Objects.requireNonNull(logger);
    return new Logging(
      text -> logger.log(Level.FINE, text),
      text -> logger.log(Level.SEVERE, text));
  }

  public static Logging ofStandardOutput() {
    return new Logging(
      System.out::println,
      System.err::println);
  }

  public void debug(String text, Object... args) {
    debugConsumer.accept(getFormattedText(text, args));
  }

  public void error(String text, Object... args) {
    errorConsumer.accept(getFormattedText(text, args));
  }

  public void displayIntArray(String text, int[] data) {
    debug(text + " (length={0}, sum={1,number,#.#########}) First entries: {2}",
        data.length,
        Arrays.stream(data).sum(),
        Arrays.stream(data)
        .limit(10L)
        .mapToObj(Integer::toString)
        .collect(Collectors.joining(", ")));
  }

  public void displayDoubleArray(String text, double[] data) {
    debug(text + " (length={0}, sum={1,number,#.########}) First entries: {2}",
        data.length,
        Arrays.stream(data).sum(),
        Arrays.stream(data)
            .limit(10L)
            .mapToObj(Double::toString)
            .collect(Collectors.joining(", ")));
  }

  private static String getFormattedText(String text, Object[] args) {
    return MessageFormat.format(text, args);
  }
}
