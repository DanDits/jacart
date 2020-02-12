package dan.dit.cartogram;

import java.text.MessageFormat;

public class Logging {

  public static void debug(String text, Object... args) {
    System.out.println(MessageFormat.format(text, args));
  }

  public static void error(String text, Object... args) {
    System.err.println(MessageFormat.format(text, args));
  }
}
