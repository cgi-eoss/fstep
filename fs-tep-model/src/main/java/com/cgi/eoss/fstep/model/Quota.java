package com.cgi.eoss.fstep.model;

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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.google.common.collect.ComparisonChain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "fstep_quota",
        indexes = {@Index(name = "fstep_quota_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(name = "fstep_quota_type_owner_idx",
              columnNames = {"owner", "usage_type"})}
)
@NoArgsConstructor
@Entity
public class Quota implements FstepEntityWithOwner<Quota> {

    
	@Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	/**
     * <p>Unique internal identifier of the costing expression.</p>
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>The usage type the quota refers to.</p>
     */
    @Column(name = "usage_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private UsageType usageType;
    
    /**
     * <p>Value of the allowed quota for this user</p>
     */
    @Column(name = "value", nullable = false)
    private Long value;
    

    @Override
    public int compareTo(Quota o) {
        return ComparisonChain.start().compare(id, o.id).result();
    }
}
