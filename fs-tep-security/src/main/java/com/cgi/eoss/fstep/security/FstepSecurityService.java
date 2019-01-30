package com.cgi.eoss.fstep.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.FstepEntityWithOwner;
import com.cgi.eoss.fstep.model.Group;
import com.cgi.eoss.fstep.model.QAclEntry;
import com.cgi.eoss.fstep.model.QAclObjectIdentity;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.PublishingRequestDataService;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.jpa.JPAExpressions;

import lombok.extern.log4j.Log4j2;

/**
 * <p>Provides common utility-style methods for interacting with the FS-TEP security context.</p>
 */
@Component
@Log4j2
public class FstepSecurityService {
    public static final Authentication PUBLIC_AUTHENTICATION = new PreAuthenticatedAuthenticationToken("PUBLIC", "N/A", ImmutableList.of(FstepPermission.PUBLIC));

    private static final Predicate<GrantedAuthority> ADMIN_PREDICATE = a -> Role.ADMIN.getAuthority().equals(a.getAuthority());
    private static final Predicate<GrantedAuthority> SUPERUSER_PREDICATE = ADMIN_PREDICATE.or(a -> Role.CONTENT_AUTHORITY.getAuthority().equals(a.getAuthority()));

    private final MutableAclService aclService;
    private final AclPermissionEvaluator aclPermissionEvaluator;
    private final UserDataService userDataService;
    private final PublishingRequestDataService publishingRequestDataService;

    @Autowired
    public FstepSecurityService(MutableAclService aclService, AclPermissionEvaluator aclPermissionEvaluator, UserDataService userDataService, PublishingRequestDataService publishingRequestDataService) {
        this.aclService = aclService;
        this.aclPermissionEvaluator = aclPermissionEvaluator;
        this.userDataService = userDataService;
        this.publishingRequestDataService = publishingRequestDataService;
    }

    public void updateOwnerWithCurrentUser(FstepEntityWithOwner entity) {
        entity.setOwner(getCurrentUser());
    }

    public User getCurrentUser() {
        String username = getCurrentAuthentication().getName();
        return userDataService.getByName(username);
    }

    public Collection<? extends GrantedAuthority> getCurrentAuthorities() {
        return getCurrentAuthentication().getAuthorities();
    }

    public Set<Group> getCurrentGroups() {
        return getCurrentUser().getGroups();
    }

    public User refreshPersistentUser(User user) {
        return userDataService.refresh(user);
    }

    /**
     * <p>Create an access control entry granting all users (the PUBLIC authority) READ access to the given object.</p>
     *
     * @param objectClass The class of the entity being published.
     * @param identifier The identifier of the entity being published.
     */
    public void publish(Class<?> objectClass, Long identifier) {
        LOG.info("Publishing entity: {} (id: {})", objectClass, identifier);

        publish(new ObjectIdentityImpl(objectClass, identifier));
    }

    /**
     * <p>Create an access control entry granting all users (the PUBLIC authority) READ access to the given object.</p>
     *
     * @param objectIdentity The identifier of the entity being published.
     */
    public void publish(ObjectIdentity objectIdentity) {
        MutableAcl acl = getAcl(objectIdentity);

        Sid publicSid = new GrantedAuthoritySid(FstepPermission.PUBLIC);

        FstepPermission.READ.getAclPermissions()
                .forEach(p -> acl.insertAce(acl.getEntries().size(), p, publicSid, true));

        saveAcl(acl);
    }

    public void unpublish(Class<?> objectClass, Long identifier) {
        if (!isPublic(new ObjectIdentityImpl(objectClass, identifier))) {
            LOG.warn("Attempted to unpublish non-public object: {} {}", objectClass, identifier);
            return;
        }

        LOG.info("Unpublishing entity: {} (id: {})", objectClass, identifier);
        MutableAcl acl = getAcl(new ObjectIdentityImpl(objectClass, identifier));

        Sid publicSid = new GrantedAuthoritySid(FstepPermission.PUBLIC);

        // Find the access control entries corresponding to PUBLIC READ access, and delete them
        int aceCount = acl.getEntries().size();
        IntStream.range(0, aceCount).map(i -> aceCount - i - 1)
                .filter(i -> acl.getEntries().get(i).getSid().equals(publicSid) && FstepPermission.READ.getAclPermissions().contains(acl.getEntries().get(i).getPermission()))
                .forEach(acl::deleteAce);

        saveAcl(acl);
    }

