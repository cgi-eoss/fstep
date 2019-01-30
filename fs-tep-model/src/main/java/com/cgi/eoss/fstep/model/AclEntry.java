package com.cgi.eoss.fstep.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.springframework.hateoas.Identifiable;

import lombok.Data;


@Entity
@Data
@Table(name = "acl_entry", indexes = {@Index(name = "acl_entry_pkey", columnList = "id")}, 
		uniqueConstraints= {@UniqueConstraint(name = "acl_entry_acl_object_identity_ace_order_key", columnNames = {"acl_object_identity","ace_order"})}
)
public class AclEntry implements Identifiable<Long>
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private final Long id;
 
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "acl_object_identity", referencedColumnName = "id", nullable = false)
    private AclObjectIdentity aclObjectIdentity;
 
    @Column(name = "ace_order", nullable = false)
    private final Integer aceOrder;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sid", referencedColumnName = "id", nullable = false)
    private AclSid sid;
    
    @Column(name = "mask", nullable = false)
    private final Integer mask;
    
    @Column(name = "granting", nullable = false)
    private final Boolean granting;
    
    @Column(name = "audit_success", nullable = false)
    private final Boolean auditSuccess;
    
    @Column(name = "audit_failure", nullable = false)
    private final Boolean auditFailure;
    
}