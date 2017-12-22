package com.cgi.eoss.fstep.model;

import com.google.common.collect.ComparisonChain;
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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <p>
 * FS-TEP User Endpoints. Defines additional endpoints (URLs) associated to users
 * </p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "fstep_user_endpoints",
        indexes = {@Index(name = "fstep_user_endpoints_owner_idx", columnList = "owner"),
                @Index(name = "fstep_user_endpoints_name_idx", columnList = "name")},
        uniqueConstraints = {@UniqueConstraint(name = "fstep_user_endpoints_name_owner_idx",
                columnNames = {"name", "owner"})})
@NoArgsConstructor
@Entity
public class UserEndpoint implements FstepEntityWithOwner<UserEndpoint> {
    /**
     * <p>
     * Internal unique identifier of the endpoint
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>
     * The user owning the endpoint.
     * </p>
     * 
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner")
    private User owner;
   
    /**
     * <p>
     * A human readable name for the endpoint
     * </p>
     * 
     */
    @Column(name = "name")
    private String name;

    /**
     * <p>
     * The endpoint path
     * </p>
     * 
     */
    @Column(name = "url")
    private String url;

    /**
     * Create a new UserEndpoint instance with the minimum required parameters
     *
     * @param name Name of the endpoint
     * @param url  URL of the endpoint
     * 
     */
    public UserEndpoint(String name, String url) {
        this.name = name;
        this.url = url;
    }


    // @Override
    public int compareTo(UserEndpoint o) {
        return ComparisonChain.start().compare(owner.getId(), o.owner.getId()).result();
    }

}
