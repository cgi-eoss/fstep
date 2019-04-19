package com.cgi.eoss.fstep.catalogue.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.UnknownFormat;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.Hints;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.text.WKTParser;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.geometry.coordinate.LineString;
import org.opengis.geometry.coordinate.PointArray;
import org.opengis.geometry.coordinate.Position;
import org.opengis.geometry.primitive.Curve;
import org.opengis.geometry.primitive.Point;
import org.opengis.geometry.primitive.Surface;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTReader;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

/**
 * <p>Utility methods for dealing with geo-spatial data and its various java libraries.</p>
 */
@Log4j2
@UtilityClass
public class GeoUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private static final WKTReader WKT_READER = new WKTReader(new GeometryFactory(new PrecisionModel(),4326));

    private static final GeometryBuilder GEOMETRY_BUILDER = new GeometryBuilder(DefaultGeographicCRS.WGS84);

    private static final WKTParser WKT_PARSER = new WKTParser(GEOMETRY_BUILDER);

    
    private static final String DEFAULT_POINT = "POINT(0 0)";

    public static GeoJsonObject defaultGeometry() {
        return wktToGeojson(DEFAULT_POINT);
    }

    public static GeoJsonObject getGeoJsonGeometry(String geometry) throws GeometryException{
        return GeoUtil.wktToGeojson(geometry);
    }
    
    public static GeoJsonObject wktToGeojson(String wkt) {
        try {
        	Geometry geom = WKT_READER.read(wkt);
        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        	new GeometryJSON(16).write(geom, baos);
        	return OBJECT_MAPPER.readValue(baos.toByteArray(), GeoJsonObject.class);
        } catch (Exception e) {
            LOG.error("Could not convert WKT to GeoJson Polygon: {}", wkt, e);
            throw new GeometryException(e);
        }
    }

    @Deprecated
    public static org.geojson.Polygon wktToGeojsonPolygon(String wkt) {
        try {
            Surface surface = (Surface) WKT_PARSER.parse(wkt);
            Curve curve = (Curve) Iterables.getOnlyElement(surface.getBoundary().getExterior().getElements());
            LineString lineString = (LineString) Iterables.getOnlyElement(curve.getSegments());
            PointArray controlPoints = lineString.getControlPoints();

            List<LngLatAlt> geojsonCoords = controlPoints.stream()
                    .map(Position::getDirectPosition)
                    .map(p -> new LngLatAlt(p.getOrdinate(0), p.getOrdinate(1)))
                    .collect(Collectors.toList());

            return new org.geojson.Polygon(geojsonCoords);
        } catch (Exception e) {
            LOG.error("Could not convert WKT to GeoJson Polygon: {}", wkt, e);
            throw new GeometryException(e);
        }
    }

    @Deprecated
    public static org.geojson.Point wktToGeojsonPoint(String wkt) {
        try {
            Point point = (Point) WKT_PARSER.parse(wkt);
            return new org.geojson.Point(
                    point.getDirectPosition().getOrdinate(0),
                    point.getDirectPosition().getOrdinate(1));
        } catch (Exception e) {
            LOG.error("Could not convert WKT to GeoJson Point: {}", wkt, e);
            throw new GeometryException(e);
        }
    }

    public static String geojsonToString(GeoJsonObject object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LOG.error("Could not serialise GeoJsonObject: {}", object, e);
            throw new GeometryException(e);
        }
    }

    public static String geojsonToWkt(GeoJsonObject geojson) {
        try {
            return new GeometryJSON().read(geojsonToString(geojson)).toString();
        } catch (Exception e) {
            LOG.error("Could not convert GeoJsonObject to WKT: {}", geojson, e);
            throw new GeometryException(e);
        }
    }

    public static GeoJsonObject stringToGeojson(String geojson) {
        try {
            return OBJECT_MAPPER.readValue(geojson, GeoJsonObject.class);
        } catch (Exception e) {
            LOG.error("Could not deserialise GeoJsonObject: {}", geojson, e);
            throw new GeometryException(e);
        }
    }
    
    public static org.geojson.Polygon extractBoundingBox(Path file) {
    	if (isZip(file)) {
			try {
				Path tmpFolder = Files.createTempDirectory("shape");
				Path temp = unzipInTempFolder(file, tmpFolder); 
	    		org.geojson.Polygon polygon = internal_extractBoundingBox(temp);
	    		FileUtils.deleteDirectory(temp.toFile());
	    		return polygon;
			} catch (IOException e) {
				 LOG.error("Could not extract bounding box from file: {}", file, e);
		            throw new GeometryException(e);
			}
    	}
    	else {
    		return internal_extractBoundingBox(file);
    	}
    }

    private static boolean isZip(Path file) {
		return FilenameUtils.getExtension(file.toString()).equalsIgnoreCase("ZIP");
	}

	private static org.geojson.Polygon internal_extractBoundingBox(Path file) {
    	try {
            ReferencedEnvelope envelope = getEnvelope(file);

            Polygon polygon = JTS.toGeometry(envelope.toBounds(DefaultGeographicCRS.WGS84));
            LOG.debug("Extracted WKT bounding box from file {}: {}", file.getFileName(), polygon);
            return (org.geojson.Polygon) wktToGeojson(polygon.toString());
        } catch (Exception e) {
            throw new GeometryException(e);
        }
    }
    
    private Path unzipInTempFolder(Path zipFilePath, Path folder) throws IOException {
    	byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath.toFile()));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(folder.toFile(), zipEntry);
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        return folder;
	}
    
    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
         
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
         
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
         
        return destFile;
    }

    public static String extractEpsg(Path file) {
        try {
            CoordinateReferenceSystem crs = getCrs(file);

            Integer epsgCode = CRS.lookupEpsgCode(crs, true);
            String epsg = "EPSG:" + epsgCode;

            LOG.debug("Extracted EPSG from file {}: {}", file.getFileName(), epsg);
            return epsg;
        } catch (Exception e) {
            throw new GeometryException(e);
        }
    }

    private static ReferencedEnvelope getEnvelope(Path file) throws IOException {
        // General metadata
        DataStore dataStore = DataStoreFinder.getDataStore(ImmutableMap.of("url", file.toUri().toURL()));
        if (dataStore != null) {
            SimpleFeatureCollection featureCollection = dataStore.getFeatureSource(dataStore.getTypeNames()[0]).getFeatures(Filter.INCLUDE);
            return featureCollection.getBounds();
        }

        // Raster data
        AbstractGridFormat gridFormat = GridFormatFinder.findFormat(file.toUri().toURL());
        if (gridFormat != null && !(gridFormat instanceof UnknownFormat)) {
            Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            AbstractGridCoverage2DReader reader = gridFormat.getReader(file.toUri().toURL(), hints);
            return ReferencedEnvelope.reference(reader.getOriginalEnvelope());
        }

        throw new UnsupportedOperationException("Could not extract geometry envelope from " + file.toString());
    }
    
    public static GeoJsonObject shapeFileToGeojson(Path shapeFile, int threshold) {
    	if (isZip(shapeFile)) {
    		Path tmpFolder = null; 
    		Path temp; 
			try {
				tmpFolder = Files.createTempDirectory("shape");
	    		temp = unzipInTempFolder(shapeFile, tmpFolder); 
				Optional<Path> shapeFileInFolder = Files.find(temp, 5, (path, attr) -> FilenameUtils.getExtension(path.getFileName().toString()).equalsIgnoreCase("shp")).findFirst();
	    		if (shapeFileInFolder.isPresent()) {
				return internal_shapeFileToGeojson(shapeFileInFolder.get(), threshold);
	    		}
	    		else {
	    			throw new IOException();
	    		}
	    	} catch (IOException e) {
	    		 String errorMessage = "Could not extract geojson from file: " + shapeFile;
				 LOG.error(errorMessage, e);
		         throw new GeometryException(errorMessage, e);
			}
			finally{
				try {
					if (tmpFolder != null) {
						FileUtils.deleteDirectory(tmpFolder.toFile());
					}
				} catch (IOException e) {
					
			 }
			}
    	}
    	else {
    		return internal_shapeFileToGeojson(shapeFile, threshold);
    	}
    }

	private static GeoJsonObject internal_shapeFileToGeojson(Path shapeFile, int threshold) {
		ShapefileDataStore store = null;
		try {
    		store = new ShapefileDataStore(shapeFile.toUri().toURL());
        	SimpleFeatureSource source = store.getFeatureSource();
        	SimpleFeatureType schema = source.getSchema();
        	CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
        	Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            CRSAuthorityFactory factory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", hints);
            CoordinateReferenceSystem targetCRS = factory.createCoordinateReferenceSystem("EPSG:4326");
        	MathTransform transform = CRS.findMathTransform(crs, targetCRS);
    		SimpleFeatureCollection featureCollection = source.getFeatures();
        	SimpleFeatureIterator features = featureCollection.features();
        	ArrayList<Geometry> geometries = new ArrayList<>();
        	int pointCount = 0;
        	String basicGeometryType = null;
        	while (features.hasNext()) {
        		SimpleFeature feature = features.next();
        		Geometry featureGeometrySource = (Geometry) feature.getDefaultGeometry();
        		Geometry featureGeometry = JTS.transform(featureGeometrySource, transform);
        		String featureGeometryType = featureGeometry.getGeometryType();
        		String featureBasicGeometryType = getBasicGeometryType(featureGeometryType);
        		if (basicGeometryType != null && !basicGeometryType.equals(featureBasicGeometryType)) {
        			throw new Exception ("Unsupported heterogeneous feature geometries found in shapefile: " + basicGeometryType  + " - " + featureBasicGeometryType);
        		}
        		basicGeometryType = featureBasicGeometryType;
        		if (featureGeometryType.equals("Polygon")) {
        			Polygon p = (Polygon) featureGeometry;
        			pointCount+=p.getCoordinates().length;
        			geometries.add(p);
        		}
        		else if (featureGeometryType.equals("MultiPolygon")) {
        			MultiPolygon multipolygon = (MultiPolygon) featureGeometry;
	        		for (int i = 0; i < multipolygon.getNumGeometries(); i++) {
	        			Polygon polygon = (Polygon) multipolygon.getGeometryN(i);
	        			pointCount+=polygon.getCoordinates().length;
	        			geometries.add(polygon);
	        		}
        		}
        		else if (featureGeometryType.equals("Point")) {
	        		com.vividsolutions.jts.geom.Point point = (com.vividsolutions.jts.geom.Point) featureGeometry;
	        		pointCount++;
        			geometries.add(point);
        		}
        		else if (featureGeometryType.equals("MultiPoint")) {
        			MultiPoint multiPoint = (MultiPoint) featureGeometry;
	        		for (int i = 0; i < multiPoint.getNumGeometries(); i++) {
	        			com.vividsolutions.jts.geom.Point point = (com.vividsolutions.jts.geom.Point) multiPoint.getGeometryN(i);
	        			pointCount++;
	        			geometries.add(point);
	        		}
        		}
        		else {
        			throw new Exception("Unsupported geometry found in shapefile:" + featureGeometryType);
        		}
        	}
        	features.close();
        	if (pointCount > threshold) {
        		throw new Exception("Geometry contains more points - " + pointCount +  " - than allowed threshold " + threshold );
        	}
        	GeometryFactory gf = new GeometryFactory();
        	Geometry combinedGeometry = null;
        	if (basicGeometryType.equals("Polygon")) {
        		combinedGeometry = gf.createMultiPolygon(geometries.toArray(new Polygon[] {}));
        		combinedGeometry.setSRID(geometries.get(0).getSRID());
        	}
        	else if (basicGeometryType.equals("Point")) {
        		combinedGeometry = gf.createMultiPoint(geometries.toArray(new com.vividsolutions.jts.geom.Point[] {}));
        		combinedGeometry.setSRID(geometries.get(0).getSRID());
        	}
        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        	new GeometryJSON(16).write(combinedGeometry, baos);
        	return OBJECT_MAPPER.readValue(baos.toByteArray(), GeoJsonObject.class);
    	}
    	catch(Exception e) {
    		LOG.error("Could not extract geometry from " + shapeFile.toString());
    		throw new GeometryException(e.getMessage(), e);
    	}
		finally
		{
    		store.dispose();
    	}
	}
	

    private static String getBasicGeometryType(String geometryType) {
		if(geometryType.equals("Polygon") || geometryType.equals("MultiPolygon")){
			return "Polygon";
		}
		
		if(geometryType.equals("Point") || geometryType.equals("MultiPoint")){
			return "Point";
		}
		return null;
	}

	private static CoordinateReferenceSystem getCrs(Path file) throws IOException {
        // General metadata
        DataStore dataStore = DataStoreFinder.getDataStore(ImmutableMap.of("url", file.toUri().toURL()));
        if (dataStore != null) {
            SimpleFeatureCollection featureCollection = dataStore.getFeatureSource(dataStore.getTypeNames()[0]).getFeatures(Filter.INCLUDE);
            return featureCollection.getSchema().getCoordinateReferenceSystem();
        }

        // Raster data
        AbstractGridFormat gridFormat = GridFormatFinder.findFormat(file.toUri().toURL());
        if (gridFormat != null && !(gridFormat instanceof UnknownFormat)) {
            Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            AbstractGridCoverage2DReader reader = gridFormat.getReader(file.toUri().toURL(), hints);
            return reader.getCoordinateReferenceSystem();
        }

        throw new UnsupportedOperationException("Could not extract CRS from " + file.toString());
    }
	
	public static Path duplicateShapeFile(Path shapeFile, String newName, Map<String, Object> newAttributes, boolean zipResult) throws IOException {
	    Path targetFolder = Files.createTempDirectory("target");
	    Path targetShapeFilePath = targetFolder.resolve(newName + ".shp");
	    if (isZip(shapeFile)) {
	        Path tmpFolder = Files.createTempDirectory("shape");
	        Path temp = unzipInTempFolder(shapeFile, tmpFolder);
	        try (Stream<Path> shapeFileInFolders = Files.find(temp, 5, (path, attr) -> FilenameUtils.getExtension(path.getFileName().toString()).equalsIgnoreCase("shp"))){
		        Optional<Path> shapeFileInFolder = shapeFileInFolders.findFirst();
		        if (shapeFileInFolder.isPresent()) {
		            duplicateShapeFile(shapeFileInFolder.get(), targetShapeFilePath, newAttributes);
		        }
		        else {
		            throw new IOException("Shapefile not found");
		        }
	        }
        }
	    else {
	        duplicateShapeFile(shapeFile, targetShapeFilePath, newAttributes);
	    }
       
	    if (zipResult) {
	        File[] files = targetShapeFilePath.getParent().toFile().listFiles();
	        File zipFile = new File(targetShapeFilePath.getParent().toFile(), targetFolder.getFileName() + ".zip");
            try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
	        	for (final File f : files) {
	                try {
	                    ZipEntry zipEntry = new ZipEntry(f.getName());
	                    zos.putNextEntry(zipEntry);
	                    byte[] bytes = new byte[1024];
	                    int length;
	                    try (FileInputStream fis = new FileInputStream(f)) {
		                    while ((length = fis.read(bytes)) >= 0) {
		                        zos.write(bytes, 0, length);
		                    }
		                    zos.closeEntry();
	                    }
	                } catch (Exception e) {
	                	throw new IOException("Shapefile could not be unzipped");
	                }
	            }
            }
            return zipFile.toPath();
	    }
	    else {
	        return targetShapeFilePath;
	    }
    }
	
	private static void duplicateShapeFile(Path shapeFile, Path newShapeFile, Map<String, Object> newAttributes) throws IOException {
        ShapefileDataStore sourceStore = new ShapefileDataStore(shapeFile.toUri().toURL());
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", newShapeFile.toUri().toURL());
        params.put("create spatial index", Boolean.FALSE);
        ShapefileDataStore targetStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        targetStore.setFidIndexed(false);
        //FeatureStore featureStore = (FeatureStore) targetStore.getFeatureSource();
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.init(sourceStore.getSchema());
        for (Entry<String, Object> newAttribute: newAttributes.entrySet()) {
            if (newAttribute.getValue() instanceof UUID) {
                AttributeTypeBuilder attributeBuilder = new AttributeTypeBuilder();
                attributeBuilder.setNillable(true);
                attributeBuilder.setBinding(String.class);
                attributeBuilder.setLength(36);
                builder.add(attributeBuilder.buildDescriptor( newAttribute.getKey()));
            }
            else {
                builder.add(newAttribute.getKey(), newAttribute.getValue().getClass());
            }
        }
        SimpleFeatureType targetFeatureType = builder.buildFeatureType();
        targetStore.createSchema(targetFeatureType);
        SimpleFeatureIterator sourceFeatures = sourceStore.getFeatureSource().getFeatures().features();
        DefaultFeatureCollection targetCollection = new DefaultFeatureCollection(null,null);
        while (sourceFeatures.hasNext()) {
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(targetFeatureType);
            featureBuilder.init(sourceFeatures.next());
            for (Entry<String, Object> newAttribute: newAttributes.entrySet()) {
                featureBuilder.set(newAttribute.getKey(), newAttribute.getValue());
            }
            targetCollection.add(featureBuilder.buildFeature(null));
        }
        sourceFeatures.close();
        sourceStore.dispose();
        String typeName = targetStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = targetStore.getFeatureSource(typeName);
        if (featureSource instanceof SimpleFeatureStore) {
            Transaction transaction = new DefaultTransaction("create");
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(targetCollection);
                transaction.commit();

            } catch (Exception problem) {
                transaction.rollback();
                throw new IOException();

            } finally {
                transaction.close();
            }
        } else {
            throw new IOException(typeName + " does not support read/write access");
        }
    }
}
