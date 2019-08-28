package com.cgi.eoss.fstep.orchestrator.service;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Optional;

import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor.Parameter;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.internal.OutputFileMetadata;
import com.cgi.eoss.fstep.model.internal.OutputFileMetadata.OutputFileMetadataBuilder;
import com.cgi.eoss.fstep.model.internal.OutputProductMetadata;
import com.cgi.eoss.fstep.model.internal.RetrievedOutputFile;
import com.cgi.eoss.fstep.rpc.FileStream;
import com.cgi.eoss.fstep.rpc.FileStreamClient;
import com.google.protobuf.Message;
import com.mysema.commons.lang.Pair;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class OutputIngestionFileStreamClient<T extends Message> extends FileStreamClient<T> {

	private OutputProductMetadata outputProductMetadata;
	private Job job;
	private CatalogueService catalogueService;
	private Path filePath;
	
	private OutputFileMetadata outputFileMetadata;
	
	@Getter
	private RetrievedOutputFile retrievedOutputFile;

	public OutputIngestionFileStreamClient(CatalogueService catalogueService, OutputProductMetadata outputProductMetadata, Job job, Path filePath) {
		this.catalogueService = catalogueService;
		this.outputProductMetadata = outputProductMetadata;
		this.job=job;
		this.filePath = filePath;
	}

	@Override
	public OutputStream buildOutputStream(FileStream.FileMeta fileMeta) throws IOException {
		LOG.info("Collecting output '{}' with filename {} ({} bytes)", outputProductMetadata.getOutputId(), fileMeta.getFilename(),
				fileMeta.getSize());

		OutputFileMetadataBuilder outputFileMetadataBuilder = OutputFileMetadata.builder();

		outputFileMetadata = outputFileMetadataBuilder.outputProductMetadata(outputProductMetadata).build();

		setOutputPath(catalogueService.provisionNewOutputProduct(outputProductMetadata, filePath.toString(), fileMeta.getSize()));
		LOG.info("Writing output file for job {}: {}", job.getExtId(), getOutputPath());
		return new BufferedOutputStream(Files.newOutputStream(getOutputPath(), CREATE, TRUNCATE_EXISTING, WRITE));
	}


	@Override
	public void onCompleted() {
		super.onCompleted();
		Pair<OffsetDateTime, OffsetDateTime> startEndDateTimes = getServiceOutputParameter(job.getConfig().getService(),
				outputProductMetadata.getOutputId()).map(p-> PlatformParameterExtractor.extractStartEndDateTimes(p, getOutputPath().getFileName().toString())).orElseGet(() -> new Pair<>(null, null));
		outputFileMetadata.setStartDateTime(startEndDateTimes.getFirst());
		outputFileMetadata.setEndDateTime(startEndDateTimes.getSecond());
		retrievedOutputFile = new RetrievedOutputFile(outputFileMetadata, getOutputPath());
	}
	
	private Optional<Parameter> getServiceOutputParameter(FstepService service, String outputId) {
		return service.getServiceDescriptor().getDataOutputs().stream().filter(p -> p.getId().equals(outputId))
				.findFirst();
	}

	
}