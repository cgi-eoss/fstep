package com.cgi.eoss.fstep.orchestrator.service;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.catalogue.geoserver.GeoServerSpec;
import com.cgi.eoss.fstep.catalogue.util.GeoUtil;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepFilesRelation;
import com.cgi.eoss.fstep.model.FstepFilesRelation.Type;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor.Parameter;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor.Relation;
import com.cgi.eoss.fstep.model.internal.OutputFileMetadata;
import com.cgi.eoss.fstep.model.internal.OutputFileMetadata.OutputFileMetadataBuilder;
import com.cgi.eoss.fstep.model.internal.OutputProductMetadata;
import com.cgi.eoss.fstep.model.internal.OutputProductMetadata.OutputProductMetadataBuilder;
import com.cgi.eoss.fstep.model.internal.ParameterRelationTypeToFileRelationTypeUtil;
import com.cgi.eoss.fstep.model.internal.RetrievedOutputFile;
import com.cgi.eoss.fstep.persistence.service.FstepFilesRelationDataService;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.protobuf.Message;
import com.mysema.commons.lang.Pair;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
public class OutputIngestionService {

	private CatalogueService catalogueService;
	private FstepFilesRelationDataService filesRelationDataService;
	
	@Autowired
	public OutputIngestionService(CatalogueService catalogueService, FstepFilesRelationDataService filesRelationDataService) {
		this.catalogueService = catalogueService;
		this.filesRelationDataService = filesRelationDataService;
	}
	
	public FstepFile repatriateAndIngestOutputFile(Job job, String outputId, Path filePath,
			OutputFileProvider<? extends Message> outputFileProvider) throws IOException, InterruptedException {
		OutputProductMetadata outputProductMetadata = getOutputProductMetadata(job, outputId);
		RetrievedOutputFile retrievedOutputFile = repatriateAndIngestOutputFile(outputFileProvider, outputProductMetadata, job, filePath);
		postProcessOutputProduct(retrievedOutputFile);
		FstepFile outputFile = catalogueService.ingestOutputProduct(retrievedOutputFile.getOutputFileMetadata(),
				retrievedOutputFile.getPath());
		processOutputFileRelations(outputId, outputFile, job);
		return outputFile;
	}

	public FstepFile repatriateAndIngestOutputFile(Job job, String outputId, Path filePath, long fileSize,
			InputStream fileInputStream) throws IOException {
		OutputProductMetadata outputProductMetadata = getOutputProductMetadata(job, outputId);
		RetrievedOutputFile retrievedOutputFile = repatriateAndIngestOutputFile(outputProductMetadata, job, filePath, fileSize, fileInputStream);
		postProcessOutputProduct(retrievedOutputFile);
		FstepFile outputFile = catalogueService.ingestOutputProduct(retrievedOutputFile.getOutputFileMetadata(),
				retrievedOutputFile.getPath());
		processOutputFileRelations(outputId, outputFile, job);
		return outputFile;
	}
	
