package com.cgi.eoss.fstep.model;

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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Sets;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <p>A reference to a layer hosted in Geoserver.</p>
 */
@Data
@EqualsAndHashCode(of = {"workspace", "layer"})
@Table(name = "fstep_geoserver_layers",
        indexes = { @Index(name = "fstep_geoserver_layers_owner_idx", columnList = "owner"),
                @Index(name = "fstep_geoserver_layers_workspace_layer_idx", columnList = "workspace, layer")},
        uniqueConstraints = {@UniqueConstraint(columnNames = {"workspace", "layer"})}
)

@NoArgsConstructor
@Entity
public class GeoserverLayer implements FstepEntityWithOwner<GeoserverLayer> {
    /**
     * <p>Internal unique identifier of the file.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    
    /**
     * <p>The user owning the layer.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner")
    private User owner;
    
    /**
     * <p>The Geoserver workspace</p>
     */
    @Column(name = "workspace", nullable = false)
    private String workspace;


    /**
     * <p>The Geoserver layer</p>
     */
    @Column(name = "layer", nullable = false)
    private String layer;
    
    /**
     * <p>The Geoserver data store</p>
     */
    @Column(name = "store")
    private String store;

    /**
     * <p>The geoserver store type.</p>
     */
    @Column(name = "store_type")
    @Enumerated(EnumType.STRING)
    private StoreType storeType;

    /**
     * <p>Member files of this geoserver layer.</p>
     */
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "geoserverLayers")
    private Set<FstepFile> files = Sets.newHashSet();
    
    /**
     * <p>Construct a new GeoserverLayer instance with all the parameters.</p>
     *
     * @param owner
     * @oaram workspace
     * @param layer
     * @param store
     * @param storeType
     */
    public GeoserverLayer(User owner, String workspace, String store, String layer, StoreType storeType) {
        this.owner = owner;
        this.workspace = workspace;
        this.store = store;
        this.layer = layer;
        this.storeType = storeType;
    }
    
    /**
     * <p>Construct a new GeoserverLayer instance with the minimum mandatory (and unique) parameters.</p>
     *
     * @param owner
     * @oaram workspace
     * @param layer
     * @param storeType
     */
    public GeoserverLayer(User owner, String workspace, String layer, StoreType storeType) {
        this.owner = owner;
        this.workspace = workspace;
        this.layer = layer;
        this.storeType = storeType;
    }


    public GeoserverLayer(String reference) {
        // No-op, for SDR https://stackoverflow.com/questions/41324078/spring-data-rest-can-not-update-patch-a-list-of-child-entities-that-have-a-r
    }

    @Override
    public int compareTo(GeoserverLayer o) {
        return ComparisonChain.start().compare(workspace, o.workspace).compare(layer, o.layer).result();
    }

    public enum StoreType {
        
        MOSAIC,
        
        GEOTIFF,

        POSTGIS;
    }
}
