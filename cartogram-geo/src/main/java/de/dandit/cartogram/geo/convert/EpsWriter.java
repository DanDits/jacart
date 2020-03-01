package de.dandit.cartogram.geo.convert;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import de.dandit.cartogram.core.pub.ResultPolygon;
import de.dandit.cartogram.core.pub.ResultRegion;

public class EpsWriter {
  public static final int GRAT_LINES = 64;

  public void createFigure(OutputStream out, int lx, int ly, List<ResultRegion> regions, double[] prjX, double[] prjY, boolean plotGraticule) {

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
        double[] ringX = polygon.getExteriorRingX();
        double[] ringY = polygon.getExteriorRingY();
        drawRing(printWriter, resultRegion, ringX, ringY, "0.96 0.92 0.70");
        for (int i = 0; i < polygon.getInteriorRingsX().size(); i++) {
          drawRing(printWriter, resultRegion, polygon.getInteriorRingsX().get(i), polygon.getInteriorRingsY().get(i), "1 1 1");
        }
      }
    }

    if (plotGraticule) {
      printWriter.println("0.3 SLW 0 0 1 SRGB");
      for (int j = 0; j < ly; j += Math.max(lx, ly) / GRAT_LINES) {
        printWriter.println(MessageFormat.format("{0} {1} m", prjX[j], prjY[j]));
        for (int i = 1; i < lx; i++) {
          printWriter.println(MessageFormat.format("{0} {1} l", prjX[i * ly + j], prjY[i * ly + j]));
        }
        printWriter.println("s");
      }
      for (int i = 0; i < lx; i += Math.max(lx, ly) / GRAT_LINES) {
        printWriter.println(MessageFormat.format("{0} {1} m", prjX[i * ly], prjY[i * ly]));
        for (int j = 1; j < ly; j++) {
          printWriter.println(MessageFormat.format("{0} {1} l", prjX[i * ly + j], prjY[i * ly + j]));
        }
        printWriter.println("s");
      }
    }

    printWriter.println("showpage");
    printWriter.flush();
  }

  private void drawRing(PrintWriter printWriter, ResultRegion resultRegion, double[] pointsX, double[] pointsY, String color) {
    printWriter.println("n");
    printWriter.println(MessageFormat.format("{0} {1} m", pointsX[0], pointsY[0]));
    for (int i = 1; i < pointsX.length; i++) {
      printWriter.println(MessageFormat.format("{0} {1} l", pointsX[i], pointsY[i]));
    }
    printWriter.println("c");
    if (!resultRegion.isNaN()) {
      printWriter.println("gsave\n" + color + " SRGB f\ngrestore\n0 SGRY s");
    } else {
      printWriter.println("gsave\n0.75 SGRY f\ngrestore\n0 SGRY s");
    }
  }
}
