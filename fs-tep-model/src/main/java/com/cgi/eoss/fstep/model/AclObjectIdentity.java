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
@Table(name = "acl_object_identity", indexes = {@Index(name = "acl_object_identity_pkey", columnList = "id")}, 
		uniqueConstraints = {@UniqueConstraint(name = "acl_object_identity_object_id_class_object_id_identity_key", columnNames = {"object_id_class","object_id_identity"})}
)
public class AclObjectIdentity implements Identifiable<Long>
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private final Long id;
 
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "object_id_class", referencedColumnName = "id", nullable = false)
    private AclClass objectIdClass;
 
    @Column(name = "object_id_identity", nullable = false)
    private Long objectIdIdentity;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_object", referencedColumnName = "id")
    private AclObjectIdentity parentObject;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_sid", referencedColumnName = "id", nullable = false)
    private AclSid ownerSid;
    
    @Column(name = "entries_inheriting", nullable = false)
    private boolean entriesInheriting;
    
}