package com.cgi.eoss.fstep.model;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.google.common.collect.ComparisonChain;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <p>File usage records</p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "fstep_files_cumulative_usage_records",
indexes = {@Index(name = "fstep_files_cumulative_usage_records_idx", columnList = "owner")})
@NoArgsConstructor
@Entity
public class FstepFilesCumulativeUsageRecord implements FstepEntityWithOwner<FstepFilesCumulativeUsageRecord> {
    /**
     * <p>Unique internal identifier of the event.</p>
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * <p>The user for which the action is performed.</p>
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>The file type.</p>
     */
    @Column(name = "file_type")
    @Enumerated(EnumType.STRING)
    private FstepFile.Type fileType;

    /**
     * <p>The date of the record.</p>
     */
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;


    /**
     * <p>The cumulative size.</p>
     */
    @Column(name = "cumulative_size", nullable = false)
    private Long cumulativeSize;

    
    @Builder
    public FstepFilesCumulativeUsageRecord(User owner, LocalDate recordDate, FstepFile.Type fileType, Long cumulativeSize) {
        this.owner = owner;
        this.recordDate = recordDate;
        this.fileType = fileType;
        this.cumulativeSize = cumulativeSize;
    }

    @Override
    public int compareTo(FstepFilesCumulativeUsageRecord o) {
        return ComparisonChain.start().compare(recordDate, o.recordDate).compare(owner, owner).compare(fileType, o.fileType).result();
    }
}
