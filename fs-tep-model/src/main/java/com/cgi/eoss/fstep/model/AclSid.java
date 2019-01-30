package com.cgi.eoss.fstep.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.springframework.hateoas.Identifiable;

import lombok.Data;


@Entity
@Data
@Table(name = "acl_sid", indexes = {@Index(name = "acl_sid_pkey", columnList = "id")}, 
		uniqueConstraints = {@UniqueConstraint(name = "acl_sid_sid_principal_key", columnNames = {"sid", "principal"})}
)
public class AclSid implements Identifiable<Long>
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private final Long id;
 
    @Column(name = "principal", nullable = false)
    private final Boolean principal;
 
    @Column(name = "sid", nullable = false)
    private final String sid;
}