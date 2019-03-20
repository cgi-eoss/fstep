package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FstepFileDao extends FstepEntityDao<FstepFile> {
    FstepFile findOneByUri(URI uri);

    FstepFile findOneByRestoId(UUID uuid);

    List<FstepFile> findByOwner(User user);

    List<FstepFile> findByType(FstepFile.Type type);
    
    @Query(value = "SELECT SUM(f.filesize) from FstepFile f where f.owner = :user")
    Long sumFilesizeByOwner(@Param(value = "user") User user);
}
