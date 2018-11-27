package com.cgi.eoss.fstep.io.download;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.cgi.eoss.fstep.io.ServiceIoException;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * <p>Downloader for accessing data from <a href="https://finder.eocloud.eu">EO Cloud</a>. Uses IPT's token
 * authentication process.</p>
 */
@Component
@Log4j2
public class CreoDIASDownloader implements Downloader {

	private final OkHttpClient searchClient;
	private final AmazonS3 s3Client;
    private final DownloaderFacade downloaderFacade;
    private final CreoDIASDownloaderProperties properties;
 
    @Autowired
    CreoDIASDownloader(OkHttpClient okHttpClient, DownloaderFacade downloaderFacade, CreoDIASDownloaderProperties properties) {
    	// Use a long timeout as the search query takes a while...
    	this.searchClient = okHttpClient.newBuilder().readTimeout(60, TimeUnit.SECONDS).build();
        this.downloaderFacade = downloaderFacade;
        this.properties = properties;
        this.s3Client = initS3Client();
    }

    @PostConstruct
    public void postConstruct() {
        downloaderFacade.registerDownloader(this);
    }

    @PreDestroy
    public void preDestroy() {
        downloaderFacade.unregisterDownloader(this);
    }

    @Override
    public Set<String> getProtocols() {
        return ImmutableSet.of("sentinel1", "sentinel2", "sentinel3", "sentinel5p", "landsat5", "landsat7", "landsat8", "envisat");
    }
    
    private AmazonS3 initS3Client() {
    	ClientConfiguration clientConfiguration = new ClientConfiguration();
		clientConfiguration.setSignerOverride("AWSS3V4SignerType");
		clientConfiguration.setMaxErrorRetry(properties.getRetries());
		clientConfiguration.setConnectionTimeout(properties.getConnectionTimeout());
		clientConfiguration.setSocketTimeout(properties.getSocketTimeout());
		clientConfiguration.setProtocol(properties.getProtocol().equals("HTTP")? Protocol.HTTP:Protocol.HTTPS);
		AmazonS3 amazonS3Client = AmazonS3ClientBuilder
				.standard()
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(properties.getEndpoint(), properties.getRegion()))
				.withPathStyleAccessEnabled(true)
				.withClientConfiguration(clientConfiguration)
				.withCredentials(new AWSStaticCredentialsProvider(
						new BasicAWSCredentials(properties.getAccessKey(), properties.getPrivateKey())))
				.build();
		return amazonS3Client;
    }
    
    @Override
    public int getPriority(URI uri) {
    	return properties.getPriority();
    }
    

	@Override
    public Path download(Path targetDir, URI uri) throws IOException {
    	LOG.info("Downloading: {}", uri);

        // CreoDIAS downloading:
        //   1. Get the product download location URL by searching the CreoDIAS catalogue
        //   2. Download the product from object storage

        String productIdentifier = getProductIdentifier(uri);
        String prefix = productIdentifier.substring("/eodata/".length()) + "/";
        ObjectListing objects = s3Client.listObjects(properties.getBucket(), prefix);
        
        List<S3ObjectSummary> summaries = objects.getObjectSummaries();
        LOG.debug("Found {} objects ", summaries.size());
        
        if (summaries.size() == 0) {
        	throw new ServiceIoException("Object not found");
        }
        
        int downloadedObjects = 0;
        
        for (S3ObjectSummary summary: summaries) {
        	downloadObject(targetDir, prefix, summary.getKey());
        	downloadedObjects++;
        }

        while (objects.isTruncated()) {
        	objects = s3Client.listNextBatchOfObjects(objects);
            summaries = objects.getObjectSummaries();
            
            for (S3ObjectSummary summary: summaries) {
            	downloadObject(targetDir, prefix, summary.getKey());
            	downloadedObjects++;
            }
        }
        
        if (summaries.size() != downloadedObjects) {
        	throw new ServiceIoException("Did not managed to download all product parts");
        }

        LOG.info("Successfully downloaded via CreoDIAS: {}", productIdentifier);
        return targetDir;
	}

	private void downloadObject(Path targetDir, String prefix, String key) throws IOException {
		LOG.debug("Object key {}", key);
		S3Object object = s3Client.getObject(properties.getBucket(), key);
		String relativePath = key.substring(prefix.length());
		if (relativePath.length() > 0) {
			Path outputFilePath = targetDir.resolve(relativePath);
			if (relativePath.endsWith("/")){
				Files.createDirectories(outputFilePath);
			}
			else {
				Files.copy(object.getObjectContent(), outputFilePath);
			}
		}
		object.close();
	}

    private String getProductIdentifier(URI uri) throws IOException {
        // Trim the leading slash from the path and get the search URL
        String productId = uri.getPath().substring(1);
        String collection = getCollection(uri);
        HttpUrl searchUrl = HttpUrl.parse(properties.getCreoDIASSearchUrl()).newBuilder()
                .addPathSegments("api/collections/" + collection + "/search.json")
                .addQueryParameter("maxRecords", "1")
                .addQueryParameter("productIdentifier", "%" + productId + "%")
                .build();

        LOG.debug("Searching CreoDIAS to find full product identifier: {}", searchUrl);

        Request request = new Request.Builder().url(searchUrl).get().build();

        try (Response response = searchClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.error("Received unsuccessful HTTP response for IPT search: {}", response.toString());
                throw new ServiceIoException("Unexpected HTTP response from IPT: " + response.message());
            }

            String responseBody = response.body().string();
            String productPath = JsonPath.read(responseBody, "$.features[0].properties.productIdentifier");

            return productPath;
        }
    }

    private String getCollection(URI uri) {
		switch (uri.getScheme()) {
			case "sentinel1": return "Sentinel1";
			case "sentinel2": return "Sentinel2";
			case "sentinel3": return "Sentinel3";
			case "sentinel5p": return "Sentinel5P";
			case "envisat": return "Envisat";
			case "landsat5": return "Landsat5";
			case "landsat7": return "Landsat7";
			case "landsat8": return "Landsat8";
			default: throw new ServiceIoException("Unsupported collection");
			
		}
	}

	@Component
	@Data
    public static final class CreoDIASDownloaderProperties {
		@Value("${fstep.worker.downloader.creodias.searchUrl:https://finder.creodias.eu/resto/}")
        private String creoDIASSearchUrl;
        @Value("${fstep.worker.downloader.creodias.endpoint:http://data.cloudferro.com}")
        private String endpoint;
        @Value("${fstep.worker.downloader.creodias.region:RegionOne}")
        private String region;
        @Value("${fstep.worker.downloader.creodias.bucket:EODATA}")
        private String bucket;
        @Value("${fstep.worker.downloader.creodias.accessKey:access}")
        private String accessKey;
        @Value("${fstep.worker.downloader.creodias.privateKey:access}")
        private String privateKey;
        @Value("${fstep.worker.downloader.creodias.protocol:HTTP}")
        private String protocol;
        @Value("${fstep.worker.downloader.creodias.connectionTimeout:10000}")
        private int connectionTimeout;
        @Value("${fstep.worker.downloader.creodias.socketTimeout:10000}")
        private int socketTimeout;
        @Value("${fstep.worker.downloader.creodias.retries:3}")
        private int retries;
        @Value("${fstep.worker.downloader.creodias.priority:1}")
        private int priority;
    }

}
