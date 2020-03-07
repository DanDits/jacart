package de.dandit.cartogram.geo.convert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import de.dandit.cartogram.core.api.ConvergenceGoalFailedException;
import de.dandit.cartogram.core.api.Region;
import de.dandit.cartogram.core.api.CartogramApi;
import de.dandit.cartogram.core.api.CartogramConfig;
import de.dandit.cartogram.core.api.CartogramResult;
import de.dandit.cartogram.core.api.MapFeatureData;
import de.dandit.cartogram.core.api.ResultRegion;
import de.dandit.cartogram.geo.data.CsvData;
import de.dandit.cartogram.geo.data.CsvDataImport;
import de.dandit.cartogram.geo.data.GeoJsonIO;

public class ExecuteCartogram {

  public static void createCartogramToEps(CartogramConfig config, InputStream geoJsonResource, InputStream dataResource,
                                          OutputStream epsOut) throws IOException, ConvergenceGoalFailedException {
    FeatureConverter featureConverter = new FeatureConverter(new GeometryConverter(new GeometryFactory()));
    CartogramResult result = createMapFeatureData(config, featureConverter, geoJsonResource, dataResource);
    new EpsWriter().createFigure(
      epsOut,
      result.getGridSizeX(),
      result.getGridSizeY(),
      result.getResultRegions(),
      result.getGridProjectionX(),
      result.getGridProjectionY(),
      true);
  }

  public static void createCartogramToGeoJson(CartogramConfig config, InputStream geoJsonResource, InputStream dataResource,
                                          OutputStream jsonOut) throws IOException, ConvergenceGoalFailedException {
    FeatureConverter featureConverter = new FeatureConverter(new GeometryConverter(new GeometryFactory()));
    CartogramResult result = createMapFeatureData(config, featureConverter, geoJsonResource, dataResource);
    outputRegionsToFile(featureConverter, result.getResultRegions(), jsonOut);
  }

  private static Double extractData(CsvData data, int regionIdColumnIndex, int regionDataColumnIndex, Integer id) {
    for (int i = 0; i < data.getData().size(); i++) {
      Object[] csvValues = data.getData().get(i);
      if (csvValues[regionIdColumnIndex].equals(id)) {
        if (csvValues[regionDataColumnIndex] == null) {
          return Double.NaN;
        }
        return (Double) csvValues[regionDataColumnIndex];
      }
    }
    throw new IllegalStateException("Did not find value for region " + id);
  }

  private static CartogramResult createMapFeatureData(CartogramConfig config, FeatureConverter featureConverter, InputStream geoJsonResource, InputStream dataResource) throws IOException, ConvergenceGoalFailedException {
    FeatureCollection<SimpleFeatureType, SimpleFeature> geo = new GeoJsonIO().importData(geoJsonResource);
    CsvData data = new CsvDataImport().importCsv(dataResource);
    ReferencedEnvelope bounds = geo.getBounds();
    int regionIdColumnIndex = data.getNames().indexOf("Region.Id");
    int regionDataColumnIndex = data.getNames().indexOf("Region.Data");
    List<Region> regions = featureConverter.createRegions(asIterable(geo),
      ExecuteCartogram::extractFeatureId,
      id -> extractData(data, regionIdColumnIndex, regionDataColumnIndex, id));
    double[] targetAreaPerRegion = new double[regions.size()];
    for (int i = 0; i < regions.size(); i++) {
      Region region = regions.get(i);
      targetAreaPerRegion[i] = region.getData();
    }
    MapFeatureData mapFeatureData = new MapFeatureData(
      bounds.getMinX(),
      bounds.getMinY(),
      bounds.getMaxX(),
      bounds.getMaxY(),
      regions,
      targetAreaPerRegion);
    return new CartogramApi().calculateGaSeMo(mapFeatureData, config);
  }

  private static int extractFeatureId(SimpleFeature feature) {
    Object value = feature.getProperties("cartogram_id").iterator().next().getValue();
    return Integer.parseInt(value.toString());
  }

  private static Iterable<? extends SimpleFeature> asIterable(FeatureCollection<SimpleFeatureType, SimpleFeature> geo) {
    return (Iterable<SimpleFeature>) () -> {
      FeatureIterator<SimpleFeature> featureIterator = geo.features();
      return new Iterator<>() {
        @Override
        public boolean hasNext() {
          return featureIterator.hasNext();
        }

        @Override
        public SimpleFeature next() {
          return featureIterator.next();
        }
      };
    };
  }

  private static void outputRegionsToFile(FeatureConverter featureConverter, List<ResultRegion> polygons, OutputStream jsonOut) throws IOException {
    DefaultFeatureCollection resultAsGeo = featureConverter.convertToFeatureCollection(polygons);
    new GeoJsonIO().exportData(
      resultAsGeo,
      jsonOut);
  }
}
