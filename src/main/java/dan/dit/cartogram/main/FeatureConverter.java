package dan.dit.cartogram.main;

import dan.dit.cartogram.core.context.Region;
import dan.dit.cartogram.core.pub.ResultPolygon;
import dan.dit.cartogram.core.pub.ResultRegion;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
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
    for (ResultRegion region : regions) {
      SimpleFeature feature = createFeature(id, region.getPolygons());
      resultAsGeo.add(feature);
      id++;
    }
    return resultAsGeo;
  }

  private SimpleFeature createFeature(int id, List<ResultPolygon> resultPolygons) {
    Geometry geometry = geometryConverter.createGeometry(resultPolygons);
    SimpleFeatureType simpleFeatureType = createSimpleFeatureType();
    return new SimpleFeatureBuilder(simpleFeatureType)
      .buildFeature(Integer.toString(id), new Object[]{geometry});
  }

  private SimpleFeatureType createSimpleFeatureType() {
    SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
    b.add("geo", Polygon.class);
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
