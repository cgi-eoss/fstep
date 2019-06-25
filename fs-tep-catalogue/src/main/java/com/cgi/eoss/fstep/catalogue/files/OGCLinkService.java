package com.cgi.eoss.fstep.catalogue.files;

import com.cgi.eoss.fstep.catalogue.geoserver.GeoserverService;
import com.cgi.eoss.fstep.model.GeoserverLayer;
import com.cgi.eoss.fstep.model.FstepFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Component
public class OGCLinkService {
    private static final String OGC_VERSION_PARAMETER = "version";
	private static final String OGC_SERVICE_PARAMETER = "service";
	private final GeoserverService geoserver;
    
    @Autowired
    public OGCLinkService(GeoserverService geoserver) {
        this.geoserver = geoserver;
    }

    public Set<Link> getOGCLinks(FstepFile fstepFile) {
        Set<Link> links = new HashSet<>();
        for (GeoserverLayer geoserverLayer : fstepFile.getGeoserverLayers()) {
            switch (geoserverLayer.getStoreType()) {
                case MOSAIC:
                    links.add(new Link(getWMSLinkToFileInMosaic(fstepFile, geoserverLayer), "wms"));
                    break;
                case GEOTIFF:
                    links.add(new Link(getWMSLinkToLayer(geoserverLayer), "wms"));
                    break;
                case POSTGIS:
                    links.add(new Link(getWMSLinkToLayer(geoserverLayer), "wms"));
                    links.add(new Link(getWFSLinkToLayer(fstepFile, geoserverLayer), "wfs"));
                    break;
            }
        }
        return links;
    }

    private String getWMSLinkToFileInMosaic(FstepFile fstepFile, GeoserverLayer geoserverLayer) {
        OffsetDateTime start = OffsetDateTime.of(LocalDateTime.of(LocalDate.ofEpochDay(0), LocalTime.MIDNIGHT), ZoneOffset.UTC);
        OffsetDateTime end = start.plusYears(140);
        
        return geoserver.getExternalUrl().newBuilder().addPathSegment(geoserverLayer.getWorkspace()).addPathSegment("wms")
                        .addQueryParameter(OGC_SERVICE_PARAMETER, "WMS").addQueryParameter(OGC_VERSION_PARAMETER, "1.1.0")
                        .addQueryParameter("layers", geoserverLayer.getWorkspace() + ":" + geoserverLayer.getLayer())
                        .addQueryParameter("time", DateTimeFormatter.ISO_INSTANT.format(start) + "/" + DateTimeFormatter.ISO_INSTANT.format(end))
                        .addQueryParameter("cql_filter", "(location LIKE '%" + fstepFile.getFilename() + "%')")
                        .build().toString();
    }

    private String getWMSLinkToLayer(GeoserverLayer geoserverLayer) {
        return geoserver.getExternalUrl().newBuilder().addPathSegment(geoserverLayer.getWorkspace()).addPathSegment("wms")
                        .addQueryParameter(OGC_SERVICE_PARAMETER, "WMS").addQueryParameter(OGC_VERSION_PARAMETER, "1.1.0")
                        .addQueryParameter("layers", geoserverLayer.getWorkspace() + ":" + geoserverLayer.getLayer())
                        .build().toString();
    }

    private String getWFSLinkToLayer(FstepFile fstepFile, GeoserverLayer geoserverLayer) {
        return geoserver.getExternalUrl().newBuilder().addPathSegment(geoserverLayer.getWorkspace()).addPathSegment("wfs")
                        .addQueryParameter(OGC_SERVICE_PARAMETER, "WFS").addQueryParameter(OGC_VERSION_PARAMETER, "1.0.0")
                        .addQueryParameter("typeName", geoserverLayer.getWorkspace() + ":" + geoserverLayer.getLayer())                        
                        .addQueryParameter("cql_filter", "(fstep_id  = '" + fstepFile.getRestoId() + "')")
                        .build().toString();
    }

}
