package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.catalogue.CatalogueUri;
import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.internal.ReferenceDataMetadata;
import com.cgi.eoss.fstep.model.internal.UploadableFileType;
import com.cgi.eoss.fstep.persistence.service.FstepFileDataService;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>A {@link RepositoryRestController} for interacting with {@link com.cgi.eoss.fstep.model.FstepFile}s. Extends the
 * simple repository-type {@link FstepFilesApi}.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/fstepFiles")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class FstepFilesApiExtension {

    private final FstepSecurityService fstepSecurityService;
    private final FstepFileDataService fstepFileDataService;
    private final CatalogueService catalogueService;
    private final CostingService costingService;

    @PostMapping("/externalProduct")
    @ResponseBody
    public ResponseEntity saveExternalProductMetadata(@RequestBody GeoJsonObject geoJson) throws Exception {
        FstepFile fstepFile = catalogueService.indexExternalProduct(geoJson);
        return ResponseEntity.created(fstepFile.getUri()).body(new Resource<>(fstepFile));
    }

    @PostMapping("/refData")
    @ResponseBody
    public ResponseEntity saveRefData(@RequestPart(required=false) Map<String, Object> userProperties,@RequestParam UploadableFileType fileType, @RequestPart(name = "file", required = true) MultipartFile file) throws Exception {
        User owner = fstepSecurityService.getCurrentUser();
        String filename = file.getOriginalFilename();

        if (Strings.isNullOrEmpty(filename)) {
            return ResponseEntity.badRequest().body(String.format("Uploaded filename may not be null %s", file));
        }

        URI uri = CatalogueUri.REFERENCE_DATA.build(
                ImmutableMap.of(
                        "ownerId", owner.getId().toString(),
                        "filename", filename));

        if (!Objects.isNull(fstepFileDataService.getByUri(uri))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(String.format("Reference data filename '%s' already exists for user %s", filename, owner.getName()));
        }

        try {
        	ReferenceDataMetadata metadata = ReferenceDataMetadata.builder()
                    .owner(owner)
                    .filename(filename)
                    .filetype(fileType)
                    .userProperties(userProperties == null? Collections.emptyMap(): userProperties) 
                    .build();

            FstepFile fstepFile = catalogueService.ingestReferenceData(metadata, file);
            return ResponseEntity.created(fstepFile.getUri()).body(new Resource<>(fstepFile));
        } catch (Exception e) {
            LOG.error("Could not ingest reference data file {}", filename, e);
            throw e;
        }
    }

    @GetMapping(value = "/{fileId}/dl")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#file, 'read')")
    public void downloadFile(@ModelAttribute("fileId") FstepFile file, HttpServletResponse response) throws IOException {
        User user = fstepSecurityService.getCurrentUser();

        int estimatedCost = costingService.estimateDownloadCost(file);
        if (estimatedCost > user.getWallet().getBalance()) {
            response.setStatus(HttpStatus.PAYMENT_REQUIRED.value());
            String message = "Estimated download cost (" + estimatedCost + " coins) exceeds current wallet balance";
            response.getOutputStream().write(message.getBytes());
            response.flushBuffer();
            return;
        }
        // TODO Should estimated cost be "locked" in the wallet?

        org.springframework.core.io.Resource fileResource = catalogueService.getAsResource(file);

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentLengthLong(fileResource.contentLength());
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileResource.getFilename() + "\"");
        ByteStreams.copy(fileResource.getInputStream(), response.getOutputStream());
        response.flushBuffer();

        costingService.chargeForDownload(user.getWallet(), file);
    }

}
