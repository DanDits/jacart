package de.dandit.cartogram.geo.convert;

import de.dandit.cartogram.core.api.Region;
import de.dandit.cartogram.core.api.LightPolygon;
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

  public Polygon asPolygon(double[] pointsX, double[] pointsY, List<double[]> interiorRingsX, List<double[]> interiorRingsY) {
    LinearRing outerRing = asRing(pointsX, pointsY);
    LinearRing[] interiorRings = new LinearRing[interiorRingsX.size()];
    for (int i = 0; i < interiorRingsX.size(); i++) {
      interiorRings[i] = asRing(interiorRingsX.get(i), interiorRingsY.get(i));
    }
    return geometryFactory.createPolygon(outerRing, interiorRings);
  }

  private LinearRing asRing(double[] pointsX, double[] pointsY) {
    Coordinate[] coords = new Coordinate[pointsX.length];
    for (int i = 0; i < coords.length; i++) {
      coords[i] = new Coordinate(pointsX[i], pointsY[i]);
    }
    return geometryFactory.createLinearRing(coords);
  }

  public Region createFromPolygon(Function<Integer, Double> valueProvider, int regionId, Polygon polygon) {
    return new Region(
      regionId,
      valueProvider.apply(regionId),
      List.of(asLightPolygon(polygon)));
  }

  public Region createFromMultiPolygon(Function<Integer, Double> valueProvider, int regionId, MultiPolygon multiPolygon) {
    multiPolygon.normalize();
    List<LightPolygon> polygons = new ArrayList<>();
    for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
      polygons.add(asLightPolygon((Polygon) multiPolygon.getGeometryN(i)));
    }
    return new Region(regionId,
      valueProvider.apply(regionId),
      polygons);
  }

  private LightPolygon asLightPolygon(Polygon polygon) {
    polygon.normalize();
    List<double[]> interiorRingsX = new ArrayList<>(polygon.getNumInteriorRing());
    List<double[]> interiorRingsY = new ArrayList<>(polygon.getNumInteriorRing());
    double[] exteriorRingX  = convertCoordinatesX(polygon.getExteriorRing());
    double[] exteriorRingY = convertCoordinatesY(polygon.getExteriorRing());
    for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
      interiorRingsX.add(convertCoordinatesX(polygon.getInteriorRingN(j)));
      interiorRingsY.add(convertCoordinatesY(polygon.getInteriorRingN(j)));
    }
    return new LightPolygon(exteriorRingX, exteriorRingY, interiorRingsX, interiorRingsY);
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

  public Geometry createGeometry(List<LightPolygon> resultPolygons) {
    Geometry geometry;
    if (resultPolygons.size() == 0) {
      geometry = geometryFactory.createPolygon();
    } else if (resultPolygons.size() == 1) {
      LightPolygon polygon = resultPolygons.get(0);
      geometry = asPolygon(polygon.getExteriorRingX(), polygon.getExteriorRingY(), polygon.getInteriorRingsX(), polygon.getInteriorRingsY());
    } else {
      Polygon[] polygons = new Polygon[resultPolygons.size()];
      for (int i = 0; i < resultPolygons.size(); i++) {
        LightPolygon p = resultPolygons.get(i);
        Polygon polygon = asPolygon(p.getExteriorRingX(), p.getExteriorRingY(), p.getInteriorRingsX(), p.getInteriorRingsY());
        polygons[i] = polygon;
      }
      geometry = geometryFactory.createMultiPolygon(polygons);
    }
    return geometry;
  }
}
