package dan.dit.cartogram.main;

import dan.dit.cartogram.core.context.Point;
import dan.dit.cartogram.core.pub.ResultPolygon;
import dan.dit.cartogram.core.pub.ResultRegion;

import java.io.*;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

public class EpsWriter {
  public static final int GRAT_LINES = 64;

  public void ps_figure(OutputStream out, int lx, int ly, List<ResultRegion> regions, Point[] prj, boolean plotGraticule) {

    Locale.setDefault(Locale.US); // required for writig EPS correctly for now
    PrintWriter printWriter = new PrintWriter(out);

    printWriter.println("%!PS-Adobe-2.0 EPSF-2.0");
    printWriter.println("%%Title: Cartogram");
    printWriter.println("%%Creator: Daniel Dittmar based on Michael T. Gastner et al.");
    printWriter.println(
      MessageFormat.format("%%BoundingBox: 0 0 {0} {1}", lx, ly));
    printWriter.println("%%Magnification: 1.0000");
    printWriter.println("%%EndComments");
    printWriter.println("/m {moveto} def\n/l {lineto} def\n/s {stroke} def");
    printWriter.println("/n {newpath} def\n/c {closepath} def\n/f {fill} def");
    printWriter.println("/SLW {setlinewidth} def\n/SGRY {setgray} def");
    printWriter.println("/SRGB {setrgbcolor} def");

    printWriter.println("0.7 SLW");
    for (ResultRegion resultRegion : regions) {
      for (ResultPolygon polygon : resultRegion.getPolygons()) {
        List<Point> ring = polygon.getExteriorRing();
        drawRing(printWriter, resultRegion, ring, "0.96 0.92 0.70");
        for (List<Point> hole : polygon.getInteriorRings()) {
          drawRing(printWriter, resultRegion, hole, "1 1 1");
        }
      }
    }

    if (plotGraticule) {
      printWriter.println("0.3 SLW 0 0 1 SRGB");
      for (int j = 0; j < ly; j += Math.max(lx, ly) / GRAT_LINES) {
        printWriter.println(MessageFormat.format("{0} {1} m", prj[j].x, prj[j].y));
        for (int i = 1; i < lx; i++) {
          printWriter.println(MessageFormat.format("{0} {1} l", prj[i * ly + j].x, prj[i * ly + j].y));
        }
        printWriter.println("s");
      }
      for (int i = 0; i < lx; i += Math.max(lx, ly) / GRAT_LINES) {
        printWriter.println(MessageFormat.format("{0} {1} m", prj[i * ly].x, prj[i * ly].y));
        for (int j = 1; j < ly; j++) {
          printWriter.println(MessageFormat.format("{0} {1} l", prj[i * ly + j].x, prj[i * ly + j].y));
        }
        printWriter.println("s");
      }
    }

    printWriter.println("showpage");
    printWriter.flush();
  }

  private void drawRing(PrintWriter printWriter, ResultRegion resultRegion, List<Point> points, String color) {
    printWriter.println("n");
    printWriter.println(MessageFormat.format("{0} {1} m", points.get(0).x, points.get(0).y));
    points.stream()
      .skip(1)
      .forEach(point -> printWriter.println(MessageFormat.format("{0} {1} l", point.x, point.y)));
    printWriter.println("c");
    if (!resultRegion.isNaN()) {
      printWriter.println("gsave\n" + color + " SRGB f\ngrestore\n0 SGRY s");
    } else {
      printWriter.println("gsave\n0.75 SGRY f\ngrestore\n0 SGRY s");
    }
  }
}
