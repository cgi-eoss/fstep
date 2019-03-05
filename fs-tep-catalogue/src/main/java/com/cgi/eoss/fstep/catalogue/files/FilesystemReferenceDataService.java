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
import java.util.UUID;

import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.MultiPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.cgi.eoss.fstep.catalogue.CatalogueUri;
import com.cgi.eoss.fstep.catalogue.resto.RestoService;
import com.cgi.eoss.fstep.catalogue.util.GeoUtil;
import com.cgi.eoss.fstep.catalogue.util.GeometryException;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.internal.FstepFileIngestion;
import com.cgi.eoss.fstep.model.internal.UploadableFileType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import com.google.common.io.MoreFiles;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class FilesystemReferenceDataService implements ReferenceDataService {

    private static final int GEOMETRY_BOUNDING_BOX_THRESHOLD = 10000;
	private final Path referenceDataBasedir;
    private final RestoService resto;
    private final ObjectMapper jsonMapper;

    @Autowired
    public FilesystemReferenceDataService(@Qualifier("referenceDataBasedir") Path referenceDataBasedir, RestoService resto, ObjectMapper jsonMapper) {
        this.referenceDataBasedir = referenceDataBasedir;
        this.resto = resto;
        this.jsonMapper = jsonMapper;
    }

	@Override
	public FstepFileIngestion ingest(User owner, String filename, UploadableFileType filetype,
			Map<String, Object> userProperties, MultipartFile multipartFile) throws IOException {
		Path dest = referenceDataBasedir.resolve(String.valueOf(owner.getId())).resolve(filename);
		LOG.info("Saving new reference data to: {}", dest);

		if (Files.exists(dest)) {
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
			properties.put("startDate", startTime.toString());
		}
		if (endTime != null) {
			properties.put("completionDate", endTime.toString());
		}

		String description = (String) userProperties.remove("description");
		if (description != null) {
			properties.put("description", description.toString());
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
			restoId = resto.ingestReferenceData(feature);
			LOG.info("Ingested reference data with Resto id {} and URI {}", restoId, uri);
		} catch (Exception e) {
			LOG.error("Failed to ingest reference data to Resto, continuing...", e);
			// TODO Add GeoJSON to FstepFile model
			restoId = UUID.randomUUID();
		}

		FstepFile fstepFile = new FstepFile(uri, restoId);
		fstepFile.setOwner(owner);
		fstepFile.setType(FstepFile.Type.REFERENCE_DATA);
		fstepFile.setFilename(referenceDataBasedir.relativize(dest).toString());
		fstepFile.setFilesize(filesize);
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

	@Override
    public Resource resolve(FstepFile file) {
        Path path = referenceDataBasedir.resolve(file.getFilename());
        return new PathResource(path);
    }

    @Override
    public void delete(FstepFile file) throws IOException {
        Files.deleteIfExists(referenceDataBasedir.resolve(file.getFilename()));
        resto.deleteReferenceData(file.getRestoId());
    }

}
