package com.cgi.eoss.fstep.model;

import com.google.common.collect.ComparisonChain;
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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <p>
 * FS-TEP User Mounts. Defines additional Docker mount points available to users
 * </p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "fstep_user_mounts",
        indexes = {@Index(name = "fstep_user_mounts_owner_idx", columnList = "owner"),
                @Index(name = "fstep_user_mounts_name_idx", columnList = "name")},
        uniqueConstraints = {@UniqueConstraint(name = "fstep_user_mounts_name_owner_idx",
                columnNames = {"name", "owner"})})
@NoArgsConstructor
@Entity
public class UserMount implements FstepEntityWithOwner<UserMount> {
    /**
     * <p>
     * Internal unique identifier of the mount
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>
     * The user owning the mount.
     * </p>
     * 
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner")
    private User owner;
   
    /**
     * <p>
     * A human readable name for the mount
     * </p>
     * 
     */
    @Column(name = "name")
    private String name;

    /**
     * <p>
     * The mount path
     * </p>
     * 
     */
    @Column(name = "mount_path")
    private String mountPath;

    /**
     * <p>The mount type (wether read-only or read-write)</p>
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MountType type;
    
    /**
     * Create a new UserMount instance with the minimum required parameters
     *
     * @param name Name of the mount
     * @param mountPath Mount path
     * 
     */
    public UserMount(String name, String mountPath, MountType type) {
        this.name = name;
        this.mountPath = mountPath;
        this.type = type;
    }


    // @Override
    public int compareTo(UserMount o) {
        return ComparisonChain.start().compare(owner.getId(), o.owner.getId()).result();
    }

    public enum MountType {
        RO, RW
    }
}
