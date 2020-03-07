package de.dandit.cartogram.geo.convert;

import de.dandit.cartogram.core.api.Region;
import de.dandit.cartogram.core.api.ResultPolygon;
import de.dandit.cartogram.core.api.ResultRegion;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class FeatureConverter {
  private final GeometryConverter geometryConverter;

  public FeatureConverter(GeometryConverter geometryConverter) {
    this.geometryConverter = geometryConverter;
  }

  public DefaultFeatureCollection convertToFeatureCollection(List<ResultRegion> regions) {
    DefaultFeatureCollection resultAsGeo = new DefaultFeatureCollection();
    int id = 0;
    boolean isMultiPolygon = regions.stream().anyMatch(region -> region.getPolygons().size() > 1);
    for (ResultRegion region : regions) {
      SimpleFeature feature = createFeature(id, region.getPolygons(), isMultiPolygon);
      resultAsGeo.add(feature);
      id++;
    }
    return resultAsGeo;
  }

  private SimpleFeature createFeature(int id, List<ResultPolygon> resultPolygons, boolean isMultiPolygon) {
    Geometry geometry = geometryConverter.createGeometry(resultPolygons);
    SimpleFeatureType simpleFeatureType = createSimpleFeatureType(isMultiPolygon);
    return new SimpleFeatureBuilder(simpleFeatureType)
      .buildFeature(Integer.toString(id), new Object[]{geometry});
  }

  private SimpleFeatureType createSimpleFeatureType(boolean isMultiPolygon) {
    SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
    b.add("geo", isMultiPolygon ? MultiPolygon.class : Polygon.class);
    b.setName("CartRegion");
    return b.buildFeatureType();
  }

  public List<Region> createRegions(Iterable<? extends SimpleFeature> features,
                                           ToIntFunction<SimpleFeature> regionIdExtractor,
                                            Function<Integer, Double> valueProvider) {
    Iterator<? extends SimpleFeature> iterator = features.iterator();
    List<Region> regions = new ArrayList<>();
    while (iterator.hasNext()) {
      SimpleFeature feature = iterator.next();
      int regionId = regionIdExtractor.applyAsInt(feature);
      Object geometry = feature.getDefaultGeometry();
      if (geometry instanceof Polygon) {
        Region region = geometryConverter.createFromPolygon(valueProvider, regionId, (Polygon) geometry);
        regions.add(region);
      } else if (geometry instanceof MultiPolygon) {
        Region region = geometryConverter.createFromMultiPolygon(valueProvider, regionId, (MultiPolygon) geometry);
        regions.add(region);
      }
    }
    return regions;
  }
}
