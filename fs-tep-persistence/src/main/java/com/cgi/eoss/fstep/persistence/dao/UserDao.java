package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.User;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface UserDao extends FstepEntityDao<User> {
    @EntityGraph(attributePaths = {"groups"})
    User findOneByName(String name);
    List<User> findByNameContainingIgnoreCase(String term);
}
