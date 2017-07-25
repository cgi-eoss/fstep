package com.cgi.eoss.fstep.security;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;

public enum FstepPermission {

    READ,
    WRITE,
    ADMIN,
    /**
     * <p>A marker permission for API access information; not used to map Spring ACLs.</p>
     */
    SUPERUSER;

    private static final BiMap<FstepPermission, Set<Permission>> SPRING_FSTEP_PERMISSION_MAP = ImmutableBiMap.<FstepPermission, Set<Permission>>builder()
            .put(FstepPermission.READ, ImmutableSet.of(BasePermission.READ))
            .put(FstepPermission.WRITE, ImmutableSet.of(BasePermission.WRITE, BasePermission.READ))
            .put(FstepPermission.ADMIN, ImmutableSet.of(BasePermission.ADMINISTRATION, BasePermission.WRITE, BasePermission.READ))
            .build();

    /**
     * <p>A Spring Security GrantedAuthority for PUBLIC visibility. Not technically an FstepPermission enum value, but
     * may be treated similarly.</p>
     */
    public static final GrantedAuthority PUBLIC = new SimpleGrantedAuthority("PUBLIC");

    public Set<Permission> getAclPermissions() {
        return SPRING_FSTEP_PERMISSION_MAP.get(this);
    }

    public static FstepPermission getFstepPermission(Set<Permission> aclPermissions) {
        return SPRING_FSTEP_PERMISSION_MAP.inverse().get(aclPermissions);
    }

}
