package com.cgi.eoss.fstep.model;

import com.cgi.eoss.fstep.model.converters.UriStringConverter;
import com.google.common.collect.ComparisonChain;

import java.net.URI;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <p>
 * FS-TEP Persistent Folders
 * </p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "fstep_persistent_folders",
        indexes = {@Index(name = "ffstep_persistent_folders_owner_idx", columnList = "owner"),
                @Index(name = "fstep_persistent_folders_name_idx", columnList = "name")},
        uniqueConstraints = {@UniqueConstraint(name = "fstep_persistent_folders_name_owner_idx",
                columnNames = {"name", "owner"})})
@NoArgsConstructor
@Entity
public class PersistentFolder implements FstepEntityWithOwner<PersistentFolder> {
    /**
     * <p>
     * Internal unique identifier of the persistent folder
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>
     * The user owning the persistent folder.
     * </p>
     * 
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner")
    private User owner;
   
    /**
     * <p>
     * A human readable name for the persistent folder
     * </p>
     * 
     */
    @Column(name = "name")
    private String name;
    
    /**
     * <p>
     * A locator for the persistent folder
     * </p>
     * 
     */
    @Column(name = "locator")
    @Convert(converter = UriStringConverter.class)
    private URI locator;

    /**
     * <p>The persistent folder  type </p>
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;
    
    /**
     * <p>The persistent folder  type </p>
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.CREATED;
    
    /**
     * Create a new PersistentFolder instance with the minimum required parameters
     *
     * @param owner Owner of the persistent folder
     * @param name Name of the persistent folder
     * @param type Type of the persistent folder
     * "Param locator The persistent folder locator URI
     * 
     */
    public PersistentFolder(User owner, String name, Type type, URI locator) {
    	this.owner = owner;
        this.name = name;
        this.type = type;
        this.locator = locator;
    }

    // @Override
    public int compareTo(PersistentFolder o) {
        return ComparisonChain.start().compare(owner.getId(), o.owner.getId()).compare(name, o.name).result();
    }

    public enum Type {
        LOCAL_TO_WORKER
    }
    
    public enum Status {
        ACTIVE, CREATED
    }

	public URI getURI() {
		return locator.resolve("/" + String.valueOf(owner.getId() + "/" + name));
	}
}
