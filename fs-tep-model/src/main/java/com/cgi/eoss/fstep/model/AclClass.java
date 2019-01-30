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
@Table(name = "acl_class", indexes = {@Index(name = "acl_class_pkey", columnList = "id")}, 
		uniqueConstraints = {@UniqueConstraint(name = "acl_class_class_key", columnNames = "class")}
)
public class AclClass implements Identifiable<Long>
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private final Long id;
 
    @Column(name = "class", nullable = false)
    private final String classStr;
 
  
}