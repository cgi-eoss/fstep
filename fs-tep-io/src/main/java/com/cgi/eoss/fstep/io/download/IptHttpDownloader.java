package com.cgi.eoss.fstep.io.download;

import com.cgi.eoss.fstep.io.ServiceIoException;
import com.cgi.eoss.fstep.rpc.Credentials;
import com.cgi.eoss.fstep.rpc.FstepServerClient;
import com.cgi.eoss.fstep.rpc.GetCredentialsParams;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.jayway.jsonpath.JsonPath;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Downloader for accessing data from <a href="https://finder.eocloud.eu">EO Cloud</a>. Uses IPT's token
 * authentication process.</p>
 */
@Component
@Log4j2
public class IptHttpDownloader implements Downloader {

    private final FstepServerClient fstepServerClient;
    private final OkHttpClient httpClient;
    private final OkHttpClient searchClient;
    private final ObjectMapper objectMapper;
    private final DownloaderFacade downloaderFacade;
    private final IptHttpDownloaderProperties properties;
    private static Random random = new Random();
    private static final String FILENAME_HEADER = "Content-disposition";
    private static final Pattern FILENAME_PATTERN = Pattern.compile(".*filename=(.*)");

    @Autowired
    IptHttpDownloader(OkHttpClient okHttpClient, FstepServerClient fstepServerClient, DownloaderFacade downloaderFacade, IptHttpDownloaderProperties properties) {
        this.httpClient = okHttpClient;
        // Use a long timeout as the search query takes a while...
        this.searchClient = okHttpClient.newBuilder().readTimeout(60, TimeUnit.SECONDS).build();
        this.fstepServerClient = fstepServerClient;
        this.downloaderFacade = downloaderFacade;
        this.objectMapper = new ObjectMapper();
        this.properties = properties;
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
        return ImmutableSet.of("sentinel1", "sentinel2", "sentinel3", "landsat5", "landsat7", "landsat8", "envisat");
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
    	int count = 0;
    	int maxTries = properties.getRetries();
    	while(true) {
    	    try {
    	    	Path path = iptDownload(targetDir, uri);
    	    	return path;
    	    } catch (Exception e) {
    	        if (++count == maxTries) { 
    	        	throw e;
    	        }
    	        try {
    	            Thread.sleep(((int) Math.round(Math.pow(2, count)) * 1000) 
    	                + (random.nextInt(999) + 1));
    	        } catch (InterruptedException ie) {
    	            //Keep retrying 
    	        }
    	    }
    	}
    }

	private Path iptDownload(Path targetDir, URI uri) throws IOException {
		LOG.info("Downloading: {}", uri);

        // IPT downloading is three-step:
        //   1. Call an authentication endpoint to get a token
        //   2. Get the product download location URL by searching the IPT catalogue
        //   3. Download the product using the token identity as a parameter

        Credentials credentials = fstepServerClient.credentialsServiceBlockingStub().getCredentials(
                GetCredentialsParams.newBuilder().setHost(HttpUrl.parse(properties.getIptDownloadUrl()).host()).build());
        String authToken = getAuthToken(credentials);

        HttpUrl downloadUrl = getDownloadUrl(uri, authToken);

        LOG.debug("Resolved IPT download URL with auth token: {}", downloadUrl);

        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = httpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new ServiceIoException("Unsuccessful HTTP response: " + response);
        }

        String filename = getFilename(downloadUrl, response.headers(FILENAME_HEADER));
        Path outputFile = targetDir.resolve(filename);

        try (BufferedSource source = response.body().source();
             BufferedSink sink = Okio.buffer(Okio.sink(outputFile))) {
            long downloadedBytes = sink.writeAll(source);
            LOG.debug("Downloaded {} bytes for {}", downloadedBytes, uri);
        }
        response.close();

