package com.cgi.eoss.fstep.io.download;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.cgi.eoss.fstep.io.ServiceIoException;
import com.cgi.eoss.fstep.rpc.Credentials;
import com.cgi.eoss.fstep.rpc.FstepServerClient;
import com.cgi.eoss.fstep.rpc.GetCredentialsParams;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import okhttp3.Authenticator;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

@Component
@Log4j2
public class CLMSDownloader implements Downloader {

	@Data
    public class FileDetails {
		private final HttpUrl downloadUrl;
		private final String fileName;
	}
	
    private final FstepServerClient fstepServerClient;
    private final OkHttpClient searchClient;
    private final OkHttpClient downloadClient;
    
    private final DownloaderFacade downloaderFacade;
	private CLMSDownloaderProperties properties;
	private static Random random = new Random();
    @Autowired
    public CLMSDownloader(FstepServerClient fstepServerClient, OkHttpClient okHttpClient, DownloaderFacade downloaderFacade, CLMSDownloaderProperties properties) {
        this.fstepServerClient = fstepServerClient;
        this.searchClient = okHttpClient.newBuilder()
                .addInterceptor(new HttpLoggingInterceptor(LOG::trace).setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        this.downloadClient = okHttpClient.newBuilder()
        		.authenticator(new CLMSAuthenticator())
                .addInterceptor(new HttpLoggingInterceptor(LOG::trace).setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        
        this.downloaderFacade = downloaderFacade;
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
        return ImmutableSet.of("clms");
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
    	LOG.info("Downloading: {}", uri);
    	
        // CLMS downloading
        //   1. Get the product download location URL by searching the CLMS catalogue
        //   2 Get the product metalink using basic auth
    	//   3.Download all the product files using the basic auth
    	HttpUrl metalinkUrl = getMetalinkUrl(uri);
    	
    	HttpUrl productUrl = HttpUrl.parse(uri.toString().replaceFirst("clms://", "http://"));
    	String bbox = productUrl.queryParameter("bbox");
    	if (!Strings.isNullOrEmpty(bbox)) {
        	metalinkUrl = metalinkUrl.newBuilder().addQueryParameter("coord", bbox.replace('%', ',')).build();
        }
        LOG.info("Resolved CLMS metalink URL: {}", metalinkUrl);
        
        Request request = new Request.Builder().url(metalinkUrl).get().build();
        try (Response response = downloadClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.error("Received unsuccessful HTTP response for CLMS search: {}", response.toString());
                throw new ServiceIoException("Unexpected HTTP response from CLMS: " + response.message());
            }

            List<FileDetails> filesDetails = getFileUrlsFromMetalink(response.body().byteStream());
            
            for (FileDetails fileDetails: filesDetails) {
            	downloadFile(targetDir, fileDetails.getDownloadUrl(), fileDetails.getFileName());
            }
        } catch (XPathExpressionException | SAXException | ParserConfigurationException e) {
        	throw new ServiceIoException("Unexpected HTTP response from CLMS: " + e.getMessage());
		}
        return targetDir;
    }
    
	private HttpUrl getMetalinkUrl(URI uri) throws IOException {
        // Trim the leading slash from the path and get the search URL
    	String[] segments = uri.getPath().split("/");
    	String collectionId = segments[1];
    	String productId =  segments[2];
        
        HttpUrl searchUrl = HttpUrl.parse(properties.getClmsSearchUrl()).newBuilder()
                .addPathSegments("findProducts.json")
                .addQueryParameter("count", "1")
                .addQueryParameter("collection", collectionId)
                .addQueryParameter("uid", productId)
                .build();

        LOG.debug("Searching CLMS to find download URL: {}", searchUrl);

        Request request = new Request.Builder().url(searchUrl).get().build();

        try (Response response = searchClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.error("Received unsuccessful HTTP response for CLMS search: {}", response.toString());
                throw new ServiceIoException("Unexpected HTTP response from CLMS: " + response.message());
            }

            String responseBody = response.body().string();
            String productPath = JsonPath.read(responseBody, "$.entry.EarthObservation.result.EarthObservationResult.product.ProductInformation.fileName.ServiceReference.href");
            return HttpUrl.parse(productPath);
          
        }
    }
    
	private List<FileDetails> getFileUrlsFromMetalink(InputStream metalinkXmlStream) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
		List<FileDetails> filesDetails = new ArrayList<>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(metalinkXmlStream);
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        XPathExpression expr =
                xpath.compile("/metalink/files/file");
        XPathExpression nameExpr =
                xpath.compile("@name");
        XPathExpression urlExpr =
                xpath.compile("resources/url[@type='http']");
        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
        	Node node = nodes.item(i);
        	filesDetails.add(new FileDetails(HttpUrl.parse((String) urlExpr.evaluate(node, XPathConstants.STRING)), (String) nameExpr.evaluate(node, XPathConstants.STRING)));
        }
        return filesDetails;
	}

	private void downloadFile(Path targetDir, HttpUrl downloadUrl, String filename) throws IOException {
    	int count = 0;
    	int maxTries = properties.getRetries();
    	while(true) {
    	    try {
    	    	 Request request = new Request.Builder().url(downloadUrl).build();
    	         Response response = downloadClient.newCall(request).execute();

    	         if (!response.isSuccessful()) {
    	             throw new ServiceIoException("Unsuccessful HTTP response: " + response);
    	         }

    	         Path outputFile = targetDir.resolve(filename);

    	         try (BufferedSource source = response.body().source();
    	              BufferedSink sink = Okio.buffer(Okio.sink(outputFile))) {
    	             long downloadedBytes = sink.writeAll(source);
    	             LOG.debug("Downloaded {} bytes for {}", downloadedBytes, downloadUrl);
    	         }
    	         response.close();
    	         
    	         LOG.info("Successfully downloaded via CLMS: {}", outputFile);
    	         return;
    	         
    	    } catch (Exception e) {
    	        if (++count == maxTries) { 
    	        	throw e;
    	        }
    	        try {
    	            Thread.sleep(((long) Math.round(Math.pow(2, count)) * 1000) 
    	                + (random.nextInt(999) + 1));
    	        } catch (InterruptedException ie) {
    	            //Keep retrying 
    	        }
    	    }
    	}
    }
	
    private final class CLMSAuthenticator implements Authenticator {
        private static final int MAX_RETRIES = 3;

        @Override
        public Request authenticate(Route route, Response response) throws IOException {
            HttpUrl url = route.address().url();

            if (responseCount(response) >= MAX_RETRIES) {
                LOG.error("Failed authentication for {} {} times, aborting", url, MAX_RETRIES);
                return null;
            }

            Credentials creds = fstepServerClient.credentialsServiceBlockingStub().getCredentials(GetCredentialsParams.newBuilder().setHost(url.host()).build());

            if (creds.getType() == Credentials.Type.BASIC) {
                String credHeader = okhttp3.Credentials.basic(creds.getUsername(), creds.getPassword());
                return response.request().newBuilder()
                        .header("Authorization", credHeader)
                        .build();
            } else {
                LOG.error("Authentication required for {}, but no basic credentials found, aborting", url);
                return null;
            }
        }

        private int responseCount(Response response) {
            int result = 1;
            while ((response = response.priorResponse()) != null) {
                result++;
            }
            return result;
        }
    }
    
    @Data
    @Component
    static final class CLMSDownloaderProperties {
        @Value("${fstep.worker.downloader.clms.searchUrl:https://land.copernicus.vgt.vito.be/openSearch/}")
        private String clmsSearchUrl;
        @Value("${fstep.worker.downloader.clms.retries:3}")
        private int retries;
    }

}
