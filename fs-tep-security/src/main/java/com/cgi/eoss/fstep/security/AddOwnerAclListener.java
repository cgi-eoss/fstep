package com.cgi.eoss.fstep.security;

import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepEntityWithOwner;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepServiceContextFile;
import com.cgi.eoss.fstep.model.Group;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.Wallet;
import com.cgi.eoss.fstep.model.WalletTransaction;
import com.google.common.collect.ImmutableSet;
import lombok.extern.log4j.Log4j2;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import java.util.Set;

@Component
@Log4j2
// TODO Replace this with an AOP aspect
public class AddOwnerAclListener implements PostInsertEventListener {

    /**
     * <p>Classes which implement {@link FstepEntityWithOwner} but should not be configured with an access control
     * list.</p>
     */
    private static final Set<Class> NON_ACL_CLASSES = ImmutableSet.of(
            FstepServiceContextFile.class,
            Wallet.class,
            WalletTransaction.class
    );

    private final EntityManagerFactory entityManagerFactory;
    private final FstepSecurityService fstepSecurityService;

    public AddOwnerAclListener(EntityManagerFactory entityManagerFactory, FstepSecurityService fstepSecurityService) {
        this.entityManagerFactory = entityManagerFactory;
        this.fstepSecurityService = fstepSecurityService;
    }

    @PostConstruct
    protected void registerSelf() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(this);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        Class<?> entityClass = event.getEntity().getClass();
        if (FstepEntityWithOwner.class.isAssignableFrom(entityClass) && !NON_ACL_CLASSES.contains(entityClass)) {
            FstepEntityWithOwner entity = (FstepEntityWithOwner) event.getEntity();

            // The owner should be User.DEFAULT for EXTERNAL_PRODUCT FstepFiles, otherwise the actual owner may be used
            PrincipalSid ownerSid =
                    FstepFile.class.equals(entityClass) && ((FstepFile) entity).getType() == FstepFile.Type.EXTERNAL_PRODUCT
                            ? new PrincipalSid(User.DEFAULT.getName())
                            : new PrincipalSid(entity.getOwner().getName());

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(FstepSecurityService.PUBLIC_AUTHENTICATION);
            }

            ObjectIdentity objectIdentity = new ObjectIdentityImpl(entityClass, entity.getId());
            MutableAcl acl = fstepSecurityService.getAcl(objectIdentity);
            acl.setOwner(ownerSid);

            if (acl.getEntries().size() > 0) {
                LOG.warn("Existing access control entries found for 'new' object: {} {}", entityClass.getSimpleName(), entity.getId());
            }

            if (Group.class.equals(entityClass)) {
                // Group members should be able to READ their groups
                LOG.debug("Adding self-READ ACL for new Group with ID {}", entity.getId());
                FstepPermission.READ.getAclPermissions()
                        .forEach(p -> acl.insertAce(acl.getEntries().size(), p, new GrantedAuthoritySid((Group) entity), true));
            }
            
            

            if (FstepFile.class.equals(entityClass) && ((FstepFile) entity).getType() == FstepFile.Type.EXTERNAL_PRODUCT) {
                // No one should have ADMIN permission for EXTERNAL_PRODUCT FstepFiles, but they should be PUBLIC to read ...
                LOG.debug("Adding PUBLIC READ-level ACL for new EXTERNAL_PRODUCT FstepFile with ID {}", entity.getId());
                FstepPermission.READ.getAclPermissions()
                        .forEach(p -> acl.insertAce(acl.getEntries().size(), p, new GrantedAuthoritySid(FstepPermission.PUBLIC), true));
            } else {
                // ... otherwise, the owner should have ADMIN permission for the entity
                LOG.debug("Adding owner-level ACL for new {} with ID {} (owner: {})", entityClass.getSimpleName(), entity.getId(), entity.getOwner().getName());
                FstepPermission.ADMIN.getAclPermissions()
                        .forEach(p -> acl.insertAce(acl.getEntries().size(), p, ownerSid, true));
            }
            
            if (FstepFile.class.equals(entityClass) && ((FstepFile) entity).getType() == FstepFile.Type.OUTPUT_PRODUCT) {
                // Fs-tep output products should have the collection as parent ACL
                LOG.debug("Adding PARENT ACL for new OUTPUT_PRODUCT FstepFile with ID {}", entity.getId());
                acl.setParent(fstepSecurityService.getAcl(new ObjectIdentityImpl(Collection.class, ((FstepFile) entity).getCollection().getId())));
            }

            fstepSecurityService.saveAcl(acl);
        }
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister persister) {
        return true;
    }

}
