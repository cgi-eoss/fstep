package com.cgi.eoss.fstep.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import com.cgi.eoss.fstep.model.converters.StringMultimapYamlConverter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Multimap;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * <p>Representation of a single {@link JobConfig} execution.</p>
 */
@Data
@ToString(exclude = {"parentJob"})
@EqualsAndHashCode(exclude = {"id", "subJobs", "outputFiles"})
@Table(name = "fstep_jobs",
        indexes = {@Index(name = "fstep_jobs_job_config_idx", columnList = "job_config"), @Index(name = "fstep_jobs_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(columnNames = "ext_id")})
@NoArgsConstructor
@Entity
public class Job implements FstepEntityWithOwner<Job> {

    /**
     * <p>Internal unique identifier of the job.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>The external unique job identifier. Must be provided (e.g. UUID from the WPS server).</p>
     */
    @Column(name = "ext_id", nullable = false, updatable = false)
    private String extId;

    /**
     * <p>The job configuration used to launch this specific execution.</p>
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_config", nullable = false)
    private JobConfig config;

    /**
     * <p>The user executing this job.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>The date and time this job was launched.</p>
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * <p>The date and time this job execution ended.</p>
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * <p>The current execution status of the job.</p>
     */
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status = Status.CREATED;

    /**
     * <p>The id of the worker executing the job.</p>
     */
    @Column(name = "worker_id")
    private String workerId;

    
    /**
     * <p>Current stage of execution. Maybe arbitrarily set by service implementations to inform the user of
     * progress.</p>
     */
    @Column(name = "stage")
    private String stage;

    /**
     * <p>URL to the graphical interface if this is a {@link FstepService.Type#APPLICATION}.</p>
     */
    @Column(name = "gui_url")
    private String guiUrl;
    
    /**
     * <p>Backend endpoint for GUI if this is a {@link FstepService.Type#APPLICATION}.</p>
     */
    @Column(name = "gui_endpoint")
    private String guiEndpoint;
    
    /**
     * <p>Last known position of this job in the execution queue, -1 for unknown</p>
     */
    @Column(name = "queue_position")
    private Integer queuePosition = -1;

    /**
     * <p>The job execution outputs.</p>
     */
    @Lob
    @Convert(converter = StringMultimapYamlConverter.class)
    @Column(name = "outputs")
    private Multimap<String, String> outputs;

    /**
     * <p>The FstepFiles produced as job outputs.</p>
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "fstep_job_output_files",
            joinColumns = @JoinColumn(name = "job_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "file_id", nullable = false),
            indexes = @Index(name = "fstep_job_output_files_job_file_idx", columnList = "job_id, file_id", unique = true)
    )
    private Set<FstepFile> outputFiles = new HashSet<>();

    /**
     * <p>The subjobs produced from a job related to a parallel processor</p>
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "parentJob")
    private Set<Job> subJobs = new HashSet<>();
    
    /**
     * <p>The parent job of a subjob</p>
     */
    @ManyToOne
    private Job parentJob;
    
    /**
     * <p>Tells if a job is a parent job</p>
     */
    @Column(name = "is_parent")
    private boolean parent;
    
    
    public Job(JobConfig config, String extId, User owner, Job parentJob) {
        this(config, extId, owner);
        this.parentJob = parentJob;
    }
    
    public Job(JobConfig config, String extId, User owner) {
        this.config = config;
        this.extId = extId;
        this.owner = owner;
    }

    public void addOutputFile(FstepFile outputFile) {
        outputFiles.add(outputFile);
    }
    
    public void addSubJob(Job job) {
        subJobs.add(job);
    }

    @Override
    public int compareTo(Job o) {
        return ComparisonChain.start().compare(startTime, o.startTime).result();
    }

    public enum Status {
        CREATED, RUNNING, COMPLETED, ERROR, CANCELLED, PENDING, WAITING
    }
}
