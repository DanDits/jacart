package de.dandit.cartogram.geo.convert;

import de.dandit.cartogram.core.context.Point;
import de.dandit.cartogram.core.context.Region;
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

  public Polygon asPolygon(List<Point> points, List<List<Point>> holes) {
    LinearRing outerRing = asRing(points);
    LinearRing[] jtsHoles = new LinearRing[holes.size()];
    for (int i = 0; i < holes.size(); i++) {
      jtsHoles[i] = asRing(holes.get(i));
    }
    return geometryFactory.createPolygon(outerRing, jtsHoles);
  }

  private LinearRing asRing(List<Point> points) {
    Coordinate[] coords = new Coordinate[points.size()];
    for (int i = 0; i < points.size(); i++) {
      Point p = points.get(i);
      coords[i] = new Coordinate(p.x, p.y);
    }
    return geometryFactory.createLinearRing(coords);
  }

  public Region createFromPolygon(Function<Integer, Double> valueProvider, int regionId, Polygon polygon) {
    polygon.normalize();
    Point[][] points = new Point[1 + polygon.getNumInteriorRing()][];
    points[0] = convertCoordinates(polygon.getExteriorRing());
    List<Integer> ringsInPolygons = new ArrayList<>();
    ringsInPolygons.add(-1);
    for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
      points[1 + j] = convertCoordinates(polygon.getInteriorRingN(j));
      ringsInPolygons.add(0);
    }
    return new Region(regionId,
      valueProvider.apply(regionId),
      points,
      ringsInPolygons.stream().mapToInt(a -> a).toArray());
  }

  public Region createFromMultiPolygon(Function<Integer, Double> valueProvider, int regionId, MultiPolygon multiPolygon) {
    multiPolygon.normalize();
    List<Point[]> points = new ArrayList<>();
    List<Integer> ringsInPolygons = new ArrayList<>();
    for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
      Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
      points.add(convertCoordinates(polygon.getExteriorRing()));
      ringsInPolygons.add(-i - 1);
      for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
        points.add(convertCoordinates(polygon.getInteriorRingN(j)));
        ringsInPolygons.add(i);
      }
    }
    return new Region(regionId,
      valueProvider.apply(regionId),
      points.toArray(Point[][]::new),
      ringsInPolygons.stream().mapToInt(a -> a).toArray());
  }

  private static Point[] convertCoordinates(LineString lineString) {
    return Arrays.stream(lineString.getCoordinates())
      .map(coord -> new Point(coord.x, coord.y))
      .toArray(Point[]::new);
  }

  public Geometry createGeometry(List<ResultPolygon> resultPolygons) {
    Geometry geometry;
    if (resultPolygons.size() == 0) {
      geometry = geometryFactory.createPolygon();
    } else if (resultPolygons.size() == 1) {
      ResultPolygon polygon = resultPolygons.get(0);
      geometry = asPolygon(polygon.getExteriorRing(), polygon.getInteriorRings());
    } else {
      Polygon[] polygons = new Polygon[resultPolygons.size()];
      for (int i = 0; i < resultPolygons.size(); i++) {
        ResultPolygon p = resultPolygons.get(i);
        Polygon polygon = asPolygon(p.getExteriorRing(), p.getInteriorRings());
        polygons[i] = polygon;
      }
      geometry = geometryFactory.createMultiPolygon(polygons);
    }
    return geometry;
  }
}
