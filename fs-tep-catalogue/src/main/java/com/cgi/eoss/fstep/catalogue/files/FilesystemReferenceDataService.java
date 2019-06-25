package com.cgi.eoss.fstep.catalogue.files;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.MultiPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.cgi.eoss.fstep.catalogue.CatalogueUri;
import com.cgi.eoss.fstep.catalogue.geoserver.GeoServerSpec;
import com.cgi.eoss.fstep.catalogue.geoserver.GeoServerType;
import com.cgi.eoss.fstep.catalogue.geoserver.GeoserverService;
import com.cgi.eoss.fstep.catalogue.resto.RestoService;
import com.cgi.eoss.fstep.catalogue.util.GeoUtil;
import com.cgi.eoss.fstep.catalogue.util.GeometryException;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.GeoserverLayer;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.internal.FstepFileIngestion;
import com.cgi.eoss.fstep.model.internal.UploadableFileType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import com.google.common.io.MoreFiles;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class FilesystemReferenceDataService implements ReferenceDataService {
	private static final String DEFAULT_REFDATA_GEOSERVER_WORKSPACE = "fs-reference";
	private static final int GEOMETRY_BOUNDING_BOX_THRESHOLD = 10000;
	private final Path referenceDataBasedir;
    private final RestoService resto;
    private final ObjectMapper jsonMapper;
    private final OGCLinkService ogcLinkService;
	private boolean useGeoServerDefaultIngestions;
    private GeoserverService geoserver;
    
    @Autowired
    public FilesystemReferenceDataService(@Qualifier("referenceDataBasedir") Path referenceDataBasedir, RestoService resto, ObjectMapper jsonMapper, OGCLinkService ogcLinkService, @Value("${fstep.reference.useGeoserverDefaultIngestion:true}") boolean useGeoServerDefaultIngestions, GeoserverService geoserver) {
        this.referenceDataBasedir = referenceDataBasedir;
        this.resto = resto;
        this.jsonMapper = jsonMapper;
        this.ogcLinkService = ogcLinkService;
        this.useGeoServerDefaultIngestions = useGeoServerDefaultIngestions;
        this.geoserver = geoserver;
    }
    
	@Override
	public FstepFileIngestion ingest(String collection, User owner, String filename, UploadableFileType filetype,
			Map<String, Object> userProperties, MultipartFile multipartFile) throws IOException {
		Path dest = referenceDataBasedir.resolve(String.valueOf(owner.getId())).resolve(filename);
		LOG.info("Saving new reference data to: {}", dest);

		if (dest.toFile().exists()) {
			LOG.warn("Found already-existing reference data, overwriting: {}", dest);
		}

		Files.createDirectories(dest.getParent());
		Files.copy(multipartFile.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

		URI uri = CatalogueUri.REFERENCE_DATA
				.build(ImmutableMap.of("ownerId", owner.getId().toString(), "filename", filename));

		Map<String, Object> properties = new HashMap<>();

		// Add automatically-determined properties
		properties.put("productIdentifier", owner.getName() + "_" + filename);
		properties.put("owner", owner.getName());
		properties.put("filename", filename);
		properties.put("fstepUrl", uri);
		// TODO Get the proper MIME type
		properties.put("resourceMimeType", "application/unknown");
		long filesize = Files.size(dest);
		properties.put("resourceSize", filesize);
		properties.put("resourceChecksum", "sha256=" + MoreFiles.asByteSource(dest).hash(Hashing.sha256()));

		String startTime = (String) userProperties.remove("startTime");
		String endTime = (String) userProperties.remove("endTime");

		if (startTime != null) {
			properties.put("startDate", startTime);
		}
		if (endTime != null) {
			properties.put("completionDate", endTime);
		}

		String description = (String) userProperties.remove("description");
		if (description != null) {
			properties.put("description", description);
		}

		GeoJsonObject geometry = null;
		String ingestionStatusMessage = null;

		switch (filetype) {
			case GEOTIFF:{
				try {
					geometry = GeoUtil.extractBoundingBox(dest);
				} catch (GeometryException e) {
					ingestionStatusMessage = "Could not extract bounding box";
				}
	
				break;
			}
			case SHAPEFILE: {
				// Try to get shapefile as multipolygon, if not possible resort to bounding box
				try {
					geometry = GeoUtil.shapeFileToGeojson(dest, GEOMETRY_BOUNDING_BOX_THRESHOLD);
					if (isValidRestoGeometry(geometry) == false) {
						throw new GeometryException("Invalid Geometry - Difference in longitude > 180 degrees ");
					}
				} catch (GeometryException e) {
					ingestionStatusMessage = "Shapefile geometry not recognized - converting to bounding box. Reason: "
							+ e.getMessage();
					try {
						geometry = GeoUtil.extractBoundingBox(dest);
	
					} catch (GeometryException e1) {
						ingestionStatusMessage += "Could not extract bounding box from shapefile";
					}
				}
				break;
			}
			case OTHER: {
				String userWktGeometry = (String) userProperties.remove("geometry");
				if (userWktGeometry != null) {
					try {
						geometry = GeoUtil.getGeoJsonGeometry(userWktGeometry);
					} catch (GeometryException e) {
						ingestionStatusMessage = "Provided geometry is not recognized. Converted to "
								+ GeoUtil.defaultGeometry().toString();
					}
				}
			}
		}

		if (geometry == null) {
			geometry = GeoUtil.defaultGeometry();
			ingestionStatusMessage = "Unrecognized geometry - product ingested with default geometry POINT(0,0)" ;
		}

		// TODO Validate extra properties?
		properties.put("extraParams", jsonMapper.writeValueAsString(userProperties));

		Feature feature = new Feature();
		feature.setId(owner.getName() + "_" + filename);
		feature.setGeometry(geometry);
		feature.setProperties(properties);

		UUID restoId;
		try {
			restoId = resto.ingestReferenceData(collection, feature);
			LOG.info("Ingested reference data with Resto id {} and URI {}", restoId, uri);
		} catch (Exception e) {
			LOG.error("Failed to ingest reference data to Resto, continuing...", e);
			// TODO Add GeoJSON to FstepFile model
			restoId = UUID.randomUUID();
		}
		
		GeoServerSpec geoServerSpec = null;
        if (properties.containsKey("geoServerSpec")) {
            geoServerSpec = (GeoServerSpec) properties.get("geoServerSpec");
        }

        else if (useGeoServerDefaultIngestions){
        	geoServerSpec = getDefaultGeoServerSpec(dest, filetype);
        }

		FstepFile fstepFile = new FstepFile(uri, restoId);
		fstepFile.setOwner(owner);
		fstepFile.setType(FstepFile.Type.REFERENCE_DATA);
		fstepFile.setFilename(referenceDataBasedir.relativize(dest).toString());
		fstepFile.setFilesize(filesize);
		
		if (geoServerSpec != null) {
        	GeoserverLayer geoserverLayer = ingestIntoGeoserver(owner, dest, restoId, geoServerSpec);
        	if (geoserverLayer != null) {
        		fstepFile.getGeoserverLayers().add(geoserverLayer);
            }
        }
		return new FstepFileIngestion(ingestionStatusMessage, fstepFile);
	}

    
	private boolean isValidRestoGeometry(GeoJsonObject geometry) {
		if (geometry instanceof MultiPoint) {
			MultiPoint multipoint = (MultiPoint) geometry;
			List<LngLatAlt> coordinates = new ArrayList<>(multipoint.getCoordinates());
			coordinates.sort((p1, p2) -> Double.compare(p1.getLongitude(), p2.getLongitude()));
			double minX = coordinates.get(0).getLongitude();
			double maxX = coordinates.get(coordinates.size() -1).getLongitude();
			if (minX < -90 && maxX > 90) {
				return false;
			}
			return true;
		}
		return true;
	}
	
	
	private GeoServerSpec getDefaultGeoServerSpec(Path dest, UploadableFileType filetype) {
    	switch (filetype) {
    	case GEOTIFF: 
    		   return getDefaultGeotiffSpec(dest);
    	case SHAPEFILE:
    		   return getDefaultShapeFileSpec(dest);
        default:
        	return null;
        }
	}

    
    private GeoServerSpec getDefaultShapeFileSpec(Path dest) {
    	Path relativePath = referenceDataBasedir.relativize(dest);
		Path relativePathWithoutExtension = relativePath.getParent().resolve(MoreFiles.getNameWithoutExtension(dest.getFileName()));
		//Use a prefix starting with a letter otherwise geoserver will silently prepend a  "a_" prefix that will break the mapping
		String layerName = "refData_" + relativePathWithoutExtension.toString().replace("/", "_");
		return GeoServerSpec.builder()
		.geoserverType(GeoServerType.SHAPEFILE_POSTGIS_IMPORT)
		.options(ImmutableMap.of("mode", "create"))
		.layerName(layerName)
		.build();
	}

	private GeoServerSpec getDefaultGeotiffSpec(Path dest) {
    	Path relativePath = referenceDataBasedir.relativize(dest);
		Path relativePathWithoutExtension = relativePath.getParent().resolve(MoreFiles.getNameWithoutExtension(dest.getFileName()));
		String coverageName = relativePathWithoutExtension.toString().replace("/", "_");
		return GeoServerSpec.builder()
		.geoserverType(GeoServerType.SINGLE_COVERAGE)
		.workspace(DEFAULT_REFDATA_GEOSERVER_WORKSPACE)
		.datastoreName(coverageName)
		.coverageName(coverageName)
		.crs(GeoUtil.extractEpsg(dest))
		.build();
	}

	private GeoserverLayer ingestIntoGeoserver(User owner, Path dest, UUID restoId, GeoServerSpec geoServerSpec) {
		try {
			GeoserverLayer geoserverLayer = geoserver.ingest(dest, geoServerSpec, restoId);
			geoserverLayer.setOwner(owner);
			return geoserverLayer;
		}
		catch (Exception e) {
            LOG.error("Failed to ingest reference data to GeoServer, continuing...", e);
            return null;
        }
	}
	
	@Override
    public Resource resolve(FstepFile file) {
        Path path = referenceDataBasedir.resolve(file.getFilename());
        return new PathResource(path);
    }
	
	@Override
    public Set<Link> getOGCLinks(FstepFile fstepFile) {
        return ogcLinkService.getOGCLinks(fstepFile);
    }

    @Override
    public void delete(FstepFile file) throws IOException {
        Files.deleteIfExists(referenceDataBasedir.resolve(file.getFilename()));
        String collection = null;
        if (file.getCollection() != null) {
        	collection = file.getCollection().getIdentifier();
        }
        resto.deleteReferenceData(collection, file.getRestoId());
        for (GeoserverLayer geoserverLayer: file.getGeoserverLayers()) {
            geoserver.cleanUpGeoserverLayer(file.getFilename(), geoserverLayer);
        }
    }
    
    @Override
    public String getDefaultCollection() {
        return resto.getReferenceDataCollection();
    }
    
    @Override
    public void createCollection(Collection collection) throws IOException{
        if(!resto.createReferenceDataCollection(collection)) {
        	throw new IOException("Failed to create the underlying collection");
        }
        
    }

    @Override
    public void deleteCollection(Collection collection) throws IOException{
    	if(!resto.deleteCollection(collection)) {
        	throw new IOException("Failed to delete the underlying collection");
        }
    }

}
