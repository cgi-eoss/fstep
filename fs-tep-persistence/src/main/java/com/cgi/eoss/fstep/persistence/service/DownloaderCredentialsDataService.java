package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.DownloaderCredentials;

public interface DownloaderCredentialsDataService extends
        FstepEntityDataService<DownloaderCredentials> {
    /**
     * @param host The hostname for which credentials are required.
     * @return The credentials required to download from the given host, or null if no credentials were found for the
     * host.
     */
    DownloaderCredentials getByHost(String host);
}
