package de.dandit.cartogram.geo.convert;

import de.dandit.cartogram.core.pub.Region;
import de.dandit.cartogram.core.pub.ResultPolygon;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class GeometryConverter {
  private final GeometryFactory geometryFactory;

  public GeometryConverter(GeometryFactory geometryFactory) {
    this.geometryFactory = Objects.requireNonNull(geometryFactory);
  }

  public Polygon asPolygon(double[] pointsX, double[] pointsY, List<double[]> holesX, List<double[]> holesY) {
    LinearRing outerRing = asRing(pointsX, pointsY);
    LinearRing[] jtsHoles = new LinearRing[holesX.size()];
    for (int i = 0; i < holesX.size(); i++) {
      jtsHoles[i] = asRing(holesX.get(i), holesY.get(i));
    }
    return geometryFactory.createPolygon(outerRing, jtsHoles);
  }

  private LinearRing asRing(double[] pointsX, double[] pointsY) {
    Coordinate[] coords = new Coordinate[pointsX.length];
    for (int i = 0; i < coords.length; i++) {
      coords[i] = new Coordinate(pointsX[i], pointsY[i]);
    }
    return geometryFactory.createLinearRing(coords);
  }

  public Region createFromPolygon(Function<Integer, Double> valueProvider, int regionId, Polygon polygon) {
    polygon.normalize();
    double[][] pointsX = new double[1 + polygon.getNumInteriorRing()][];
    double[][] pointsY = new double[1 + polygon.getNumInteriorRing()][];
    pointsX[0] = convertCoordinatesX(polygon.getExteriorRing());
    pointsY[0] = convertCoordinatesY(polygon.getExteriorRing());
    List<Integer> ringsInPolygons = new ArrayList<>();
    ringsInPolygons.add(-1);
    for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
      pointsX[1 + j] = convertCoordinatesX(polygon.getInteriorRingN(j));
      pointsY[1 + j] = convertCoordinatesY(polygon.getInteriorRingN(j));
      ringsInPolygons.add(0);
    }
    return new Region(regionId,
      valueProvider.apply(regionId),
      pointsX,
      pointsY,
      ringsInPolygons.stream().mapToInt(a -> a).toArray());
  }

  public Region createFromMultiPolygon(Function<Integer, Double> valueProvider, int regionId, MultiPolygon multiPolygon) {
    multiPolygon.normalize();
    List<double[]> ringsX = new ArrayList<>();
    List<double[]> ringsY = new ArrayList<>();
    List<Integer> ringsInPolygons = new ArrayList<>();
    for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
      Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
      ringsX.add(convertCoordinatesX(polygon.getExteriorRing()));
      ringsY.add(convertCoordinatesY(polygon.getExteriorRing()));
      ringsInPolygons.add(-i - 1);
      for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
        ringsX.add(convertCoordinatesX(polygon.getInteriorRingN(j)));
        ringsY.add(convertCoordinatesY(polygon.getInteriorRingN(j)));
        ringsInPolygons.add(i);
      }
    }
    return new Region(regionId,
      valueProvider.apply(regionId),
      ringsX.toArray(double[][]::new),
      ringsY.toArray(double[][]::new),
      ringsInPolygons.stream().mapToInt(a -> a).toArray());
  }

  private static double[] convertCoordinatesX(LineString lineString) {
    return Arrays.stream(lineString.getCoordinates())
        .mapToDouble(coord -> coord.x)
        .toArray();
  }

  private static double[] convertCoordinatesY(LineString lineString) {
    return Arrays.stream(lineString.getCoordinates())
        .mapToDouble(coord -> coord.y)
        .toArray();
  }

  public Geometry createGeometry(List<ResultPolygon> resultPolygons) {
    Geometry geometry;
    if (resultPolygons.size() == 0) {
      geometry = geometryFactory.createPolygon();
    } else if (resultPolygons.size() == 1) {
      ResultPolygon polygon = resultPolygons.get(0);
      geometry = asPolygon(polygon.getExteriorRingX(), polygon.getExteriorRingY(), polygon.getInteriorRingsX(), polygon.getInteriorRingsY());
    } else {
      Polygon[] polygons = new Polygon[resultPolygons.size()];
      for (int i = 0; i < resultPolygons.size(); i++) {
        ResultPolygon p = resultPolygons.get(i);
        Polygon polygon = asPolygon(p.getExteriorRingX(), p.getExteriorRingY(), p.getInteriorRingsX(), p.getInteriorRingsY());
        polygons[i] = polygon;
      }
      geometry = geometryFactory.createMultiPolygon(polygons);
    }
    return geometry;
  }
}
