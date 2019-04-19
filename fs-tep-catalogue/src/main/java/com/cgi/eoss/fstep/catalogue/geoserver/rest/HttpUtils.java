package com.cgi.eoss.fstep.catalogue.geoserver.rest;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

@Deprecated
/**
 * This is a drop-in replacement of {@link it.geosolutions.geoserver.rest.HTTPUtils}  
 * to cover for the fact that the original class does not correctly manage the 204 success code
 **/
public class HttpUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);

    /**
     * Performs an HTTP GET on the given URL.
     * 
     * @param url The URL where to connect to.
     * @return The HTTP response as a String if the HTTP response code was 200
     *         (OK).
     * @throws MalformedURLException
     */
    public static String get(String url) throws MalformedURLException {
        return get(url, null, null);
    }

    /**
     * Performs an HTTP GET on the given URL. <BR>
     * Basic auth is used if both username and pw are not null.
     * 
     * @param url The URL where to connect to.
     * @param username Basic auth credential. No basic auth if null.
     * @param pw Basic auth credential. No basic auth if null.
     * @return The HTTP response as a String if the HTTP response code was 200
     *         (OK).
     * @throws MalformedURLException
     */
    public static String get(String url, String username, String pw) {

        GetMethod httpMethod = null;
        HttpClient client = new HttpClient();
        HttpConnectionManager connectionManager = client.getHttpConnectionManager();
        try {
            setAuth(client, url, username, pw);
            httpMethod = new GetMethod(url);
            connectionManager.getParams().setConnectionTimeout(5000);
            int status = client.executeMethod(httpMethod);
            if (status == HttpStatus.SC_OK) {
                InputStream is = httpMethod.getResponseBodyAsStream();
                String response = IOUtils.toString(is);
                IOUtils.closeQuietly(is);
                if (response.trim().length() == 0) { // sometime gs rest fails
                    LOGGER.warn("ResponseBody is empty");
                    return null;
                } else {
                    return response;
                }
            } else {
                LOGGER.info("(" + status + ") " + HttpStatus.getStatusText(status) + " -- " + url);
            }
        } catch (ConnectException e) {
            LOGGER.info("Couldn't connect to [" + url + "]");
        } catch (IOException e) {
            LOGGER.info("Error talking to [" + url + "]", e);
        } finally {
            if (httpMethod != null)
                httpMethod.releaseConnection();
            connectionManager.closeIdleConnections(0);
        }

        return null;
    }

    /**
     * PUTs a File to the given URL. <BR>
     * Basic auth is used if both username and pw are not null.
     * 
     * @param url The URL where to connect to.
     * @param file The File to be sent.
     * @param contentType The content-type to advert in the PUT.
     * @param username Basic auth credential. No basic auth if null.
     * @param pw Basic auth credential. No basic auth if null.
     * @return The HTTP response as a String if the HTTP response code was 200
     *         (OK).
     * @throws MalformedURLException
     * @return the HTTP response or <TT>null</TT> on errors.
     */
    public static String put(String url, File file, String contentType, String username, String pw) {
        return put(url, new FileRequestEntity(file, contentType), username, pw);
    }

    /**
     * PUTs a String to the given URL. <BR>
     * Basic auth is used if both username and pw are not null.
     * 
     * @param url The URL where to connect to.
     * @param content The content to be sent as a String.
     * @param contentType The content-type to advert in the PUT.
     * @param username Basic auth credential. No basic auth if null.
     * @param pw Basic auth credential. No basic auth if null.
     * @return The HTTP response as a String if the HTTP response code was 200
     *         (OK).
     * @throws MalformedURLException
     * @return the HTTP response or <TT>null</TT> on errors.
     */
    public static String put(String url, String content, String contentType, String username, String pw) {
        try {
            return put(url, new StringRequestEntity(content, contentType, null), username, pw);
        } catch (UnsupportedEncodingException ex) {
            LOGGER.error("Cannot PUT " + url, ex);
            return null;
        }
    }

    /**
     * PUTs a String representing an XML document to the given URL. <BR>
     * Basic auth is used if both username and pw are not null.
     * 
     * @param url The URL where to connect to.
     * @param content The XML content to be sent as a String.
     * @param username Basic auth credential. No basic auth if null.
     * @param pw Basic auth credential. No basic auth if null.
     * @return The HTTP response as a String if the HTTP response code was 200
     *         (OK).
     * @throws MalformedURLException
     * @return the HTTP response or <TT>null</TT> on errors.
     */
    public static String putXml(String url, String content, String username, String pw) {
        return put(url, content, "text/xml", username, pw);
    }

    /**
     * Performs a PUT to the given URL. <BR>
     * Basic auth is used if both username and pw are not null.
     * 
     * @param url The URL where to connect to.
     * @param requestEntity The request to be sent.
     * @param username Basic auth credential. No basic auth if null.
     * @param pw Basic auth credential. No basic auth if null.
     * @return The HTTP response as a String if the HTTP response code was 200
     *         (OK).
     * @throws MalformedURLException
     * @return the HTTP response or <TT>null</TT> on errors.
     */
    public static String put(String url, RequestEntity requestEntity, String username, String pw) {
        return send(new PutMethod(url), url, requestEntity, username, pw);
    }

    /**
     * POSTs a File to the given URL. <BR>
     * Basic auth is used if both username and pw are not null.
     * 
     * @param url The URL where to connect to.
     * @param file The File to be sent.
     * @param contentType The content-type to advert in the POST.
     * @param username Basic auth credential. No basic auth if null.
     * @param pw Basic auth credential. No basic auth if null.
     * @return The HTTP response as a String if the HTTP response code was 200
     *         (OK).
     * @throws MalformedURLException
     * @return the HTTP response or <TT>null</TT> on errors.
     */
    public static String post(String url, File file, String contentType, String username, String pw) {
        return post(url, new FileRequestEntity(file, contentType), username, pw);
    }

    /**
     * POSTs a String to the given URL. <BR>
     * Basic auth is used if both username and pw are not null.
     * 
     * @param url The URL where to connect to.
     * @param content The content to be sent as a String.
     * @param contentType The content-type to advert in the POST.
     * @param username Basic auth credential. No basic auth if null.
     * @param pw Basic auth credential. No basic auth if null.
     * @return The HTTP response as a String if the HTTP response code was 200
     *         (OK).
     * @throws MalformedURLException
     * @return the HTTP response or <TT>null</TT> on errors.
     */
    public static String post(String url, String content, String contentType, String username, String pw) {
        try {
            return post(url, new StringRequestEntity(content, contentType, null), username, pw);
        } catch (UnsupportedEncodingException ex) {
            LOGGER.error("Cannot POST " + url, ex);
            return null;
        }
    }

    /**
     * POSTs a String representing an XML document to the given URL. <BR>
     * Basic auth is used if both username and pw are not null.
     * 
     * @param url The URL where to connect to.
     * @param content The XML content to be sent as a String.
     * @param username Basic auth credential. No basic auth if null.
     * @param pw Basic auth credential. No basic auth if null.
     * @return The HTTP response as a String if the HTTP response code was 200
     *         (OK).
     * @throws MalformedURLException
     * @return the HTTP response or <TT>null</TT> on errors.
     */
    public static String postXml(String url, String content, String username, String pw) {
        return post(url, content, "text/xml", username, pw);
    }

    /**
     * Performs a POST to the given URL. <BR>
     * Basic auth is used if both username and pw are not null.
     * 
     * @param url The URL where to connect to.
     * @param requestEntity The request to be sent.
     * @param username Basic auth credential. No basic auth if null.
     * @param pw Basic auth credential. No basic auth if null.
     * @return The HTTP response as a String if the HTTP response code was 200
     *         (OK).
     * @throws MalformedURLException
     * @return the HTTP response or <TT>null</TT> on errors.
     */
    public static String post(String url, RequestEntity requestEntity, String username, String pw) {
        return send(new PostMethod(url), url, requestEntity, username, pw);
    }
    
    private static String send(final EntityEnclosingMethod httpMethod, String url, RequestEntity requestEntity, String username,
                    String pw) {
        HttpClient client = new HttpClient();
        HttpConnectionManager connectionManager = client.getHttpConnectionManager();
        try {
            setAuth(client, url, username, pw);
            connectionManager.getParams().setConnectionTimeout(5000);
            if (requestEntity != null)
                httpMethod.setRequestEntity(requestEntity);
            int status = client.executeMethod(httpMethod);

            switch (status) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_CREATED:
                case HttpURLConnection.HTTP_ACCEPTED:
                    String response = IOUtils.toString(httpMethod.getResponseBodyAsStream());
                    // LOGGER.info("================= POST " + url);
                    if (LOGGER.isInfoEnabled())
                        LOGGER.info("HTTP " + httpMethod.getStatusText() + ": " + response);
                    return response;
		case HttpURLConnection.HTTP_NO_CONTENT:
		    return "";
                default:
                    LOGGER.warn("Bad response: code[" + status + "]" + " msg[" + httpMethod.getStatusText() + "]" + " url[" + url + "]"
                                    + " method[" + httpMethod.getClass().getSimpleName() + "]: "
                                    + IOUtils.toString(httpMethod.getResponseBodyAsStream()));
                    return null;
            }
        } catch (ConnectException e) {
            LOGGER.info("Couldn't connect to [" + url + "]");
            return null;
        } catch (IOException e) {
            LOGGER.error("Error talking to " + url + " : " + e.getLocalizedMessage());
            return null;
        } finally {
            if (httpMethod != null)
                httpMethod.releaseConnection();
            connectionManager.closeIdleConnections(0);
        }
    }

    private static void setAuth(HttpClient client, String url, String username, String pw) throws MalformedURLException {
        URL u = new URL(url);
        if (username != null && pw != null) {
            Credentials defaultcreds = new UsernamePasswordCredentials(username, pw);
            client.getState().setCredentials(new AuthScope(u.getHost(), u.getPort()), defaultcreds);
            client.getParams().setAuthenticationPreemptive(true); // GS2 by
                                                                  // default
                                                                  // always
                                                                  // requires
                                                                  // authentication
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Not setting credentials to access to " + url);
            }
        }
    }
}
