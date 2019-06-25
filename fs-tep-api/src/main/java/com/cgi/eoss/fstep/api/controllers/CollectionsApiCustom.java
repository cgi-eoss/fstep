package com.cgi.eoss.fstep.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile.Type;
import com.cgi.eoss.fstep.model.User;

public interface CollectionsApiCustom {
    
    <S extends Collection> S save(S collection);
    
    void delete(Collection collection);
    
    Page<Collection> findByFilterOnly(String filter, Pageable pageable);

    Page<Collection> findByFilterAndOwner(String filter, User user, Pageable pageable);

    Page<Collection> findByFilterAndNotOwner(String filter, User user, Pageable pageable);

	Page<Collection> parametricFind(String filter, Type fileType, User user, User notOwner, Pageable pageable);
}
