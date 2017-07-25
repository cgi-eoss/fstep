package com.cgi.eoss.fstep.model;

public interface FstepEntityWithOwner<T> extends FstepEntity<T> {

    /**
     * @return The user who owns the entity.
     */
    User getOwner();

    /**
     * @param owner The new owner of the entity.
     */
    void setOwner(User owner);

}
