package com.cgi.eoss.fstep.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
// import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import com.google.common.collect.ComparisonChain;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <p>
 * FS-TEP User Preferences. A simple KV store to save user data
 * </p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "fstep_user_preferences",
        indexes = {@Index(name = "fstep_user_preferences_owner_idx", columnList = "owner"),
                @Index(name = "fstep_user_preferences_name_idx", columnList = "name")},
        uniqueConstraints = {@UniqueConstraint(name = "fstep_user_preferences_name_owner_idx",
                columnNames = {"name", "owner"})})
@NoArgsConstructor
@Entity
public class UserPreference implements FstepEntityWithOwner<UserPreference> {
    /**
     * <p>
     * Internal unique identifier of the preference
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>
     * The user owning the preference.
     * </p>
     * 
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner")
    private User owner;
    //
    @Column(name = "name")
    private String name;

    @Lob
    @Column(name = "preference", length = 51200)
    private String preference;

    @Column(name = "type")
    private String type;

    /**
     * Create a new UserPreference instance with the minimum required parameters
     *
     * @param name Name of the preference
     * @param type Type of the preference
     * @param preference Value of the preference
     * 
     */
    public UserPreference(String name, String type, String preference) {
        this.name = name;
        this.type = type;
        this.preference = preference;
    }


    @Override
    public int compareTo(UserPreference o) {
        return ComparisonChain.start().compare(owner.getId(), o.owner.getId()).result();
    }

}
