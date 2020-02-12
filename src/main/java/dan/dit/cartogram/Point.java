package dan.dit.cartogram;

public class Point { // TODO remove this Point class since it represents performance overhead that we must avoid at the cost of less abstractions
  public double x;
  public double y;

  public Point(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public Point createCopy() {
    return new Point(x, y);
  }
}
