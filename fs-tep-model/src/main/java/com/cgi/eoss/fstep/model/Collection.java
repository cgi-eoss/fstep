package com.cgi.eoss.fstep.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.cgi.eoss.fstep.model.FstepFile.Type;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ComparisonChain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(exclude = {"id", "fstepFiles"})
@Table(name = "fstep_collections",
        indexes = {@Index(name = "fstep_collections_name_idx", columnList = "name"), @Index(name = "fstep_collections_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(name = "fstep_collections_name_owner_idx", columnNames = {"name", "owner"})})
@NoArgsConstructor
@Entity
public class Collection implements FstepEntityWithOwner<Collection>, Searchable {
    /**
     * <p>Internal unique identifier of the collection.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    /**
     * <p>Name of the collection.</p>
     */
    @Column(name = "name", nullable = false)
    private String name;
    
    /**
     * <p>Collection identifier.</p>
     */
    @JsonIgnore
    @Column(name = "identifier", nullable = false)
    private String identifier;

    /**
     * <p>Human-readable descriptive summary of the collection.</p>
     */
    @Lob
    @Column(name = "description")
    private String description;
    
    /**
     * <p>Human-readable descriptive summary of the collection products type</p>
     */
    @Column(name = "productsType")
    private String productsType;

    /**
     * <p>Type of files inside this collection</p>
     */
    @Column(name = "file_type")
    @Enumerated(EnumType.STRING)
    private FstepFile.Type fileType = Type.OUTPUT_PRODUCT;
    
    /**
     * <p>The user owning the collection, typically the collection creator.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;
    
    /**
     * <p>The files belonging to the collection</p>
     */
    
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "collection")
    private Set<FstepFile> fstepFiles = new HashSet<>();

    /**
     * <p>Create a new collection with the minimum mandatory parameters.</p>
     *
     * @param name Name of the collection. Must be unique per user.
     * @param owner User owning the collection.
     */
    public Collection(String name, User owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public int compareTo(Collection o) {
        return ComparisonChain.start().compare(name, o.name).compare(owner.getId(), o.owner.getId()).result();
    }

}