        LOG.info("Successfully downloaded via IPT: {}", outputFile);
        return outputFile;
	}
	
	 private String getFilename(HttpUrl httpUrl, List<String> headers) {
	        return headers.stream()
	                .map(FILENAME_PATTERN::matcher)
	                .filter(Matcher::matches)
	                .map(m -> m.group(1))
	                .findAny()
	                .orElse(Iterables.getLast(httpUrl.pathSegments()));
	    }

    private String getAuthToken(Credentials credentials) throws IOException {
        Request authRequest = new Request.Builder()
                .url(properties.getAuthEndpoint())
                .post(new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("domainName", properties.getAuthDomain())
                        .addFormDataPart("userName", credentials.getUsername())
                        .addFormDataPart("userPass", credentials.getPassword())
                        .build())
                .build();

        try (Response response = httpClient.newCall(authRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new ServiceIoException("Unsuccessful IPT login: " + response);
            }
            IptTokenResponse iptTokenResponse = objectMapper.readValue(response.body().string(), IptTokenResponse.class);
            LOG.debug("Logged into IPT as user '{}' (id: {}) with tokenIdentity '{}'", iptTokenResponse.getUserName(), iptTokenResponse.getUserId(), iptTokenResponse.getTokenIdentity());
            return iptTokenResponse.getTokenIdentity();
        }
    }

    private HttpUrl getDownloadUrl(URI uri, String authToken) throws IOException {
        // Trim the leading slash from the path and get the search URL
        String productId = uri.getPath().substring(1);
        String collection = getCollection(uri);
        HttpUrl searchUrl = HttpUrl.parse(properties.getIptSearchUrl()).newBuilder()
                .addPathSegments("api/collections/" + collection + "/search.json")
                .addQueryParameter("maxRecords", "1")
                .addQueryParameter("productIdentifier", "%" + productId + "%")
                .build();

        LOG.debug("Searching IPT to find download URL: {}", searchUrl);

        Request request = new Request.Builder().url(searchUrl).get().build();

        try (Response response = searchClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.error("Received unsuccessful HTTP response for IPT search: {}", response.toString());
                throw new ServiceIoException("Unexpected HTTP response from IPT: " + response.message());
            }

            String responseBody = response.body().string();
            String productPath = JsonPath.read(responseBody, "$.features[0].properties.productIdentifier");

            return HttpUrl.parse(properties.getIptDownloadUrl()).newBuilder()
                    .addPathSegments(productPath.replaceFirst("^/eodata/", ""))
                    .addQueryParameter("token", authToken)
                    .build();
        }
    }

    private String getCollection(URI uri) {
		switch (uri.getScheme()) {
			case "sentinel1": return "Sentinel1";
			case "sentinel2": return "Sentinel2";
			case "sentinel3": return "Sentinel3";
			case "envisat": return "Envisat";
			case "landsat5": return "Landsat5";
			case "landsat7": return "Landsat7";
			case "landsat8": return "Landsat8";
			default: throw new ServiceIoException("Unsupported collection");
			
		}
	}

	@Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class IptTokenResponse {
        private String token;
        private String tokenIdentity;
        private String userName;
        private String userId;
        private String objectStoreUrl;
    }

    @Data
    @Component
    private static final class IptHttpDownloaderProperties {
        @Value("${fstep.worker.downloader.ipt.searchUrl:https://finder.eocloud.eu/resto/}")
        private String iptSearchUrl;
        @Value("${fstep.worker.downloader.ipt.downloadBaseUrl:https://static.eocloud.eu/v1/AUTH_8f07679eeb0a43b19b33669a4c888c45}")
        private String iptDownloadUrl;
        @Value("${fstep.worker.downloader.ipt.authEndpoint:https://finder.eocloud.eu/resto/api/authidentity}")
        private String authEndpoint;
        @Value("${fstep.worker.downloader.ipt.authDomain:__secret__}")
        private String authDomain;
        @Value("${fstep.worker.downloader.ipt.retries:3}")
        private int retries;
    }

}