    /**
     * <p>Return the current access properties of the current user on the given object.</p>
     *
     * @param objectClass The class of the entity being tested.
     * @param identifier The identifier of the entity being tested.
     * @return Whether the object is published or not, as well as the {@link FstepPermission} corresponding to the
     * current access level.
     */
    public FstepAccess getCurrentAccess(Class<?> objectClass, Long identifier) {
        Authentication authentication = getCurrentAuthentication();
        ObjectIdentity objectIdentity = new ObjectIdentityImpl(objectClass, identifier);

        return FstepAccess.builder()
                .published(isPublic(objectIdentity))
                .publishRequested(isPublishRequested(objectClass, identifier))
                .currentLevel(getCurrentPermission(authentication, objectIdentity))
                .build();
    }

    private FstepPermission getCurrentPermission(Authentication authentication, ObjectIdentity objectIdentity) {
        if (isSuperUser()) {
            return FstepPermission.SUPERUSER;
        } else if (hasFstepPermission(authentication, FstepPermission.ADMIN, objectIdentity)) {
            return FstepPermission.ADMIN;
        } else if (hasFstepPermission(authentication, FstepPermission.WRITE, objectIdentity)) {
            return FstepPermission.WRITE;
        } else if (hasFstepPermission(authentication, FstepPermission.READ, objectIdentity) || isPublic(objectIdentity)) {
            return FstepPermission.READ;
        } else {
            return null;
        }
    }

    /**
     * <p>Verify that the FstepPermission.READ permission is granted on the given object to the static PUBLIC
     * Authentication entity.</p>
     *
     * @param objectClass The class of the entity being tested.
     * @param identifier The identifier of the entity being tested.
     * @return True if the object is READable by PUBLIC.
     */
    public boolean isPublic(Class<?> objectClass, Long identifier) {
        return isPublic(new ObjectIdentityImpl(objectClass, identifier));
    }

    /**
     * <p>Verify that the FstepPermission.READ permission is granted on the given object to the static PUBLIC
     * Authentication entity.</p>
     *
     * @param objectIdentity The object identity to be tested for PUBLIC visibility.
     * @return True if the object is READable by PUBLIC.
     */
    public boolean isPublic(ObjectIdentity objectIdentity) {
        return hasFstepPermission(PUBLIC_AUTHENTICATION, FstepPermission.READ, objectIdentity);
    }


    /**
     * <p>Verify that the given user has the given permission on the given object.</p>
     *
     * @param user The User identity being tested.
     * @param permission The permission being tested.
     * @param objectClass The class of the entity being tested.
     * @param identifier The identifier of the entity being tested.
     * @return True if the object has the requested permission for the requested user.
     */
    public boolean hasUserPermission(User user, FstepPermission permission, Class<?> objectClass, Long identifier) {
        return hasUserPermission(user, permission, new ObjectIdentityImpl(objectClass, identifier));
    }

    /**
     * <p>Verify that the given user has the given permission on the given object.</p>
     *
     * @param user The User identity being tested.
     * @param permission The permission being tested.
     * @param objectIdentity The object identity to be tested for PUBLIC visibility.
     * @return True if the object has the requested permission for the requested user.
     */
    public boolean hasUserPermission(User user, FstepPermission permission, ObjectIdentity objectIdentity) {
        Authentication authentication = getAuthentication(user);
        return isSuperUser(authentication) || hasFstepPermission(authentication, permission, objectIdentity);
    }

    private Authentication getAuthentication(User user) {
        SecurityUser securityUser = new SecurityUser(user);
        return new PreAuthenticatedAuthenticationToken(securityUser.getUsername(), securityUser.getPassword(), securityUser.getAuthorities());
    }

    @Transactional
    public MutableAcl getAcl(ObjectIdentity objectIdentity, Sid... sids) {
        try {
            return (MutableAcl) aclService.readAclById(objectIdentity, Arrays.asList(sids));
        } catch (NotFoundException nfe) {
            return aclService.createAcl(objectIdentity);
        }
    }

