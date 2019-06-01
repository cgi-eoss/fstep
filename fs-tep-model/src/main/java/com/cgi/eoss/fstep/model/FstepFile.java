package com.cgi.eoss.fstep.model;

import com.cgi.eoss.fstep.model.converters.UriStringConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Sets;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * <p>A raw reference to an FS-TEP file-type object. Files may be physically located on disk, or simply references to
 * external files. These objects may be included in databaskets.</p>
 */
@Data
@EqualsAndHashCode(exclude = {"id", "geoserverLayers", "jobs"})
@Table(name = "fstep_files",
        indexes = {
                @Index(name = "fstep_files_uri_idx", columnList = "uri"),
                @Index(name = "fstep_files_resto_id_idx", columnList = "resto_id"),
                @Index(name = "fstep_files_owner_idx", columnList = "owner")},
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "uri"),
                @UniqueConstraint(columnNames = "resto_id")})
@NoArgsConstructor
@Entity
public class FstepFile implements FstepEntityWithOwner<FstepFile> {
    /**
     * <p>Internal unique identifier of the file.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>Unique URI for the file. May be in an FS-TEP internal form, e.g. "<code>"fstep://refData/${owner}/${restoId}"</code>".</p>
     */
    @Column(name = "uri", nullable = false)
    @Convert(converter = UriStringConverter.class)
    private URI uri;

    /**
     * <p>Resto catalogue identifier for the file. Used to hydrate API responses with comprehensive metadata.</p>
     */
    @Column(name = "resto_id", nullable = false)
    private UUID restoId;

    /**
     * <p>File type.</p>
     */
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private Type type;

    /**
     * <p>The user owning the file, typically the file uploader or job creator.</p>
     * <p>May be null, particularly in the case of {@link Type#EXTERNAL_PRODUCT}.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner")
    private User owner;

    /**
     * <p>Real filename to be used when materialising this file reference.</p>
     */
    @Column(name = "filename")
    private String filename;

    /**
     * <p>Size in bytes of the referenced file.</p>
     */
    @Column(name = "filesize")
    private Long filesize;

    /**
     * <p>The optional FS-TEP datasource to which this file is associated.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "datasource")
    @JsonIgnore
    private DataSource dataSource;
    
    /**
     * <p>The optional FS-TEP collection to which this file is associated.</p>
     */
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "collection_id")
    @JsonIgnore
    private Collection collection;

    /**
     * <p>Geoserver layers this file is associated to</p>
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "fstep_geoserver_layer_files",
            joinColumns = @JoinColumn(name = "file_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "geoserver_layer_id", referencedColumnName = "id"),
            uniqueConstraints = @UniqueConstraint(name = "fstep_geoserver_layer_files_layer_file_idx", columnNames = {"geoserver_layer_id", "file_id"}))
    private Set<GeoserverLayer> geoserverLayers = Sets.newHashSet();
    
    /**
     * <p>The FstepFiles produced as job outputs.</p>
     */
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "outputFiles")
    private Set<Job> jobs = new HashSet<>();
    
    
    /**
     * <p>Construct a new FstepFile instance with the minimum mandatory (and unique) parameters.</p>
     *
     * @param uri
     * @param restoId
     */
    public FstepFile(URI uri, UUID restoId) {
        this.uri = uri;
        this.restoId = restoId;
    }

    public FstepFile(String reference) {
        // No-op, for SDR https://stackoverflow.com/questions/41324078/spring-data-rest-can-not-update-patch-a-list-of-child-entities-that-have-a-r
    }

    @Override
    public int compareTo(FstepFile o) {
        return ComparisonChain.start().compare(uri, o.uri).result();
    }

    public enum Type {
        /**
         * <p>User-managed reference data file.</p>
         */

        REFERENCE_DATA,
        /**
         * <p>Output product generated by an FS-TEP service.</p>
         */

        OUTPUT_PRODUCT,

        /**
         * <p>External data product reference.</p>
         */
        EXTERNAL_PRODUCT;
    }
}
