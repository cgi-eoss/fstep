package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public interface FstepFileDataService extends
        FstepEntityDataService<FstepFile> {
    FstepFile getByUri(URI uri);

    FstepFile getByUri(String uri);

    FstepFile getByRestoId(UUID uuid);

    List<FstepFile> findByOwner(User user);

    List<FstepFile> getByType(FstepFile.Type type);

	Long sumFilesizeByOwner(User user);
	
	public FstepFile syncGeoserverLayersAndSave(FstepFile fstepFile);
}