    @Transactional
    public void saveAcl(MutableAcl acl) {
        aclService.updateAcl(acl);
    }

    private List<Sid> getSids(Authentication authentication) {
        return ImmutableList.<Sid>builder()
                .add(new PrincipalSid(authentication))
                .addAll(authentication.getAuthorities().stream().map(GrantedAuthoritySid::new).collect(Collectors.toSet()))
                .build();
    }
    
    private String getPrincipalSidAsString(Authentication authentication) {
        return new PrincipalSid(authentication).getPrincipal();
    }
    
    private List<String> getAuthoritiesSidsAsString(Authentication authentication) {
        return ImmutableList.<String>builder()
                .addAll(authentication.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet()))
                .build();
    }

    private boolean hasFstepPermission(Authentication authentication, FstepPermission permission, ObjectIdentity objectIdentity) {
        return permission.getAclPermissions().stream()
                .allMatch(p -> aclPermissionEvaluator.hasPermission(authentication, objectIdentity.getIdentifier(), objectIdentity.getType(), p));
    }

    private Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public Set<Long> getVisibleObjectIds(Class<?> objectClass, List<Long> allIds) {
        if (allIds.isEmpty()) {
            return ImmutableSet.of();
        }

        List<Sid> sids = getSids(getCurrentAuthentication());
        List<ObjectIdentity> objectIdentities = allIds.stream().map(id -> new ObjectIdentityImpl(objectClass, id)).collect(Collectors.toList());

        Map<ObjectIdentity, Acl> objectIdentityAclMap = null;
        try {
            objectIdentityAclMap = aclService.readAclsById(objectIdentities, sids);
        } catch (NotFoundException e) {
            // Manually build up the ACLs one by one with the null-safe getter
            objectIdentityAclMap = objectIdentities.stream()
                    .collect(Collectors.toMap(Function.identity(), oid -> this.getAcl(oid, sids.toArray(new Sid[sids.size()]))));
        }

        return objectIdentityAclMap.entrySet().stream()
                .filter(e -> {
                    try {
                        return e.getValue().isGranted(ImmutableList.of(BasePermission.READ), sids, false);
                    } catch (NotFoundException ex) {
                        return false;
                    }
                })
                .map(e -> e.getKey().getIdentifier())
                .map(Long.class::cast)
                .collect(Collectors.toSet());
    }

    public boolean isSuperUser() {
        return isSuperUser(getCurrentAuthentication());
    }

    public boolean isAdmin() {
        return getCurrentAuthorities().stream().anyMatch(ADMIN_PREDICATE);
    }

    private boolean isPublishRequested(Class<?> objectClass, Long identifier) {
        return !publishingRequestDataService.findOpenByAssociated(objectClass, identifier).isEmpty();
    }

    private boolean isSuperUser(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(SUPERUSER_PREDICATE);
    }

    public boolean isReadableByCurrentUser(Class<?> objectClass, Long objectId) {
        return isSuperUser() || hasFstepPermission(getCurrentAuthentication(), FstepPermission.READ, new ObjectIdentityImpl(objectClass, objectId));
    }

	public SubQueryExpression<Long> getVisibleQuery(Class<?> objectClass) {
		return JPAExpressions.selectDistinct(QAclObjectIdentity.aclObjectIdentity.objectIdIdentity)
				.from(QAclEntry.aclEntry).join(QAclObjectIdentity.aclObjectIdentity)
				.on(QAclEntry.aclEntry.aclObjectIdentity.eq(QAclObjectIdentity.aclObjectIdentity))
				.where(QAclObjectIdentity.aclObjectIdentity.objectIdClass.classStr.eq(objectClass.getCanonicalName())
						.and(QAclEntry.aclEntry.sid.sid.eq(getPrincipalSidAsString(getCurrentAuthentication()))
								.and(QAclEntry.aclEntry.sid.principal.eq(true))
								.or(QAclEntry.aclEntry.sid.sid
										.in(getAuthoritiesSidsAsString(getCurrentAuthentication()))
										.and(QAclEntry.aclEntry.sid.principal.eq(false)))))

		;
	}
	

}