	private RetrievedOutputFile repatriateAndIngestOutputFile(OutputProductMetadata outputProductMetadata, Job job,
			Path filePath, long fileSize, InputStream fileInputStream) throws IOException {
		OutputFileMetadataBuilder outputFileMetadataBuilder = OutputFileMetadata.builder();
		OutputFileMetadata outputFileMetadata = outputFileMetadataBuilder.outputProductMetadata(outputProductMetadata).build();
		String outputId = outputProductMetadata.getOutputId();
		String fullFilePath = Paths.get(outputId).resolve(filePath).toString();
		Path outputPath = catalogueService.provisionNewOutputProduct(outputProductMetadata, fullFilePath, fileSize);
		LOG.info("Writing output file for job {}: {}", job.getExtId(), outputPath);
		BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outputPath, CREATE, TRUNCATE_EXISTING, WRITE));
		IOUtils.copy(fileInputStream, outputStream);
		Pair<OffsetDateTime, OffsetDateTime> startEndDateTimes = getServiceOutputParameter(job.getConfig().getService(),
				outputProductMetadata.getOutputId()).map(p-> PlatformParameterExtractor.extractStartEndDateTimes(p, outputPath.getFileName().toString())).orElseGet(() -> new Pair<>(null, null));
		outputFileMetadata.setStartDateTime(startEndDateTimes.getFirst());
		outputFileMetadata.setEndDateTime(startEndDateTimes.getSecond());
		return new RetrievedOutputFile(outputFileMetadata, outputPath);
	}

	public OutputProductMetadata getOutputProductMetadata(Job job, String outputId) throws IOException {
		Map<String, GeoServerSpec> geoServerSpecs = PlatformParameterExtractor.getGeoServerSpecs(job);
		Map<String, String> collectionSpecs = PlatformParameterExtractor.getCollectionSpecs(job);
		return buildOutputMetadata(job, geoServerSpecs, collectionSpecs, outputId);
	}
	
	private <T extends Message> RetrievedOutputFile repatriateAndIngestOutputFile(OutputFileProvider<T> outputFileProvider, OutputProductMetadata outputProductMetadata, Job job, Path filePath) throws IOException, InterruptedException {
		try(OutputIngestionFileStreamClient<T> fileStreamClient = new OutputIngestionFileStreamClient<>(catalogueService, outputProductMetadata, job, filePath)){
			outputFileProvider.provideFile(fileStreamClient.getFileStreamObserver());
			fileStreamClient.getLatch().await();
			return fileStreamClient.getRetrievedOutputFile();
		}
	}
	
	private OutputProductMetadata buildOutputMetadata(Job job, Map<String, GeoServerSpec> geoServerSpecs,
			Map<String, String> collectionSpecs, String outputId) {
		OutputProductMetadataBuilder outputProductMetadataBuilder = OutputProductMetadata.builder()
				.owner(job.getOwner()).service(job.getConfig().getService()).outputId(outputId).jobId(job.getExtId());
		Builder<String, Object> propertiesBuilder = ImmutableMap.<String, Object>builder()
		.put("jobId", job.getExtId()).put("intJobId", job.getId())
		.put("serviceName", job.getConfig().getService().getName()).put("jobOwner", job.getOwner().getName());
		if (job.getStartTime() != null) {
			propertiesBuilder.put("jobStartTime", job.getStartTime().atOffset(ZoneOffset.UTC).toString());
		}
		if (job.getEndTime() != null) {
			propertiesBuilder.put("jobEndTime", job.getEndTime().atOffset(ZoneOffset.UTC).toString());
		}
		HashMap<String, Object> properties = new HashMap<>(propertiesBuilder.build());

		GeoServerSpec geoServerSpecForOutput = geoServerSpecs.get(outputId);
		if (geoServerSpecForOutput != null) {
			properties.put("geoServerSpec", geoServerSpecForOutput);
		}

		String collectionSpecForOutput = collectionSpecs.get(outputId);
		if (collectionSpecForOutput != null) {
			properties.put("collection", collectionSpecForOutput);
		}

		Map<String, String> extraParams = new HashMap<>();
		extraParams.put("outputId", outputId);

		getServiceOutputParameter(job.getConfig().getService(), outputId)
				.ifPresent(p -> addPlatformMetadata(extraParams, p));
		properties.put("extraParams", extraParams);

		return outputProductMetadataBuilder.productProperties(properties).build();
	}

	private void addPlatformMetadata(Map<String, String> extraParams, Parameter outputParameter) {
		if (outputParameter.getPlatformMetadata() != null && outputParameter.getPlatformMetadata().size() > 0) {
			extraParams.putAll(outputParameter.getPlatformMetadata());
		}
	}
	
	private Optional<Parameter> getServiceOutputParameter(FstepService service, String outputId) {
		return service.getServiceDescriptor().getDataOutputs().stream().filter(p -> p.getId().equals(outputId))
				.findFirst();
	}
	
	private RetrievedOutputFile postProcessOutputProduct(RetrievedOutputFile retrievedOutputFile) {
		retrievedOutputFile.getOutputFileMetadata().setCrs(getOutputCrs(retrievedOutputFile.getPath()));
		retrievedOutputFile.getOutputFileMetadata().setGeometry(getOutputGeometry(retrievedOutputFile.getPath()));
		return retrievedOutputFile;
	}
	
	private String getOutputCrs(Path outputPath) {
		try {
			return GeoUtil.extractEpsg(outputPath);
		} catch (Exception e) {
			return null;
		}
	}

	private String getOutputGeometry(Path outputPath) {
		try {
			return GeoUtil.geojsonToWkt(GeoUtil.extractBoundingBox(outputPath));
		} catch (Exception e) {
			return null;
		}
	}
	
	private void processOutputFileRelations(String outputId, FstepFile outputFile, Job job) {
		processRelationsAsSource(outputId, outputFile, job);
		processRelationsAsTarget(outputId, outputFile, job);
	}

	private void processRelationsAsSource(String outputId, FstepFile sourceFile, Job job) {
		Optional<Parameter> outputParameter = getServiceOutputParameter(job.getConfig().getService(), outputId);
		if (outputParameter.isPresent() && outputParameter.get().getParameterRelations() != null) {
			for (Relation parameterRelation : outputParameter.get().getParameterRelations()) {
				Collection<FstepFile> targetFiles = findJobOutputFilesById(
						job, parameterRelation.getTargetParameterId());
				for (FstepFile targetFile : targetFiles) {
					Type relationType = ParameterRelationTypeToFileRelationTypeUtil
							.fromParameterRelationType(parameterRelation.getType());
					FstepFilesRelation relation = new FstepFilesRelation(sourceFile, targetFile, relationType);
					filesRelationDataService.save(relation);
				}
			}
		}
	}
	
	private void processRelationsAsTarget(String outputId, FstepFile targetFile, Job job) {
		List<ImmutablePair<Parameter, Relation>> sourceParameterAndRelations = getSourceParametersAndRelations(job.getConfig().getService(), outputId);
		for (ImmutablePair<Parameter, Relation> sourceParameterAndRelation: sourceParameterAndRelations) {
			Parameter sourceParameter = sourceParameterAndRelation.getLeft();
			Relation parameterRelation = sourceParameterAndRelation.getRight();
			Collection<FstepFile> sourceFiles = findJobOutputFilesById(
					job, sourceParameter.getId());
			for (FstepFile sourceFile: sourceFiles) {
				Type relationType = ParameterRelationTypeToFileRelationTypeUtil
						.fromParameterRelationType(parameterRelation.getType());
				FstepFilesRelation relation = new FstepFilesRelation(sourceFile, targetFile, relationType);
				filesRelationDataService.save(relation);
			}
		}
	}

	
	private List<ImmutablePair<Parameter, Relation>> getSourceParametersAndRelations(FstepService service, String outputId) {
		return service.getServiceDescriptor().getDataOutputs().stream().flatMap(p -> getRelationsWithOutput(p, outputId).stream().map(r -> ImmutablePair.of(p,  r))).collect(Collectors.toList());
	}
	
	private List<Relation> getRelationsWithOutput(Parameter p, String outputId) {
		List<Relation> parameterRelations = p.getParameterRelations();
		if (parameterRelations == null || parameterRelations.isEmpty()) {
			return Collections.emptyList();
		}
		return parameterRelations.stream().filter(r-> r.getTargetParameterId().equals(outputId)).collect(Collectors.toList());
	}
	
	private List<FstepFile> findJobOutputFilesById(Job job, String outputId) {
		return job.getOutputs().get(outputId).stream()
				.map(output-> getFileByUri(job, output)).filter(Optional::isPresent)
				  .map(Optional::get)
				  .collect(Collectors.toList());
	}
	
	private Optional<FstepFile> getFileByUri(Job job, String output){
		return job.getOutputFiles().stream().filter(f -> f.getUri().toString().equals(output)).findFirst();
	}

}
