package dan.dit.cartogram.data;

import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * For visualization, simplification and detecting of self intersections of GeoJson FeatureCollections
 * https://mapshaper.org/ is a great tool!
 */
public class GeoJsonIO {

  public FeatureCollection<SimpleFeatureType, SimpleFeature> importData(InputStream resource) throws IOException {
    FeatureJSON featureJSON = new FeatureJSON();
    FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = featureJSON.readFeatureCollection(resource);
    return featureCollection;
  }

  public void reWriteDataInIdOrder(InputStream resource, OutputStream outputResource) throws IOException {
    FeatureJSON featureJSON = new FeatureJSON();
    FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = featureJSON.readFeatureCollection(resource);
    featureJSON.writeFeatureCollection(featureCollection, outputResource);
  }

  public void exportData(FeatureCollection<SimpleFeatureType, SimpleFeature> features, OutputStream resource) throws IOException {
    FeatureJSON featureJSON = new FeatureJSON();
    featureJSON.writeFeatureCollection(features, resource);
  }
}
